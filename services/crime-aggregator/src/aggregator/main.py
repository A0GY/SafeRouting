"""FastAPI application entry point."""
from __future__ import annotations

import logging
from typing import Dict

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware

from .database import init_engine, session_scope
from .schemas import TileResponse
from .service import build_tile_response, newest_incident_timestamp, query_viewport

LOGGER = logging.getLogger("crime-aggregator")
logging.basicConfig(level=logging.INFO)

app = FastAPI(title="SafeRouting Crime Aggregator", version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"]
    ,
    allow_headers=["*"],
)


@app.on_event("startup")
def startup() -> None:
    init_engine()


@app.get("/health")
def healthcheck() -> Dict[str, str]:
    with session_scope() as session:
        latest = newest_incident_timestamp(session)
    return {"status": "ok", "last_update": latest.isoformat() if latest else None}


@app.get("/tiles/viewport", response_model=TileResponse)
def tiles_for_viewport(
    north: float = Query(..., description="North latitude of visible region"),
    south: float = Query(..., description="South latitude of visible region"),
    east: float = Query(..., description="East longitude of visible region"),
    west: float = Query(..., description="West longitude of visible region"),
    zoom: int = Query(15, ge=8, le=18, description="Zoom level for tile bucketing"),
    cached: list[str] = Query(default=[], description="Client cache of tileId:revision"),
):
    if north < south:
        raise HTTPException(status_code=400, detail="north must be greater than south")
    if east < west:
        raise HTTPException(status_code=400, detail="east must be greater than west")

    cache_map = {}
    for entry in cached:
        try:
            tile_id, revision = entry.split(":")
            cache_map[tile_id] = int(revision)
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=f"Invalid cached entry '{entry}'") from exc

    with session_scope() as session:
        incidents = query_viewport(session, north=north, south=south, east=east, west=west)
        response = build_tile_response(incidents, cache_map)
    return response


def run() -> None:
    import uvicorn

    uvicorn.run("aggregator.main:app", host="0.0.0.0", port=8000, reload=False)


__all__ = ["app", "run"]
