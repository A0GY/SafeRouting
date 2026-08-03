# SafeRouting

SafeRouting is an Android navigation prototype that compares route efficiency with crime-aware risk estimates. It combines Google Maps route data with London crime records so users can inspect the fastest route, a lower-risk alternative, crime patterns, and regional statistics in one application.

The project was developed as a final-year Computer Science project at the University of Reading and was selected for showcase at a University Open Evening.

## Demo

https://github.com/user-attachments/assets/b18d078d-d6f0-47f9-9312-ee39b52f86ef

## Core features

- Compares safest and fastest route options between two selected map points
- Displays the chosen route with distance, duration, risk score, and alternative routes
- Visualises crime density as a heatmap at wider zoom levels
- Shows individual crime markers and clusters at closer zoom levels
- Filters map data by recent month, last three months, or all available data
- Simulates different times of day to show how the risk estimate changes
- Uses a drawn polygon to analyse crime counts, categories, severity, and density within a custom area
- Provides regional crime statistics and side-by-side borough comparisons
- Falls back to bundled London crime data when remote data is unavailable

## How the route model works

SafeRouting does not generate a road network from scratch. It requests alternative route geometries from Google Maps, evaluates each option, and ranks the available routes according to the selected mode.

For the completed on-device model:

1. Each route polyline is decoded and sampled at regular intervals.
2. An R-tree spatial index finds nearby crime records without scanning the full dataset for every sample.
3. Each nearby record contributes to the score according to distance, severity, crime category, and recency.
4. A time-of-day multiplier adjusts the estimate for the selected hour.
5. The fastest mode ranks routes by duration. The safest mode gives greater weight to estimated risk while still considering distance and duration.

The result is a comparative route score. It is not a prediction that a crime will occur and it should not be interpreted as a guarantee of personal safety.

## Architecture

| Area | Main responsibility | Key files |
|---|---|---|
| UI and navigation | Compose screens, map interaction, filters, route controls, and statistics | `ProgressiveMapScreen.kt`, `NavGraph.kt`, `CrimeStatsScreen.kt`, `CrimeCompareScreen.kt` |
| Route planning | Retrieves alternatives, ranks candidates, and prepares route metrics | `SafeRoutePlanner.kt`, `DirectionsClient.kt`, `RouteModels.kt` |
| Spatial analysis | Indexes crime points and calculates local or polygon-based risk estimates | `CrimeSpatialIndex.kt`, `AreaAnalysis.kt` |
| Data | Loads crime records from remote services with bundled JSON fallback | `CrimeDataRepository.kt`, `PoliceApiClient.kt` |
| Visualisation | Heatmaps, clustered markers, charts, route polylines, and time controls | `ProgressiveMapScreen.kt`, `CrimeCharts.kt`, `ui/theme/` |

## Technology

- Kotlin
- Android SDK 34, minimum SDK 24
- Jetpack Compose and Material 3
- Google Maps SDK for Android
- Google Maps Directions API
- Maps SDK for Android Utility Library
- R-tree spatial indexing
- Retrofit, OkHttp, and Gson
- Kotlin coroutines
- MPAndroidChart
- Gradle with Kotlin DSL

## Data flow

```text
Crime records -> repository -> spatial index -> route samples -> risk score
Google route alternatives -> route planner -> ranked routes -> map and route sheet
```

The project includes a direct client for the UK Police data API and bundled JSON data for offline fallback. The latest source snapshot also contains a backend-first migration for crime aggregation, map tiles, analytics, route scoring, routines, and incident reports.

## Running locally

### Requirements

- Android Studio with Android SDK 34
- JDK 17
- A Google Maps Platform API key with the required Maps and Directions services enabled
- An Android emulator or physical device running Android 7.0 or newer

### Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/A0GY/SafeRouting.git
   cd SafeRouting
   ```

2. Open the project in Android Studio.

3. Replace the `google_maps_key` value in `app/src/main/res/values/strings.xml` with your own restricted API key.

4. Sync the Gradle project and run the `app` configuration.

5. If you are testing the backend-first code, replace `API_BASE_URL` in `app/build.gradle.kts` with the URL of a compatible backend service.

## Project status

SafeRouting is an academic research prototype, not a production navigation or personal-safety service.

The completed final-year version performed crime retrieval, spatial indexing, and route scoring on the Android device. The latest `master` snapshot contains an experimental migration toward a backend-first design. The backend implementation is not included, and the configured backend URL is a placeholder. Backend-dependent screens therefore require a separate compatible service.

The final self-contained on-device implementation can be reviewed at commit [`c6457bb`](https://github.com/A0GY/SafeRouting/tree/c6457bb).

## Limitations

- Crime records are historical, delayed, and geographically approximate.
- Severity and category weights are modelling choices rather than official risk values.
- The safest result is selected only from the route alternatives returned by Google.
- Results depend on data coverage, the selected time, and the configured scoring parameters.
- The current backend migration is incomplete without its separate service.
