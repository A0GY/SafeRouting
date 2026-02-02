# Quick Reference: Backend API Clients

## Files Created

### 1. Crime Data APIs
- **`BackendCrimeClient.kt`** - Crime aggregation + vector tiles
  - Class: `object BackendCrimeClient`
  - Key methods:
    - `suspend fun getCrimes()` - Fetch aggregated crimes
    - `suspend fun getTiles()` - Fetch tile URLs
    - `suspend fun getAggregatedCrimes()` - Fetch aggregated stats

### 2. Routing APIs  
- **`BackendRouteClient.kt`** - Route scoring + geometry
  - Class: `object BackendRouteClient`
  - Key methods:
    - `suspend fun getRoutes()` - Get scored routes
    - `fun getCachedRoute()` - Retrieve from cache
    - `suspend fun getRoutesBatch()` - Batch requests
    - `fun computeRequestHash()` - Cache key generation

### 3. Tile Caching
- **`TileCacheManager.kt`** - Disk cache for map tiles
  - Class: `class TileCacheManager(cacheDir: File)`
  - Key methods:
    - `suspend fun getTile()` - Get from cache or fetch
    - `fun clearCache()` - Clear all tiles
    - `fun evictOldTiles()` - LRU eviction
    - `fun getCacheSize()` - Check size
  - Also: `object BackendTileService` - Tile URL configuration

### 4. Routine Management
- **`BackendRoutineClient.kt`** - Recurring safe routes
  - Class: `object BackendRoutineClient`
  - Key methods:
    - `suspend fun createRoutine()` - Create recurring route
    - `suspend fun listRoutines()` - Get user's routines
    - `suspend fun checkRoutineRisk()` - Trigger risk check

### 5. UGC & Moderation
- **`BackendUGCClient.kt`** - Crowd-sourced incident reports
  - Class: `object BackendUGCClient`
  - Key methods:
    - `suspend fun submitReport()` - Upload incident
    - `suspend fun verifyReport()` - Corroborate incident
    - `suspend fun getModerationQueue()` - Moderator view
    - `fun computePhotoHash()` - Privacy-preserving hash

### 6. Analytics
- **`BackendAnalyticsClient.kt`** - Aggregated statistics
  - Class: `object BackendAnalyticsClient`
  - Key methods:
    - `suspend fun getBoroughStats()` - Borough details
    - `suspend fun compareBorough()` - Compare two boroughs
    - `suspend fun getTimeSeries()` - Trend data
    - `suspend fun getBoroughRanking()` - All boroughs ranked

### 7. Route Planning (Refactored)
- **`SafeRoutePlannerRefactored.kt`** - Backend-driven routes
  - Class: `object SafeRoutePlanner` (same name, different logic)
  - Key methods:
    - `suspend fun safestRoute()` - Get backend-scored routes
    - `fun setSimulatedHour()` - For testing

---

## Integration Guide

### Step 1: Add Crime Data to Map
```kotlin
// In MapScreen.kt
val tileUrls = BackendCrimeClient.getTiles(zoom = 13)
val tileCache = TileCacheManager(context.cacheDir)
// Add tiles to map
```

### Step 2: Get Scored Routes
```kotlin
// In DirectionsClient or MapScreen
val routes = BackendRouteClient.getRoutes(
    originLat = origin.latitude,
    originLng = origin.longitude,
    destLat = dest.latitude,
    destLng = dest.longitude,
    mode = "walking"
)
```

### Step 3: Update Analytics
```kotlin
// In CrimeStatsViewModel
val stats = BackendAnalyticsClient.getBoroughStats(borough)
```

### Step 4: Create Routine (Future)
```kotlin
// In new RoutineScreen
val routine = BackendRoutineClient.createRoutine(
    userId = userId,
    name = "Daily Commute",
    startLat = 51.5,
    startLng = -0.1,
    endLat = 51.6,
    endLng = -0.2,
    mode = "walking",
    dayOfWeek = "monday", // Repeats on Mondays only
    startTime = "08:30",
    endTime = "09:00"
)
```

### Step 5: Submit UGC Report (Future)
```kotlin
// In ReportIncidentScreen
val report = BackendUGCClient.submitReport(
    latitude = mapLat,
    longitude = mapLng,
    category = "crime",
    severity = "high",
    description = "Saw suspicious activity",
    redactedPhotoBitmap = userPhoto  // Client redacted
)
```

---

## Backend API Contracts

### Endpoint: GET /api/crimes
```
Query Params:
  lat (double) - Latitude
  lng (double) - Longitude
  radius_m (int, default 500) - Search radius
  limit (int, default 1000) - Max results
  version (string, optional) - For cache validation

Response:
{
  "crimes": [
    {
      "id": "crime123",
      "latitude": 51.5074,
      "longitude": -0.1278,
      "severity": 7.5,
      "date": "2024-01-15",
      "type": "robbery",
      "region": "Westminster"
    }
  ],
  "version": "v1.2024-01-15",
  "timestamp": 1234567890,
  "totalCount": 1500
}
```

### Endpoint: POST /api/routes
```
Request Body:
{
  "origin": {"latitude": 51.5074, "longitude": -0.1278},
  "destination": {"latitude": 51.6000, "longitude": -0.2000},
  "mode": "walking",
  "timeOfDay": "evening",
  "avoidAreas": [
    {"minLat": 51.51, "minLng": -0.13, "maxLat": 51.52, "maxLng": -0.12}
  ]
}

Response:
{
  "routes": [
    {
      "id": "route1",
      "geometry": "<encoded polyline>",
      "distanceMeters": 2500,
      "durationSeconds": 1800,
      "riskScore": 35.5,
      "riskExplanation": [
        {
          "type": "crime_density",
          "severity": "medium",
          "description": "Moderate crime density in route area",
          "value": 12.0
        }
      ],
      "segments": [
        {
          "index": 0,
          "startLat": 51.5074,
          "startLng": -0.1278,
          "endLat": 51.5100,
          "endLng": -0.1300,
          "distanceMeters": 400,
          "riskLevel": "low",
          "riskScore": 15.0,
          "crimeTypes": ["theft"]
        }
      ]
    }
  ],
  "requestHash": "abc123def456",
  "timestamp": 1234567890,
  "dataVersion": "v2.2024-01-15"
}
```

### Endpoint: GET /api/tiles
```
Query Params:
  zoom (int, default 13) - Tile zoom level
  bbox (string, optional) - "minLat,minLng,maxLat,maxLng"

Response:
{
  "tiles": [
    "https://tiles.saferouting.local/data/13/4096/2731.pbf",
    "https://tiles.saferouting.local/data/13/4097/2731.pbf",
    ...
  ],
  "version": "v3.2024-01-15",
  "timestamp": 1234567890
}
```

### Endpoint: GET /api/analytics/borough/{borough}
```
Query Params:
  start_date (string, optional) - "YYYY-MM-DD"
  end_date (string, optional) - "YYYY-MM-DD"

Response:
{
  "borough": "Westminster",
  "totalCrimes": 4500,
  "crimeCategories": [
    {
      "name": "theft",
      "count": 1200,
      "percentage": 26.7,
      "trend": "increasing"
    }
  ],
  "timeSeriesData": {
    "dates": ["2024-01-01", "2024-01-02", ...],
    "values": [150, 145, ...],
    "borough": "Westminster"
  },
  "riskFactors": ["high_theft_density", "evening_risk"],
  "safetyRecommendations": [
    "Avoid Bond Street station between 21:00-23:00",
    "Use main streets during evening hours"
  ],
  "dataVersion": "v1.2024-01-15",
  "timestamp": 1234567890
}
```

---

## Common Use Cases

### Use Case 1: Load Crime Map
```kotlin
// OLD
val crimeData = CrimeDataRepository.loadCrimeData(context)  // 31+ API calls
val index = CrimeSpatialIndex(crimeData)
val heatmapProvider = HeatmapTileProvider.Builder().data(...).build()

// NEW
val tileUrls = BackendCrimeClient.getTiles(zoom = 13)  // 1 API call
val tileCache = TileCacheManager(context.cacheDir)
// Add tile layer to map
```

### Use Case 2: Get Safe Route
```kotlin
// OLD
val routes = DirectionsClient.getAlternatives(origin, dest, isSafest)
val scoredRoutes = routes.map { route ->
    val riskScore = calculateRiskOnDevice(route)  // CPU-intensive
    RouteCandidate(route, riskScore, distance)
}

// NEW
val routes = BackendRouteClient.getRoutes(
    originLat, originLng, destLat, destLng, mode = "walking"
)
// Routes already scored by backend with explainability
```

### Use Case 3: Display Crime Statistics
```kotlin
// OLD
val stats = loadFromResources(R.raw.crime_data_updated)

// NEW
val stats = BackendAnalyticsClient.getBoroughStats("Westminster")
// Auto-refreshed based on dataVersion
```

### Use Case 4: Monitor Commute (Future)
```kotlin
// Create routine
val routine = BackendRoutineClient.createRoutine(
    userId = userId,
    name = "Daily Commute",
    startLat = home.lat, startLng = home.lng,
    endLat = work.lat, endLng = work.lng,
    startTime = "08:30",
    dayOfWeek = "monday"  // Repeats daily
)

// Later, backend checks risk periodically and alerts if needed
```

---

## Error Handling

### All Clients Use Standard Fallback
```kotlin
try {
    val data = BackendSomeClient.someMethod()
    // Use data
} catch (e: Exception) {
    DebugLogger.logError(TAG, "Error: ${e.message}", e)
    // Return default/cached data or empty
    return emptyList()
}
```

### Graceful Degradation
- Crime data: Falls back to local JSON
- Routes: Falls back to DirectionsClient (no risk scoring)
- Tiles: No tiles displayed (backend unavailable)
- Routines: Cannot create (backend required)
- UGC: Cannot submit (backend required)

---

## Configuration

### Update Backend URL (Before Deployment)
```kotlin
// In BackendCrimeClient.kt
private const val BASE_URL = "https://api.saferouting.local/"

// In BackendRouteClient.kt
private const val BASE_URL = "https://api.saferouting.local/"

// In TileCacheManager.kt
private const val TILE_BASE_URL = "https://tiles.saferouting.local/data/{z}/{x}/{y}.pbf"

// In BackendRoutineClient.kt
private const val BASE_URL = "https://api.saferouting.local/"

// In BackendUGCClient.kt
private const val BASE_URL = "https://api.saferouting.local/"

// In BackendAnalyticsClient.kt
private const val BASE_URL = "https://api.saferouting.local/"
```

---

## Testing

### Mock Responses for Unit Tests
```kotlin
// Example: Test crime loading
@Test
fun testLoadCrimesFromBackend() {
    val mockResponse = BackendCrimesResponse(
        crimes = listOf(
            CrimeIncident(
                id = "test1",
                latitude = 51.5,
                longitude = -0.1,
                severity = 5.0,
                date = "2024-01-15",
                type = "theft",
                region = "Westminster"
            )
        ),
        version = "v1.test",
        timestamp = System.currentTimeMillis(),
        totalCount = 1
    )
    
    // Mock Retrofit to return mockResponse
    // Assert BackendCrimeClient.getCrimes() returns expected data
}
```

---

## Performance Expectations

| Operation | Before | After | Improvement |
|-----------|--------|-------|-------------|
| Load crimes | 8-15 sec | 2-3 sec | 5-6x faster |
| Score routes | 3-5 sec | 1-2 sec | 2-3x faster |
| Load tiles | 5-10 sec | 1 sec | 5-10x faster |
| Memory (crime) | ~200 MB | ~50 MB | 4x less |
| API calls | 31-45 | 1-3 | 15x fewer |

---

## Checklist for Integration

- [ ] Backend services deployed with API contracts
- [ ] Feature flags configured in BuildConfig
- [ ] CrimeDataRepository refactored (✅ done)
- [ ] MapScreen/ProgressiveMapScreen tile integration
- [ ] CrimeStatsScreen analytics integration
- [ ] RoutineScreen creation and WorkManager setup
- [ ] ReportIncidentScreen UGC integration
- [ ] Unit tests for all new clients
- [ ] Integration tests with live backend
- [ ] QA testing and data validation
- [ ] Canary rollout (5% users)
- [ ] Full rollout (100% users)
- [ ] Monitor error rates, latency, cache hit rates
- [ ] Deprecate old code if rollout successful

---

**Last Updated**: February 2, 2026  
**Backend Architecture Version**: 2.0  
**Compatibility**: Android 6.0+ (API 23+)
