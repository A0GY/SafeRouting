"""Core aggregation logic and Police API ingestion."""
from __future__ import annotations

import datetime as dt
import logging
from typing import Dict, Iterable, List, Sequence, Tuple

import asyncio
import httpx
from sqlalchemy import delete, func, select
from sqlalchemy.orm import Session
from tenacity import retry, stop_after_attempt, wait_exponential

from .config import DEFAULT_COORDINATES, get_police_api_base_url, get_retention_days
from .models import CrimeIncident, TileRevision
from .schemas import TileResponse, TileSnapshot
from .tiler import TILE_ZOOM, bucket_incidents, generate_clusters, generate_heatmap_points, web_mercator_tile

LOGGER = logging.getLogger("crime-aggregator")

CRIME_SEVERITY_MAP: Dict[str, float] = {
    "anti-social-behaviour": 3.0,
    "bicycle-theft": 2.0,
    "burglary": 4.0,
    "criminal-damage-arson": 4.5,
    "drugs": 3.0,
    "other-theft": 2.0,
    "possession-of-weapons": 5.0,
    "public-order": 3.0,
    "robbery": 5.0,
    "shoplifting": 1.5,
    "theft-from-the-person": 3.0,
    "vehicle-crime": 3.0,
    "violent-crime": 5.0,
    "violence-and-sexual-offences": 5.0,
    "other-crime": 2.0,
}


def normalize_category(raw: str) -> str:
    return raw.lower().replace(" ", "-")


def severity_for_category(raw: str) -> float:
    return CRIME_SEVERITY_MAP.get(normalize_category(raw), 2.5)


def build_incident_key(payload: dict) -> str:
    persistent_id = payload.get("persistent_id")
    if persistent_id:
        return persistent_id
    return f"{payload['id']}-{payload['month']}"


async def fetch_crimes(client: httpx.AsyncClient, lat: float, lng: float, date: str) -> List[dict]:
    params = {"lat": lat, "lng": lng, "date": date}
    response = await client.get("crimes-street/all-crime", params=params, timeout=60.0)
    response.raise_for_status()
    return response.json()


async def poll_police_api(months: int = 3, coordinates: Sequence[Tuple[float, float]] | None = None) -> List[dict]:
    coords = coordinates or DEFAULT_COORDINATES
    base_url = get_police_api_base_url()
    async with httpx.AsyncClient(base_url=base_url) as client:
        now = dt.date.today().replace(day=1)
        dates = [now - dt.timedelta(days=30 * i) for i in range(months)]
        tasks = []
        for lat, lng in coords:
            for date in dates:
                tasks.append(fetch_crimes(client, lat, lng, date.strftime("%Y-%m")))
        results: Sequence[List[dict]] = await asyncio.gather(*tasks)
    flattened: List[dict] = []
    for batch in results:
        flattened.extend(batch)
    return flattened


@retry(wait=wait_exponential(multiplier=0.5, min=1, max=8), stop=stop_after_attempt(3))
def upsert_incidents(session: Session, payloads: Iterable[dict]) -> int:
    upserted = 0
    for payload in payloads:
        if payload.get("location") is None:
            continue
        lat = float(payload["location"]["latitude"])
        lon = float(payload["location"]["longitude"])
        month = payload["month"]
        occurred_on = dt.datetime.strptime(month + "-01", "%Y-%m-%d")
        normalized = normalize_category(payload["category"])
        severity = severity_for_category(normalized)
        tile_id = web_mercator_tile(lat, lon, TILE_ZOOM)
        incident_key = build_incident_key(payload)

        instance = session.execute(
            select(CrimeIncident).where(CrimeIncident.incident_key == incident_key)
        ).scalar_one_or_none()
        if instance:
            instance.latitude = lat
            instance.longitude = lon
            instance.category = normalized
            instance.severity = severity
            instance.occurred_on = occurred_on
            instance.month_bucket = month
            instance.tile_z15 = tile_id
            instance.context = payload.get("context")
            instance.updated_at = dt.datetime.utcnow()
        else:
            instance = CrimeIncident(
                incident_key=incident_key,
                latitude=lat,
                longitude=lon,
                category=normalized,
                severity=severity,
                occurred_on=occurred_on,
                month_bucket=month,
                context=payload.get("context"),
                tile_z15=tile_id,
            )
            session.add(instance)
        # track revision
        revision = session.execute(
            select(TileRevision).where(TileRevision.tile_id == tile_id)
        ).scalar_one_or_none()
        if revision:
            revision.revision = max(revision.revision + 1, int(dt.datetime.utcnow().timestamp()))
        else:
            session.add(TileRevision(tile_id=tile_id, revision=int(dt.datetime.utcnow().timestamp())))
        upserted += 1
    return upserted


def purge_expired(session: Session) -> int:
    retention_days = get_retention_days()
    cutoff = dt.datetime.utcnow() - dt.timedelta(days=retention_days)
    result = session.execute(
        delete(CrimeIncident).where(CrimeIncident.occurred_on < cutoff)
    )
    return result.rowcount or 0


def query_viewport(session: Session, *, north: float, south: float, east: float, west: float) -> Sequence[CrimeIncident]:
    stmt = (
        select(CrimeIncident)
        .where(CrimeIncident.latitude.between(south, north))
        .where(CrimeIncident.longitude.between(west, east))
    )
    return session.scalars(stmt).all()


def build_tile_response(
    incidents: Sequence[CrimeIncident],
    cached_tiles: Dict[str, int],
) -> TileResponse:
    aggregates = bucket_incidents(incidents)
    tiles: List[TileSnapshot] = []
    for tile_id, aggregate in aggregates.items():
        if cached_tiles.get(tile_id) == aggregate.revision:
            # Client already has this revision
            continue
        heatmap_points = generate_heatmap_points(aggregate.incidents)
        clusters = generate_clusters(aggregate.incidents)
        tiles.append(
            TileSnapshot(
                tile_id=tile_id,
                revision=aggregate.revision,
                heatmap=heatmap_points,
                clusters=clusters,
                incidents=[
                    {
                        "incident_key": incident.incident_key,
                        "latitude": incident.latitude,
                        "longitude": incident.longitude,
                        "category": incident.category,
                        "severity": incident.severity,
                        "occurred_on": incident.occurred_on,
                        "month_bucket": incident.month_bucket,
                        "borough": incident.borough,
                    }
                    for incident in aggregate.incidents
                ],
            )
        )
    active_tile_ids = set(aggregates.keys())
    removed_tiles = [tile_id for tile_id in cached_tiles.keys() if tile_id not in active_tile_ids]
    generated_at = dt.datetime.utcnow()
    return TileResponse(
        tiles=tiles,
        removed_tiles=removed_tiles,
        generated_at=generated_at,
        next_refresh_seconds=300,
    )


def newest_incident_timestamp(session: Session) -> dt.datetime | None:
    return session.scalars(select(func.max(CrimeIncident.updated_at))).first()
