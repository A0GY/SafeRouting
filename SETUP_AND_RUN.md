# SafeRouting – Setup and Run Guide

This document explains how to bootstrap the full SafeRouting stack (Android clients, the Kotlin safest-route engine, and the new Python-based crime aggregation API), how to configure secrets, and how to operate the system in a GDPR-compliant way.

---

## 1. Architecture Overview

| Component | Purpose | Stack |
|-----------|---------|-------|
| Android client (Compose) | Interactive maps, safest vs. fastest routing, crime overlays | Kotlin, Google Maps SDK, Heatmap utils |
| `SafeRoutePlanner` | Risk-aware routing engine with incremental caching | Kotlin, Google Directions API |
| Crime aggregation API | Polls the UK Police API, normalises incidents, exposes tile snapshots, moderation hooks | Python 3.11, FastAPI, SQLAlchemy, PostgreSQL/SQLite |
| Moderation/monitoring hooks | Text and media scanning, Discord alerts, manual review queue | External APIs (Perspective/OpenAI/GCP Vision), Discord webhooks |

The mobile app now streams crime tiles from the aggregation service instead of loading the entire UK dataset on-device. Tiles are cached client-side and synchronised incrementally with the route-risk engine.

---

## 2. Prerequisites

### Android / Kotlin side
- Android Studio Giraffe (or newer)
- JDK 17+
- Android SDK 34 with Google Play Services packages
- Google Maps & Directions API keys (store outside VCS via the Secrets Gradle plugin)

### Crime aggregation service
- Python 3.11+
- `pip` or `uv` for dependency management
- PostgreSQL 14+ (production) or SQLite (local default)
- Optional: Discord webhook URL, moderation API keys (Perspective/OpenAI/GCP Vision, etc.)

---

## 3. Repository Layout Highlights

```
app/                         Android application source
services/crime-aggregator/   FastAPI aggregation service and ingestion CLI
SETUP_AND_RUN.md             This document
SafeRouting-Documentation.md High-level product notes
```

---

## 4. Initial Checkout

```bash
git clone https://github.com/<your-org>/SafeRouting.git
cd SafeRouting
```

If you use Git LFS for media assets, install it before cloning.

---

## 5. Secrets & Configuration

### Android secrets
1. Create `secrets.properties` (ignored by Git) in the repo root:
   ```ini
   googleMapsKey=YOUR_MAPS_KEY
   directionsApiKey=YOUR_DIRECTIONS_KEY
   ```
2. The Gradle Secrets plugin merges these into `BuildConfig`. No keys should be committed to VCS.

### Aggregation service environment variables
Create `.env` (or set environment variables) inside `services/crime-aggregator/`:

```env
DATABASE_URL=postgresql+psycopg2://user:pass@host:5432/saferouting
RETENTION_DAYS=365
DISCORD_MODERATION_WEBHOOK=https://discord.com/api/webhooks/...
POLICE_API_BASE_URL=https://data.police.uk/api/
```

- For local testing, omit `DATABASE_URL` to fall back to SQLite (`crime.db`).
- Adjust `RETENTION_DAYS` to control how long official incidents stay active.

### Android ↔ backend connectivity
- The Android app reads `BuildConfig.CRIME_AGGREGATOR_BASE_URL`. Override it per build with:
  ```
  ./gradlew assembleDebug -PcrimeAggregatorBaseUrl=http://<host>:8000/
  ```
- Emulator default (`10.0.2.2`) already points to the host machine.

---

## 6. Crime Aggregation Service

### Installation
```bash
cd services/crime-aggregator
python -m venv .venv
source .venv/bin/activate
pip install -e .
```

### Database setup
- For PostgreSQL, create the database manually (e.g., `createdb saferouting`).
- Tables are auto-created on startup via SQLAlchemy metadata.

### Running the API locally
```bash
source .venv/bin/activate
export DATABASE_URL=sqlite:///crime.db  # optional
crime-aggregator-api
```

The service listens on `0.0.0.0:8000`. Endpoints:
- `GET /health`
- `GET /tiles/viewport?north=...&south=...&east=...&west=...&zoom=15`

### Ingesting Police data
Run the CLI to fetch 3 months of incidents for the default London grid:
```bash
source .venv/bin/activate
crime-aggregator-ingest --months 3
```
Schedule this nightly (cron, GitHub Actions, or a serverless job). The command de-duplicates by `persistent_id`, normalises crime categories, and updates tile revisions. Expired incidents (older than `RETENTION_DAYS`) are purged automatically.

### Moderation / Discord
- Every ingested incident can be mirrored to a Discord moderation channel by polling new rows and posting embeds (extend `service.py` as needed).
- Connect text moderation (Perspective/OpenAI) and image/video scanning (GCP Vision, Amazon Rekognition, Hive) before allowing user-generated reports.

---

## 7. Android Client

### Install dependencies
Android Studio will prompt for missing SDK packages. Ensure the following in **SDK Manager → SDK Tools**:
- Android SDK Build-Tools 34+
- Google Play services
- Google USB driver (for physical devices)

### Running the app
1. Start the aggregation API (`crime-aggregator-api`).
2. Launch an Android emulator (API 34, Google Play image) or connect a device.
3. Build & run from Android Studio, or via CLI:
   ```bash
   ./gradlew :app:installDebug
   adb shell am start -n com.universityofreading.demo/.MainActivity
   ```
4. Use the **Safest/Fastest** toggle to compare routes. Tile streaming will automatically adjust overlays and routing risk without reloading the entire dataset.

### Build configuration tips
- Override the aggregator base URL for production builds using Gradle properties (see §5).
- `SafeRoutePlanner` now caches route risk per tile revision; crime tile updates only invalidate affected routes, keeping UI responsive even with continuous streaming.

---

## 8. Data Lifecycle & GDPR Compliance

| Data type | Retention | Notes |
|-----------|-----------|-------|
| Official Police incidents | Default 365 days (configurable) | Automatically purged via `RETENTION_DAYS`. Older data never reaches clients, keeping map density manageable. |
| User-generated reports | Decide policy (recommend 180 days) | Store consent timestamps. Provide in-app deletion and DSAR workflows. |
| Media assets (avatars, report images/video) | Mirror data retention of parent report | Use object-storage lifecycle rules (e.g., S3, Supabase Storage) to auto-delete. |
| Discord moderation copies | Treat as ephemeral | Avoid sending PII when possible; ensure webhook channel is private and access-controlled. |

**GDPR checklist**
- Document lawful bases (legitimate interest + consent for uploads).
- Provide privacy policy, acceptable use policy, and data-retention statements within the app and landing page.
- Implement DSAR tooling (export & delete) by linking user IDs across the auth provider, aggregation DB, and storage bucket.
- Log moderation actions and who performed them for audit trails.

---

## 9. Hosting Recommendations

| Layer | Low-cost option | Notes |
|-------|-----------------|-------|
| Aggregation API | Fly.io, Render, Railway, or Azure Container Apps | Deploy FastAPI container with periodic ingestion job. Configure persistent Postgres (e.g., Supabase, Neon). |
| Database | Supabase (Postgres + storage) or Neon | Keep data in EU/UK regions to satisfy GDPR. Enable daily backups. |
| Media storage | Supabase Storage, Backblaze B2 + Cloudflare R2 | Enforce signed URLs and lifecycle policies for deletions. |
| Auth & user profiles | Firebase Auth or Supabase Auth | Provides social logins and passwordless flows out-of-the-box. |
| Moderation queues | Discord + lightweight admin panel | Use Discord for triage; store final decisions back in Postgres. |

For production, run ingestion via a scheduled serverless task (Supabase Edge Functions, AWS Lambda + EventBridge, GitHub Actions) to avoid manual runs.

---

## 10. Performance & Testing Checklist

- **Streaming tiles**: Verify cache hit rates and ensure `queueRefresh` only pulls when the viewport changes significantly. Use logcat with `DebugLogger` to confirm.
- **Routing**: Because `SafeRoutePlanner` now caches risk by tile revision, toggling safest/fastest should feel instantaneous unless new tiles arrive.
- **Fallback mode**: If the aggregation API is unreachable, the Android app falls back to bundled sample data via `CrimeDataRepository.loadCrimeData(context)`; logcat will display the error.
- **Instrumentation**: Add UI tests for tile streaming (mocking the REST API) and regression tests for route risk caching when you expand coverage.

---

## 11. Next Steps & Extensibility

- Hook the moderation pipeline into the aggregation API so that user-submitted reports enter the same tile system (flagged, pending review, approved).
- Expose `/stats/borough` and `/tiles/clusters` endpoints for analytics dashboards once requirements are finalised.
- Integrate Compose Multiplatform/iOS once the shared Kotlin module is extracted (current routing logic is already isolated).

---

Questions? Open an issue or reach out to the SafeRouting maintainers.
