# SafeRouting

SafeRouting highlights UK crime data on top of Google Maps, compares “Safest” versus “Fastest” routes using a custom risk engine, and now streams incidents from a dedicated aggregation API so the client stays fast.

- Android client: Jetpack Compose, Google Maps, in-app routing UI
- Risk engine: Kotlin (`SafeRoutePlanner`) with incremental tile caching
- Aggregation API: Python/FastAPI service in `services/crime-aggregator/`

➡️ **First time here?** Read [SETUP_AND_RUN.md](SETUP_AND_RUN.md) for step-by-step installation, secrets management, and GDPR/operations guidance.

---

## Quick start (short version)
1. `git clone` the repo and open it in Android Studio.
2. Provide Google Maps + Directions keys via `secrets.properties`.
3. Start the aggregation API locally:
   ```bash
   cd services/crime-aggregator
   python -m venv .venv && source .venv/bin/activate
   pip install -e .
   crime-aggregator-api
   ```
4. (Optional) ingest sample data: `crime-aggregator-ingest --months 3`
5. Run the Android app – the map will stream heatmaps/markers for the visible viewport and the safest-route toggle will react instantly as tiles change.

See the full setup guide for hosting recommendations, moderation hooks, and retention policies.
