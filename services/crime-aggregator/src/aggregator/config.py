"""Configuration helpers for the crime aggregation service."""
from __future__ import annotations

from functools import lru_cache
from pathlib import Path
from typing import List, Tuple

from dotenv import load_dotenv
import os

load_dotenv()

DEFAULT_COORDINATES: List[Tuple[float, float]] = [
    (51.5074, -0.1278),  # Westminster
    (51.5390, -0.1425),  # Camden
    (51.5465, -0.1058),  # Islington
    (51.5450, -0.0554),  # Hackney
    (51.5096, -0.0177),  # Tower Hamlets
    (51.4826, 0.0077),   # Greenwich
    (51.4526, -0.0154),  # Lewisham
    (51.5055, -0.0907),  # Southwark
    (51.4900, -0.1221),  # Lambeth
    (51.4567, -0.1910),  # Wandsworth
    (51.4927, -0.2339),  # Hammersmith and Fulham
    (51.5000, -0.1919),  # Kensington and Chelsea
    (51.5588, -0.2817),  # Brent
    (51.5130, -0.3089),  # Ealing
    (51.4746, -0.3680),  # Hounslow
    (51.4479, -0.3260),  # Richmond upon Thames
    (51.4085, -0.2861),  # Kingston upon Thames
    (51.4097, -0.1978),  # Merton
    (51.3618, -0.1945),  # Sutton
    (51.3762, -0.0982),  # Croydon
    (51.4039, 0.0198),   # Bromley
    (51.6252, -0.1517),  # Barnet
    (51.5898, -0.3346),  # Harrow
    (51.5441, -0.4760),  # Hillingdon
    (51.6521, -0.0807),  # Enfield
    (51.5908, -0.0134),  # Waltham Forest
    (51.5590, 0.0741),   # Redbridge
    (51.5812, 0.1837),   # Havering
    (51.5462, 0.1313),   # Barking and Dagenham
    (51.5076, 0.0343),   # Newham
    (51.4549, 0.1505),   # Bexley
    (51.5906, -0.1110),  # Haringey
]


@lru_cache(maxsize=1)
def get_database_url() -> str:
    """Return the configured database URL with a sensible default."""
    return os.getenv("DATABASE_URL", "sqlite:///" + str(Path("crime.db").absolute()))


@lru_cache(maxsize=1)
def get_police_api_base_url() -> str:
    return os.getenv("POLICE_API_BASE_URL", "https://data.police.uk/api/")


@lru_cache(maxsize=1)
def get_discord_webhook_url() -> str | None:
    return os.getenv("DISCORD_MODERATION_WEBHOOK")


@lru_cache(maxsize=1)
def get_retention_days() -> int:
    return int(os.getenv("RETENTION_DAYS", "365"))
