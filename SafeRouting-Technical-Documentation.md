# Safe Route Planning Application - Technical Documentation

## 1. Project Overview

The Safe Route Planning application is a sophisticated Android app designed to help users navigate urban areas safely by considering crime data in route planning decisions. The application uses real-time crime data, spatial analysis, and routing algorithms to suggest the safest paths between destinations.

## 2. System Architecture

The application follows a modular architecture with these main components:

- **UI Layer**: Built with Jetpack Compose
- **Data Layer**: Handles crime data loading and management
- **Navigation Layer**: Provides routing algorithms and spatial analysis
- **Utility Layer**: Helper functions and debugging tools

## 3. Core Data Models

### 3.1 CrimeData (`app/src/main/java/com/universityofreading/demo/data/CrimeData.kt`)

The foundation data model used throughout the application:

```kotlin
data class CrimeData(
    val latitude: Double,
    val longitude: Double,
    val severity: Double,
    val date: String,  // Format: "YYYY-MM-DD"
    val type: String,  // Human-readable crime type
    val region: String // London borough or area name
)
```

### 3.2 AreaAnalysis (`app/src/main/java/com/universityofreading/demo/navigation/AreaAnalysis.kt`)

Used for displaying crime statistics for a user-defined area:

```kotlin
data class AreaAnalysis(
    val crimeTypeCounts: Map<String, Int>,
    val crimeCount: Int,
    val averageSeverity: Double,
    val riskPercentage: Int,
    val highRiskCrimeCount: Int
)
```

### 3.3 RouteCandidate (`app/src/main/java/com/universityofreading/demo/navigation/RouteModels.kt`)

Represents possible routes with safety and distance metrics:

```kotlin
data class RouteCandidate(
    val route: DirectionsRoute,
    val riskScore: Double,
    val distanceM: Double,
    val durationS: Double = 0.0,
    val highRiskSegments: Int = 0
) {
    // Legacy cost calculation method
    fun cost(weight: Double, maxDist: Double): Double =
        weight * riskScore + (1 - weight) * (distanceM / maxDist)
}
```

## 4. Key Components and Features

### 4.1 Spatial Analysis (`app/src/main/java/com/universityofreading/demo/navigation/CrimeSpatialIndex.kt`)

The `CrimeSpatialIndex` class uses an R-tree data structure for efficient spatial queries of crime data:

```kotlin
class CrimeSpatialIndex(crimes: List<CrimeData>) {
    // Build an immutable R‑tree filled with all crime points
    private val tree: RTree<CrimeData, Point> =
        crimes.fold(RTree.star().create()) { acc, crime ->
            acc.add(
                crime,
                Geometries.pointGeographic(crime.longitude, crime.latitude)
            )
        }
    
    // Calculate risk at a specific location
    fun riskAt(pos: LatLng, radiusM: Double = RISK_RADIUS_METERS): Double { ... }
    
    // Analyze the risk for a user-defined area
    fun analyzeAreaRisk(polygon: List<LatLng>): AreaAnalysis { ... }
}
```

The class includes sophisticated algorithms for:
- Risk calculation based on crime severity, distance, recency, and type
- Time-based risk adjustments (higher at night)
- Spatial queries within polygonal areas
- Customizable weighting for different crime types

### 4.2 Route Planning (`app/src/main/java/com/universityofreading/demo/navigation/SafeRoutePlanner.kt`)

The `SafeRoutePlanner` object provides intelligent routing capabilities:

```kotlin
object SafeRoutePlanner {
    // Calculate the safest or fastest route
    suspend fun safestRoute(
        origin: LatLng,
        dest: LatLng,
        isSafestMode: Boolean,
        index: CrimeSpatialIndex
    ): Pair<RouteCandidate, List<RouteCandidate>> { ... }
    
    // Calculate risk score for a route
    private suspend fun calculateRouteRiskAndSegments(
        route: DirectionsRoute,
        index: CrimeSpatialIndex,
        timeMultiplier: Double
    ): Pair<Double, Int> { ... }
}
```

Key features include:
- Dual routing modes: safest (prioritizes safety) and fastest (prioritizes speed)
- Time-based risk assessment with adjustments based on time of day
- Identification of high-risk segments within routes
- Balanced cost function considering both distance and safety

### 4.3 Directions API Client (`app/src/main/java/com/universityofreading/demo/navigation/DirectionsClient.kt`)

The `DirectionsClient` provides an interface to Google's Directions API:

```kotlin
object DirectionsClient {
    // Get route alternatives with different optimization strategies
    suspend fun getAlternatives(
        origin: LatLng,
        dest: LatLng,
        isSafestMode: Boolean = false
    ) { ... }
    
    // Get routes with a specified travel mode
    suspend fun getRoutes(
        origin: LatLng,
        dest: LatLng,
        mode: String = "walking",
        alternatives: Boolean = true
    ) { ... }
}
```

Features:
- Retrieves multiple route alternatives from Google's Directions API
- Supports different travel modes (walking, cycling, driving)
- Fallback mechanisms for API failures
- Caching and error handling

### 4.4 Main Map Screen (`app/src/main/java/com/universityofreading/demo/ProgressiveMapScreen.kt`)

The main interface for interacting with the map and routing features:

```kotlin
@Composable
fun ProgressiveMapScreen() {
    // State variables for UI
    var isDrawModeActive by remember { mutableStateOf(false) }
    var drawnPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    
    // Create crime spatial index for route risk calculation
    val crimeSpatialIndex = remember(crimeData) {
        if (crimeData.isNotEmpty()) {
            CrimeSpatialIndex(crimeData)
        } else null
    }
    
    // Map setup and interaction handlers
    // ...
}
```

Key features:
- Interactive map with crime data visualization (heatmap and markers)
- Route planning with toggleable safety/speed priorities
- Area drawing and analysis tool
- Time simulation for risk assessment at different times of day
- Various UI components for displaying risk information

### 4.5 Area Drawing and Analysis (`app/src/main/java/com/universityofreading/demo/ui/theme/CustomComponents.kt`)

The application allows users to draw custom areas on the map and analyze crime data within those areas:

```kotlin
// Draw mode button
@Composable
fun DrawModeButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = if (isActive) Color(0xFF9C27B0) else Color.White,
        contentColor = if (isActive) Color.White else Color(0xFF9C27B0)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_pencil),
            contentDescription = "Draw Area"
        )
    }
}

// Area analysis dialog
@Composable
fun CustomAreaAnalysisDialog(
    analysis: AreaAnalysis,
    onDismiss: () -> Unit
) { ... }
```

Features:
- Interactive polygon drawing on the map
- Crime statistics calculation for the defined area
- Visual breakdown of crime types and severity
- Risk percentage assessment based on crime density and severity

### 4.6 Time Simulation (`app/src/main/java/com/universityofreading/demo/ui/theme/TimeSimulationSlider.kt`)

The application allows users to simulate different times of day to assess risk variations:

```kotlin
// In ProgressiveMapScreen.kt
// Time-based risk state
var currentTimePeriod by remember { mutableStateOf(SafeRoutePlanner.getCurrentTimePeriod()) }
var currentRiskLevel by remember { mutableStateOf(SafeRoutePlanner.getCurrentTimeRiskLevel()) }

// Time simulation slider UI component
@Composable
fun TimeSimulationSlider(
    simulatedHour: Int,
    onHourChange: (Int) -> Unit,
    isUsingCurrentTime: Boolean,
    onUseCurrentTime: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) { ... }
```

Features:
- Adjustable time slider for simulating different hours of the day
- Visual risk indicators that change based on time
- Route recalculation based on time-adjusted risk profiles

### 4.7 Route Options Bottom Sheet (`app/src/main/java/com/universityofreading/demo/ui/theme/SafeRouteBottomSheet.kt`)

The application presents route options in a bottom sheet:

```kotlin
@Composable
fun SafeRouteBottomSheet(
    distanceKm: Double,
    riskScore: Double,
    highRiskSegments: Int = 0,
    isSafestMode: Boolean,
    onSafestModeChanged: (Boolean) -> Unit,
    onStartNavigationClick: () -> Unit,
    onDismissRequest: () -> Unit = {}
) { ... }
```

Features:
- Toggle between safest and fastest routing modes
- Distance, time, and risk level display
- High-risk segment warnings
- Start navigation button

## 5. Navigation and Screens (`app/src/main/java/com/universityofreading/demo/NavGraph.kt`)

The application uses the Navigation Component for screen navigation:

```kotlin
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MENU_SCREEN
    ) {
        // Menu/Home screen
        composable(Routes.MENU_SCREEN) {
            MenuScreen(navController)
        }

        // Map screen
        composable(Routes.MAP_SCREEN) {
            ProgressiveMapScreen()
        }

        // Crime Statistics screen
        composable(Routes.CRIME_STATS_SCREEN) {
            CrimeStatsScreen()
        }

        // Crime Compare screen
        composable(Routes.CRIME_COMPARE_SCREEN) {
            CrimeCompareScreen()
        }
    }
}
```

Main screens:
1. **Menu Screen**: App entry point with navigation options
2. **Map Screen**: Main interactive map with routing and area analysis
3. **Crime Statistics Screen**: Statistical view of crime data by region
4. **Crime Compare Screen**: Side-by-side comparison of crime statistics in different regions

## 6. Technical Implementation Details

### 6.1 Crime Data Visualization (`app/src/main/java/com/universityofreading/demo/ProgressiveMapScreen.kt`)

The application uses two main approaches to visualize crime data:

1. **Heatmap**: For lower zoom levels, showing crime density
   ```kotlin
   private fun addHeatMap(map: GoogleMap, crimeData: List<CrimeData>) {
       // Create weighted data points for the heatmap
       val weighted = crimeData.map {
           WeightedLatLng(LatLng(it.latitude, it.longitude), it.severity + 1)
       }
       
       // Create a gradient from green (low) to yellow to red (high)
       val colors = intArrayOf(
           android.graphics.Color.rgb(81, 255, 0),   // green
           android.graphics.Color.rgb(255, 165, 0),  // orange
           android.graphics.Color.rgb(255, 0, 0)     // red
       )
       
       val provider = HeatmapTileProvider.Builder()
           .weightedData(weighted)
           .gradient(Gradient(colors, floatArrayOf(0f, 0.5f, 1f)))
           .radius(50)
           .build()
           
       map.addTileOverlay(TileOverlayOptions().tileProvider(provider))
   }
   ```

2. **Individual Markers**: For higher zoom levels, showing specific crime locations with emoji icons
   ```kotlin
   private fun addMarkers(context: Context, map: GoogleMap, crimeData: List<CrimeData>) {
       // Cache for marker icons
       val iconCache = mutableMapOf<String, BitmapDescriptor>()
       
       // Add each crime as a marker with emoji representation
       for (crime in crimeData) {
           // Get emoji for crime type
           val emoji = crime.getEmoji()
           
           // Get or create marker icon
           val markerIcon = iconCache.getOrPut(emoji) {
               createBitmapFromText(context, emoji)
           }
           
           map.addMarker(
               MarkerOptions()
                   .position(LatLng(crime.latitude, crime.longitude))
                   .title(crime.type)
                   .snippet("Date: ${crime.date}")
                   .icon(markerIcon)
           )
       }
   }
   ```

### 6.2 Route Visualization (`app/src/main/java/com/universityofreading/demo/ProgressiveMapScreen.kt`)

Routes are displayed with different styles based on their safety:

```kotlin
// Draw the main route polyline
val routeColor = if (isSafestMode) 
    android.graphics.Color.rgb(0, 160, 0) // Darker green for safest
else 
    android.graphics.Color.rgb(0, 0, 220) // Blue for fastest

// Highlight high-risk segments
if (isSafestMode) {
    // For safest route: Find and highlight high-risk segments
    highRiskPointPairs.forEach { (start, end) ->
        val highRiskSegment = map.addPolyline(
            PolylineOptions()
                .add(start, end)
                .color(android.graphics.Color.RED)
                .width(12f)
                .pattern(listOf(Gap(10f), Dot(), Gap(10f)))
                .zIndex(15f) // Higher than the main route
        )
    }
}
```

## 7. Complete Feature List

### Core Navigation Features
1. **Dual Routing Modes** (`SafeRoutePlanner.kt`)
   - Safest route prioritization (emphasizes safety over speed)
   - Fastest route prioritization (emphasizes speed over safety)
   
2. **Interactive Route Planning** (`ProgressiveMapScreen.kt`)
   - Start and destination marker placement
   - Draggable markers for route adjustment
   - Route recalculation when markers are moved

3. **Route Visualization** (`ProgressiveMapScreen.kt`)
   - Color-coded routes (green for safe routes, blue for fast routes)
   - Highlighted high-risk segments in red with dashed patterns
   - Alternative routes displayed as gray dashed lines

4. **Route Information** (`SafeRouteBottomSheet.kt`)
   - Distance display in kilometers
   - Estimated journey time calculation
   - Risk score assessment (0-100 scale)
   - High-risk segment count

### Crime Data Visualization

5. **Dual Visualization Modes** (`ProgressiveMapScreen.kt`)
   - Heatmap for low zoom levels showing crime density
   - Individual markers for high zoom levels showing specific crimes

6. **Crime Marker Customization** (`ProgressiveMapScreen.kt`)
   - Emoji-based markers specific to crime types
   - Custom info windows with crime details
   - Automatic marker clustering for dense areas

7. **Crime Filtering** (`ProgressiveMapScreen.kt`)
   - Date-based filtering (most recent month, last 3 months, all data)
   - Automatic filtering by visible map region

### Area Analysis Tools

8. **Custom Area Drawing** (`ProgressiveMapScreen.kt`, `CustomComponents.kt`)
   - Interactive polygon drawing mode with purple indicator
   - "Done" button to complete drawing
   - Automatic polygon completion when near starting point

9. **Area Crime Analysis** (`CrimeSpatialIndex.kt`, `CustomAreaAnalysisDialog.kt`)
   - Total crime count in the selected area
   - Average crime severity calculation
   - High-risk crime count
   - Overall area risk percentage (0-100 scale)
   - Crime type breakdown with counts

### Time-Based Features

10. **Time Simulation** (`TimeSimulationSlider.kt`, `SafeRoutePlanner.kt`)
    - Adjustable time slider for different hours
    - Current time mode with real-time updates
    - Visual time period indicator
    - Time-based risk level adjustment

11. **Dynamic Risk Assessment** (`CrimeSpatialIndex.kt`, `SafeRoutePlanner.kt`)
    - Time-adjusted risk calculation (higher at night)
    - Real-time route recalculation when time changes
    - Visual indicators of current time period risk level

### Statistics and Comparison Features

12. **Crime Statistics Screen** (`CrimeStatsScreen.kt`)
    - Region-specific crime statistics
    - Crime type distribution charts
    - Temporal trend analysis

13. **Region Comparison** (`CrimeCompareScreen.kt`)
    - Side-by-side comparison of different regions
    - Crime rate differential analysis
    - Safety ranking between regions

### User Interface Features

14. **Bottom Sheet Controls** (`SafeRouteBottomSheet.kt`)
    - Route details summary
    - Safety/speed preference toggle
    - Navigation start button
    - Risk level indicator with color coding

15. **Map Controls** (`ProgressiveMapScreen.kt`)
    - Zoom-level dependent visualization
    - Custom control positioning
    - Drawing tools in bottom left corner
    - Time controls in top right corner

16. **Information Displays** (`ProgressiveMapScreen.kt`)
    - Toast notifications for user instructions
    - Error messages with retry options
    - Debug information display
    - Loading indicators with progress feedback

### Technical Features

17. **Spatial Analysis** (`CrimeSpatialIndex.kt`)
    - R-tree implementation for efficient spatial queries
    - Polygon area calculation
    - Point-in-polygon testing
    - Distance-weighted risk calculation

18. **API Integration** (`DirectionsClient.kt`)
    - Google Maps API for mapping
    - Google Directions API for routing
    - Multiple travel mode support (walking, cycling, driving)
    - Fallback mechanisms for API failures

19. **Performance Optimizations** (`ProgressiveMapScreen.kt`)
    - Icon caching for marker rendering
    - Selective marker display based on zoom level
    - Coroutine-based asynchronous processing
    - Cached spatial index for quick queries

20. **Data Management** (`CrimeDataRepository.kt`)
    - Crime data repository with caching
    - Date-based filtering and grouping
    - Crime severity normalization
    - Type-based weighting system

## 8. Summary

The Safe Route Planning application is a sophisticated mobile solution that combines crime data analysis, spatial indexing, and routing algorithms to enhance user safety during urban navigation. Its key technical strengths include:

1. **Advanced Spatial Analysis**: Efficient R-tree implementation for quick spatial queries
2. **Sophisticated Risk Calculation**: Multi-factor risk assessment considering crime severity, distance, recency, and type
3. **Smart Routing**: Dual-mode routing strategy balancing safety and efficiency
4. **Interactive UI**: Modern Jetpack Compose interface with intuitive controls
5. **Visual Data Representation**: Effective visualization of crime data through heatmaps and marked locations
6. **Area Analysis**: Custom area drawing and statistical analysis
7. **Time Simulation**: Risk assessment adjustments based on time of day

The application demonstrates effective integration of Google Maps, spatial indexing, and modern Android architecture components to create a practical safety-focused navigation solution. 