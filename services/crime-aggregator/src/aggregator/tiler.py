"""Utilities for tile aggregation and clustering."""
from __future__ import annotations

import math
from collections import defaultdict
from dataclasses import dataclass
from typing import Dict, Iterable, List, Sequence

from .models import CrimeIncident

TILE_ZOOM = 15


@dataclass
class TileAggregate:
    tile_id: str
    incidents: List[CrimeIncident]
    revision: int


def web_mercator_tile(lat: float, lon: float, zoom: int = TILE_ZOOM) -> str:
    lat = max(min(lat, 85.05112878), -85.05112878)
    lon = ((lon + 180.0) % 360.0) - 180.0
    lat_rad = math.radians(lat)
    n = 2 ** zoom
    xtile = int((lon + 180.0) / 360.0 * n)
    ytile = int((1.0 - math.log(math.tan(lat_rad) + 1 / math.cos(lat_rad)) / math.pi) / 2.0 * n)
    xtile = max(0, min(xtile, n - 1))
    ytile = max(0, min(ytile, n - 1))
    return f"z{zoom}_{xtile}_{ytile}"


def bucket_incidents(incidents: Sequence[CrimeIncident]) -> Dict[str, TileAggregate]:
    buckets: Dict[str, List[CrimeIncident]] = defaultdict(list)
    for incident in incidents:
        buckets[incident.tile_z15].append(incident)

    aggregates: Dict[str, TileAggregate] = {}
    for tile_id, bucket in buckets.items():
        revision = int(max(inc.updated_at for inc in bucket).timestamp())
        aggregates[tile_id] = TileAggregate(tile_id=tile_id, incidents=list(bucket), revision=revision)
    return aggregates


def generate_heatmap_points(incidents: Iterable[CrimeIncident]) -> List[Dict[str, float]]:
    points: List[Dict[str, float]] = []
    for incident in incidents:
        points.append(
            {
                "latitude": incident.latitude,
                "longitude": incident.longitude,
                "intensity": max(0.1, incident.severity / 10.0 + 0.1),
            }
        )
    return points


def generate_clusters(incidents: Sequence[CrimeIncident], threshold: int = 15) -> List[Dict[str, float]]:
    if not incidents:
        return []

    # Simple grid-based clustering keyed by rounded coordinates
    cell_size = 0.0025  # approx 250m
    clusters: Dict[str, Dict[str, float]] = {}
    counts: Dict[str, int] = defaultdict(int)

    for incident in incidents:
        cell_x = round(incident.longitude / cell_size)
        cell_y = round(incident.latitude / cell_size)
        key = f"{cell_x}:{cell_y}"
        counts[key] += 1
        cluster = clusters.setdefault(
            key,
            {
                "cluster_id": key,
                "latitude": 0.0,
                "longitude": 0.0,
                "severity_sum": 0.0,
            },
        )
        cluster["latitude"] += incident.latitude
        cluster["longitude"] += incident.longitude
        cluster["severity_sum"] += incident.severity

    payload: List[Dict[str, float]] = []
    for key, cluster in clusters.items():
        count = counts[key]
        if count == 0:
            continue
        payload.append(
            {
                "cluster_id": key,
                "latitude": cluster["latitude"] / count,
                "longitude": cluster["longitude"] / count,
                "count": count,
                "average_severity": cluster["severity_sum"] / count,
            }
        )

    # Return dense clusters first
    payload.sort(key=lambda c: c["count"], reverse=True)
    return payload[:threshold]
