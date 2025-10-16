"""SQLAlchemy models for crime aggregation."""
from __future__ import annotations

from datetime import datetime
from typing import Optional

from sqlalchemy import DateTime, Float, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from .database import Base


class CrimeIncident(Base):
    __tablename__ = "crime_incidents"
    __table_args__ = (UniqueConstraint("incident_key", name="uq_incident_key"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    incident_key: Mapped[str] = mapped_column(String(128), nullable=False)
    latitude: Mapped[float] = mapped_column(Float, nullable=False)
    longitude: Mapped[float] = mapped_column(Float, nullable=False)
    category: Mapped[str] = mapped_column(String(80), nullable=False)
    severity: Mapped[float] = mapped_column(Float, nullable=False)
    occurred_on: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    month_bucket: Mapped[str] = mapped_column(String(7), nullable=False)
    context: Mapped[Optional[str]] = mapped_column(String(255))
    tile_z15: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    borough: Mapped[Optional[str]] = mapped_column(String(80))
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)


class TileRevision(Base):
    __tablename__ = "tile_revisions"
    tile_id: Mapped[str] = mapped_column(String(32), primary_key=True)
    revision: Mapped[int] = mapped_column(Integer, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)
