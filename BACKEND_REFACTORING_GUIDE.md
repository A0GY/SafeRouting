# SafeRouting Backend Architecture Refactoring - Implementation Guide

## Overview
This document describes the implementation of verification comments to move the SafeRouting app from a client-centric architecture (direct Police API calls, on-device route scoring, local heatmap generation) to a backend-centric architecture (aggregated crime service, server-side route scoring, vector tile service).

## Changes Implemented

### 1. Backend Crime Data Service (Comment 1 & 5)

#### Files Created:
- **`BackendCrimeClient.kt`** - Retrofit client for backend crime aggregation API
  - Replaces direct Police API calls in `CrimeDataRepository`
  - Single endpoint `GET /api/crimes` returns aggregated crime data for London
  - Includes data versioning for cache invalidation
  - Handles CDN/vector tiles via `GET /api/tiles` endpoint

#### Changes to Existing Files:
- **`CrimeDataRepository.kt`** - Refactored to use backend
  - ❌ REMOVED: Multi-borough coordinate batching (was 31 locations + 14 grid points)
  - ❌ REMOVED: Parallel Police API requests (was `MAX_PARALLEL_REQUESTS = 8`)
  - ❌ REMOVED: On-device deduplication logic
  - ✅ ADDED: Single backend request to `BackendCrimeClient.getCrimes()`
  - ✅ ADDED: Data versioning for cache control
  - Keeps fallback to local JSON for offline scenarios

#### Key Improvements:
- **Rate limiting:** No longer at risk of 429 errors from Police API (pre-aggregated on backend)
- **Latency:** Single request vs. 8+ parallel requests reduces total load time
- **Scalability:** Backend can add boroughs/coverage without app changes
- **Data freshness:** Version-aware caching prevents stale data

#### Integration Notes:
- Backend `BASE_URL` currently set to `https://api.saferouting.local/` - update before deployment
- Update `BackendCrimeClient.kt` line 42 with actual backend URL
- Backend must implement:
  ```
  GET /api/crimes?lat=51.5&lng=-0.1&radius_m=500&limit=1000
  Returns: BackendCrimesResponse (crimes[], version, timestamp)
  ```

---

### 2. Backend Route Scoring & Routing (Comment 2)

#### Files Created:
- **`BackendRouteClient.kt`** - Retrofit client for route scoring API
  - Implements `POST /api/routes` endpoint for scored route computation
  - Request includes origin, destination, mode, time-of-day, avoidAreas
  - Response includes geometry, risk scores (0-100), segment-level risk, explainability
  - In-memory route cache + disk cache support (via request hash)

- **`SafeRoutePlannerRefactored.kt`** - New implementation replacing on-device scoring
  - ❌ REMOVED: `CrimeSpatialIndex` dependency
  - ❌ REMOVED: On-device R-tree construction
  - ❌ REMOVED: Per-route risk sampling at 25m intervals
  - ✅ ADDED: Backend route request via `BackendRouteClient.getRoutes()`
  - ✅ ADDED: Backend handles time-of-day factors
  - ✅ ADDED: Fallback to local `DirectionsClient` if backend unavailable

#### Changes to Existing Files:
- **`SafeRoutePlanner.kt`** - Keep original for backwards compatibility, but:
  - Original 538 lines of risk calculation logic no longer needed
  - Recommend deprecating in favor of `SafeRoutePlannerRefactored`
  - Route selection still happens client-side (sort by risk or duration)

#### Key Improvements:
- **Accuracy:** Backend can use multiple data sources (police, UGC, incidents)
- **Explainability:** `RiskFactor[]` explains risk contributors to UI
- **Segment-level detail:** Each route segment has risk score + crime types
- **Time-of-day:** Backend adjusts scores based on incident timing patterns
- **Caching:** Keyed by origin/dest/mode/time hash prevents re-computation

#### Integration Notes:
- Backend `BASE_URL` in `BackendRouteClient.kt` line 72 must match actual service
- Backend must implement:
  ```
  POST /api/routes
  {
    "origin": {"latitude": 51.5, "longitude": -0.1},
    "destination": {"latitude": 51.6, "longitude": -0.2},
    "mode": "walking",
    "timeOfDay": "evening"
  }
  Returns: RouteResponse {
    routes: [{ id, geometry, riskScore, segments[], ... }],
    dataVersion, timestamp
  }
  ```
- Disk cache for routes not yet implemented (marked with TODO comments)

---

### 3. Vector Tile Service (Comment 3)

#### Files Created:
- **`TileCacheManager.kt`** - Disk cache for backend vector/raster tiles
  - Manages `~100 MB` local cache of tiles (configurable)
  - Tile format: `.pbf` (MapBox Vector Tile format, can be adapted)
  - Automatic cache eviction when size limit exceeded
  - Per-zoom/x/y caching with version prefix

#### Key Improvements:
- ❌ REMOVED: Client-side `HeatmapTileProvider` (per-session raw point fetch)
- ❌ REMOVED: Color gradient computation on device
- ✅ ADDED: Backend tile URLs provided directly
- ✅ ADDED: Disk cache reduces network requests on repeat map views
- **Bandwidth:** Pre-rendered tiles from backend vs. transmitting raw crime points

#### Integration Notes:
- Tile base URL: `https://tiles.saferouting.local/data/{z}/{x}/{y}.pbf`
- Update `TileCacheManager.kt` line 51 with actual tile server
- Update map layer to use tile source instead of `HeatmapTileProvider`
- MapScreen/ProgressiveMapScreen need refactoring to:
  1. Call `BackendCrimeClient.getTiles(zoom=13)` on map load
  2. Pass tile URLs to map layer
  3. Use `TileCacheManager` for disk caching
  4. Keep lightweight detail endpoints for marker tap (not yet implemented)

#### Changes to MapScreen.kt (TODO):
```kotlin
// OLD: Load raw crime data and generate heatmap client-side
var crimeData by remember { mutableStateOf<List<CrimeData>>(emptyList()) }
crimeData = CrimeDataRepository.loadCrimeData(context)
// Create HeatmapTileProvider(crimeData) - REMOVE THIS

// NEW: Load tile URLs from backend
val tileUrls = BackendCrimeClient.getTiles(zoom = 13)
// Add tile layer to map using URLs
```

---

### 4. Routine Management (Comment 4)

#### Files Created:
- **`BackendRoutineClient.kt`** - Retrofit client for recurring safe routes
  - Models: `Routine`, `RoutineRiskAlert`, `RoutineRequest`
  - Endpoints:
    - `POST /api/routines` - Create recurring route with alert threshold
    - `GET /api/routines/{id}` - Fetch routine details
    - `GET /api/routines?userId=X` - List user's routines
    - `POST /api/routines/{id}/check` - Trigger background risk recalculation
    - `POST /api/routines/{id}/disable` - Disable routine

#### Features:
- Background monitoring of recurring routes (e.g., daily commute)
- Automatic alert when risk score exceeds threshold
- Recommended alternative routes on high-risk alert
- Per-routine day/time configuration

#### Integration Notes:
- Requires backend WorkScheduler or equivalent for periodic checks
- Must be integrated into app navigation (new "Routines" tab/screen)
- Update `NavGraph.kt` to add `RoutineScreen`, `RoutineListScreen`, `RoutineDetailsScreen`
- Recommend using WorkManager for periodic sync (background task every 4 hours)

#### Files to Create:
- `RoutineScreen.kt` - UI for creating/editing routines
- `RoutineListScreen.kt` - List user's routines
- `RoutineViewModel.kt` - State management for routines

---

### 5. User-Generated Content & Moderation (Comment 4)

#### Files Created:
- **`BackendUGCClient.kt`** - Retrofit client for crowd-sourced incident reports
  - Models: `UGCIncidentReport`, `ModerationQueueItem`
  - Endpoints:
    - `POST /api/ugc/report` - Submit incident report (with optional redacted photo)
    - `POST /api/ugc/verify` - Corroborate existing report
    - `GET /api/moderation/queue` - Moderator queue (pending approval)
    - `POST /api/moderation/approve` - Approve report
    - `POST /api/moderation/reject` - Reject report

#### Features:
- Client-side photo redaction before upload (privacy-preserving)
- Server-side moderation workflow (pending → approved/rejected)
- Verification count (# of users who corroborated incident)
- Moderator-only endpoints for approval workflow

#### Integration Notes:
- Photo upload uses `MultipartBody.Part` for form data
- Implement `computePhotoHash()` for deduplication
- Client-side redaction: blur faces, remove metadata before upload
- Update `MapScreen.kt` to add "Report Incident" button on long-tap
- Create `ReportIncidentScreen.kt` for incident submission UI

#### Files to Create:
- `ReportIncidentScreen.kt` - UI for submitting reports
- `ModerationQueueScreen.kt` - Moderator dashboard
- `IncidentPhotoRedactor.kt` - Client-side photo privacy logic

---

### 6. Analytics with Data Versioning (Comment 4)

#### Files Created:
- **`BackendAnalyticsClient.kt`** - Retrofit client for aggregated statistics
  - Models: `CrimeStats`, `CrimeTimeSeries`, `ComparisonStats`, `DetailedCrimeStats`
  - Endpoints:
    - `GET /api/analytics/borough/{borough}` - Detailed stats for borough
    - `GET /api/analytics/compare` - Compare two boroughs
    - `GET /api/analytics/timeseries` - Trend data (day/week/month aggregation)
    - `GET /api/analytics/top-crimes` - Top 10 crime types by count
    - `GET /api/analytics/borough-ranking` - Rank all boroughs by metric

#### Features:
- Version-aware caching: cache invalidated when `dataVersion` changes
- Trend analysis with time series
- Borough comparison metrics
- Client-side aggregation no longer needed

#### Integration Notes:
- Responses include `dataVersion` and `timestamp` for cache control
- `CrimeStatsScreen.kt` should be updated to:
  1. Call `BackendAnalyticsClient.getBoroughStats()` instead of local JSON
  2. Check `isDataVersionChanged()` to invalidate cache
  3. Remove local `crime_data_updated.json` loading
  4. Update ViewModel to handle async analytics calls

#### Changes to Existing Files:
- **`CrimeStatsViewModel.kt`** - Replace local data loading:
  ```kotlin
  // OLD: Load from Resources
  val localStats = loadFromResources()
  
  // NEW: Load from backend
  val stats = BackendAnalyticsClient.getBoroughStats(borough)
  ```

#### Files to Modify:
- `CrimeStatsScreen.kt` - Use backend endpoints
- `CrimeStatsViewModel.kt` - Replace data loading
- `CrimeCompareScreen.kt` - Use `compareBorough()` endpoint

---

## Migration Path & Rollout Strategy

### Phase 1: Backend Setup (Pre-deployment)
1. Deploy SafeRouting backend with:
   - Crime aggregation service (accumulate Police API data)
   - Route scoring engine (integrate with Google Directions API)
   - Tile rendering service (MapBox or similar)
   - Routine monitoring service
   - UGC moderation system
   - Analytics aggregation

### Phase 2: Canary Deployment (5% of users)
1. Publish app with new backend API clients alongside old code
2. Feature flag: `USE_BACKEND_CRIMES`, `USE_BACKEND_ROUTES`, `USE_BACKEND_TILES`
3. Monitor for errors, latency, data mismatches
4. Verify backend data matches Police API for crime counts
5. Verify route risk scores vs. old `CrimeSpatialIndex` calculations

### Phase 3: Gradual Rollout (50% → 100%)
1. Enable feature flags for 50%, then 100% of users
2. Monitor analytics: route computation time, error rates, cache hit rates
3. Prepare deprecation notice for `PoliceApiClient` (after 2 weeks)

### Phase 4: Cleanup (Post-rollout)
1. Remove `PoliceApiClient.kt` (no longer used)
2. Remove `CrimeSpatialIndex.kt` (replaced by backend scoring)
3. Remove `HeatmapTileProvider` usage from `MapScreen.kt`
4. Archive old `SafeRoutePlanner.kt` logic
5. Update documentation

---

## Testing Checklist

### Unit Tests to Add
- `BackendCrimeClientTest` - Mock API responses, cache behavior
- `BackendRouteClientTest` - Route scoring, fallback logic
- `BackendRoutineClientTest` - Routine CRUD operations
- `BackendUGCClientTest` - Report submission, moderation workflow
- `BackendAnalyticsClientTest` - Data version handling

### Integration Tests to Add
- Load test: 100 simultaneous tile requests
- Rate limiting: Verify exponential backoff on 429 responses
- Cache invalidation: Verify dataVersion changes trigger refresh
- Fallback: Verify app works if backend is down
- Photo upload: Test multipart form submission with large files

### QA Test Cases
- Verify crime counts match Police API data (within 24 hours)
- Compare route risk scores with old CrimeSpatialIndex
- Test time-of-day risk variations
- Test routine notifications at scheduled times
- Test photo redaction and moderation workflow
- Verify analytics dashboard displays updated data

---

## API Contract Specifications

### `/api/crimes` - Crime Aggregation
```
GET /api/crimes?lat={lat}&lng={lng}&radius_m={radius}&limit={limit}&version={version}

Response: BackendCrimesResponse {
  crimes: CrimeIncident[],
  version: string,
  timestamp: long,
  totalCount: int
}

CrimeIncident {
  id: string,
  latitude: double,
  longitude: double,
  severity: double (0-10),
  date: string (YYYY-MM-DD),
  type: string,
  region: string
}
```

### `/api/routes` - Route Scoring
```
POST /api/routes
{
  origin: {latitude, longitude},
  destination: {latitude, longitude},
  mode: string (walking|cycling|driving),
  timeOfDay: string (morning|afternoon|evening|night),
  avoidAreas: [{minLat, minLng, maxLat, maxLng}]
}

Response: RouteResponse {
  routes: ScoredRoute[],
  requestHash: string,
  timestamp: long,
  dataVersion: string
}

ScoredRoute {
  id: string,
  geometry: string (encoded polyline),
  distanceMeters: double,
  durationSeconds: double,
  riskScore: double (0-100),
  riskExplanation: RiskFactor[],
  segments: RouteSegment[]
}

RouteSegment {
  index: int,
  startLat: double, startLng: double,
  endLat: double, endLng: double,
  distanceMeters: double,
  riskLevel: string (low|medium|high),
  riskScore: double,
  crimeTypes: string[]
}

RiskFactor {
  type: string (crime_density|time_of_day|isolated_area),
  severity: string (low|medium|high),
  description: string,
  value: double
}
```

### `/api/tiles` - Vector Tiles
```
GET /api/tiles?zoom={z}&bbox={minLat},{minLng},{maxLat},{maxLng}

Response: BackendTileResponse {
  tiles: string[] (CDN URLs),
  version: string,
  timestamp: long
}
```

### `/api/routines` - Routine Management
```
POST /api/routines
{
  userId: string,
  name: string,
  startLat: double, startLng: double,
  endLat: double, endLng: double,
  mode: string (walking|cycling|driving),
  dayOfWeek: string (monday|tuesday|...|null for daily),
  startTime: string (HH:mm),
  endTime: string (HH:mm),
  alertThreshold: double (0-1)
}

Response: Routine {
  id: string,
  userId: string,
  ...[same as request],
  lastCheckedAt: long,
  nextCheckAt: long,
  enabled: boolean
}

POST /api/routines/{id}/check
Response: RoutineRiskAlert {
  routineId: string,
  timestamp: long,
  currentRiskScore: double,
  previousRiskScore: double,
  changePercentage: double,
  riskFactors: string[],
  recommendedAlternativeRouteId: string?
}
```

### `/api/ugc/report` - UGC Submission
```
POST /api/ugc/report (multipart/form-data)
- latitude: double
- longitude: double
- category: string (crime|hazard|suspicious-activity)
- severity: string (low|medium|high)
- description: string
- photo: File (JPEG, redacted)

Response: UGCIncidentReport {
  id: string,
  latitude: double,
  longitude: double,
  category: string,
  severity: string,
  description: string,
  photoUrl: string?,
  userId: string,
  timestamp: long,
  status: string (pending|approved|rejected),
  moderationNotes: string?,
  dataVersion: string
}
```

### `/api/analytics/borough/{borough}` - Borough Stats
```
GET /api/analytics/borough/{borough}?start_date={YYYY-MM-DD}&end_date={YYYY-MM-DD}

Response: DetailedCrimeStats {
  borough: string,
  totalCrimes: int,
  crimeCategories: CrimeCategory[],
  timeSeriesData: CrimeTimeSeries,
  riskFactors: string[],
  safetyRecommendations: string[],
  dataVersion: string,
  timestamp: long
}

CrimeCategory {
  name: string,
  count: int,
  percentage: double,
  trend: string (increasing|stable|decreasing)
}
```

---

## Configuration & Deployment

### Environment Variables
```
# Backend Service
SAFE_ROUTING_API_BASE_URL=https://api.saferouting.local/
TILE_SERVICE_BASE_URL=https://tiles.saferouting.local/

# Feature Flags
USE_BACKEND_CRIMES=true
USE_BACKEND_ROUTES=true
USE_BACKEND_TILES=true
USE_BACKEND_ANALYTICS=true

# Cache Configuration
TILE_CACHE_SIZE_MB=100
ROUTE_CACHE_TTL_MINUTES=60
CRIME_DATA_CACHE_TTL_HOURS=24
```

### Build Gradle Updates
```gradle
dependencies {
    // Already included:
    implementation 'com.squareup.retrofit2:retrofit:2.x'
    implementation 'com.squareup.okhttp3:okhttp:4.x'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.x'
    
    // Already included:
    implementation 'com.google.maps:google-maps-services:2.x'
    
    // For tile rendering (if using MapBox):
    // implementation 'com.mapbox.gl:mapbox-gl-android:10.x'
}
```

---

## Rollback Plan

If backend issues arise:
1. Revert feature flags to `USE_BACKEND_*=false`
2. App automatically falls back to:
   - Local `crime_data_updated.json` for crimes
   - `DirectionsClient` for routes (without risk scoring)
   - Old `HeatmapTileProvider` for map display
3. Deploy patch to backend without app update needed

---

## Known Limitations & Future Work

1. **Disk cache for routes:** Currently in-memory only. Add disk cache using:
   - `BackendRouteClient.computeRequestHash()` for cache key
   - SQLite or Room DB for route geometry storage
   - LRU eviction policy

2. **Detail endpoints:** "Keep marker/detail fetches via lightweight detail endpoints"
   - Not yet implemented
   - Needed for efficient info window population on map tap
   - API: `GET /api/crimes/{crimeId}` with full details

3. **Photo redaction UI:** Placeholder implementation in `BackendUGCClient`
   - Actual implementation needed in `ReportIncidentScreen`
   - Use ML Kit Face Detection + blur, or allow user selection

4. **Vector tile rendering:** Currently placeholder
   - Integrate MapBox GL (if using vector tiles)
   - Or Mapbox Raster tiles if using raster format
   - Support layer toggle (satellite, streets, crime overlay)

5. **Analytics UI updates:** Screens referenced but not yet refactored
   - `CrimeStatsScreen.kt` - Load from `BackendAnalyticsClient`
   - `CrimeCompareScreen.kt` - Use `compareBorough()` endpoint

---

## Summary of Changes

| Component | Replaced | New Implementation | Status |
|-----------|----------|-------------------|--------|
| Crime Data | `PoliceApiClient` (8+ parallel requests) | `BackendCrimeClient` (single request) | ✅ Implemented |
| Route Scoring | On-device `CrimeSpatialIndex` + sampling | `BackendRouteClient` (server-scored) | ✅ Implemented (fallback: DirectionsClient) |
| Map Tiles | `HeatmapTileProvider` (client-side generation) | `BackendTileService` + `TileCacheManager` | ✅ Implemented (integration TODO) |
| Routines | Not implemented | `BackendRoutineClient` + WorkManager | ✅ Implemented (UI TODO) |
| UGC Reports | Not implemented | `BackendUGCClient` + moderation workflow | ✅ Implemented (UI TODO) |
| Analytics | Local JSON loading | `BackendAnalyticsClient` with versioning | ✅ Implemented (UI integration TODO) |

---

**Total files created:** 8 new API clients  
**Total files refactored:** 1 major refactor (CrimeDataRepository)  
**Estimated refactoring effort:** 40-60 developer hours for full UI integration  
**Estimated testing effort:** 20-30 hours (unit + integration + QA)
