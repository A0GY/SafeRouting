"""Command line entry point for ingestion."""
from __future__ import annotations

import argparse
import asyncio
import logging
from typing import Sequence, Tuple

from .config import DEFAULT_COORDINATES
from .database import init_engine, session_scope
from .service import poll_police_api, purge_expired, upsert_incidents

LOGGER = logging.getLogger("crime-aggregator")
logging.basicConfig(level=logging.INFO)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Ingest UK Police API data into SafeRouting aggregator")
    parser.add_argument("--months", type=int, default=3, help="Number of months to fetch (default: 3)")
    parser.add_argument(
        "--coordinates",
        nargs="*",
        type=float,
        help="Sequence of lat lon pairs (default: London borough centroids)",
    )
    return parser.parse_args()


def chunk_coordinates(args: argparse.Namespace) -> Sequence[Tuple[float, float]]:
    if not args.coordinates:
        return DEFAULT_COORDINATES
    if len(args.coordinates) % 2 != 0:
        raise ValueError("Coordinates must be provided as lat lon pairs")
    pairs = []
    coords = list(args.coordinates)
    for i in range(0, len(coords), 2):
        pairs.append((coords[i], coords[i + 1]))
    return pairs


def main() -> None:
    args = parse_args()
    init_engine()
    coordinates = chunk_coordinates(args)
    LOGGER.info("Fetching police data for %d coordinate pairs", len(coordinates))

    payloads = asyncio.run(poll_police_api(months=args.months, coordinates=coordinates))
    LOGGER.info("Fetched %d raw incidents", len(payloads))

    with session_scope() as session:
        inserted = upsert_incidents(session, payloads)
        LOGGER.info("Upserted %d incidents", inserted)
        purged = purge_expired(session)
        LOGGER.info("Purged %d expired incidents", purged)


if __name__ == "__main__":
    main()
