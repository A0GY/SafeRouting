"""Pydantic schemas for the aggregation API."""
from __future__ import annotations

from datetime import datetime
from typing import List, Optional

from pydantic import BaseModel, Field


class CrimeIncidentSchema(BaseModel):
    incident_key: str
    latitude: float
    longitude: float
    category: str
    severity: float
    occurred_on: datetime
    month_bucket: str
    borough: Optional[str] = None


class HeatmapPoint(BaseModel):
    latitude: float
    longitude: float
    intensity: float


class ClusterPayload(BaseModel):
    cluster_id: str
    latitude: float
    longitude: float
    count: int
    average_severity: float


class TileSnapshot(BaseModel):
    tile_id: str = Field(..., description="Web mercator tile identifier")
    revision: int
    heatmap: List[HeatmapPoint]
    clusters: List[ClusterPayload]
    incidents: List[CrimeIncidentSchema]


class TileResponse(BaseModel):
    tiles: List[TileSnapshot]
    removed_tiles: List[str]
    generated_at: datetime
    next_refresh_seconds: int
