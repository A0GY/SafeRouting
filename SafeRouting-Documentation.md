# Safe Routing System Documentation

## Feature List

### Main Menu Features
- **Intuitive Navigation Hub**: Central access point to all app features
- **Interactive UI**: Attractive gradient background with large, easy-to-tap buttons
- **Safe Travel Branding**: Clear application title and purpose indication
- **Direct Access Buttons**: One-tap navigation to map, statistics, and comparison tools
- **Visual Hierarchy**: Clean layout emphasizing primary features

### Home Screen Features
- **Map Interface**: Interactive Google Maps integration
- **Current Location Detection**: Automatic user location identification
- **Search Functionality**: Address and landmark search capabilities
- **Crime Heatmap Visualization**: Visual representation of crime risk areas
- **Time Simulation Controls**: Adjust time of day to see risk variations
- **Risk Level Indicator**: Dynamic display of current risk level based on time
- **Date Filtering**: Filter crime data by recency (7 days, 30 days, all)
- **Zoom-Based Detail**: Automatic transition between heatmap and individual markers based on zoom level
- **Marker Clustering**: Efficient display of multiple crime points in close proximity

### Navigation Features
- **Dual Routing Modes**: Toggle between "Safest" and "Fastest" route options
- **Route Visualization**: Color-coded route paths (green for safe, blue for fast)
- **Alternative Routes Display**: Secondary route options shown with dashed lines
- **Route Details Panel**: Bottom sheet with distance, time, and risk information
- **High-Risk Segment Warnings**: Alerts for routes passing through dangerous areas
- **Navigation Start Button**: Begin turn-by-turn navigation with selected route
- **Interactive Route Selection**: Tap to place start and destination markers
- **Route Mode Description**: Detailed explanation of route optimization strategy

### Crime Analysis Features
- **Specific Crime Analysis**: Detailed examination of individual crime types and statistics
- **Comparative Analysis**: Side-by-side comparison of crime patterns across different areas
- **Region Selection**: Dropdown menu for choosing areas to analyze
- **Crime Time Distribution**: Analysis of crime occurrence by time of day
- **Crime Type Breakdown**: Statistical distribution of crime categories in selected areas
- **Trend Visualization**: Charts and graphs showing crime trends over time
- **Crime Filtering**: Filter crime data by type, severity, and time period
- **Area-Based Statistics**: Crime density and frequency metrics for selected regions
- **Export Functionality**: Export analysis data for further processing
- **Pagination Controls**: Navigate through multiple analysis screens with next/previous buttons
- **Severity Distribution**: Breakdown of crimes by severity levels (high/medium/low)

### Risk Assessment Features
- **Dynamic Risk Calculation**: Real-time risk assessment based on location
- **Time-Sensitive Risk**: Risk levels that change with time of day
- **Crime Type Weighting**: Different risk weights for various crime categories
- **Crime Recency Factors**: More recent crimes have greater impact on risk
- **Spatial Risk Distribution**: Inverse square law applied to crime proximity
- **Risk Normalization**: Scaling to ensure consistent and comparable risk scores
- **Risk Explanation**: User-friendly descriptions of current risk factors

### Data Visualization Features
- **Crime Heatmap**: Color-gradient visualization of crime density
- **Risk-Coded Routes**: Routes colored according to safety level
- **Time Period Indicators**: Visual cues for current time period (Day/Night)
- **High-Risk Area Highlighting**: Emphasis on areas with elevated risk
- **Risk Level Legend**: Color key for interpreting heatmap intensity
- **Statistical Charts**: Pie and bar charts for crime distribution visualization
- **Temporal Heat Maps**: Visualization of crime patterns changing over time
- **Line Charts**: Trend visualization for crime severity over time
- **Bar Comparison Charts**: Side-by-side comparison of statistics between regions

### User Experience Features
- **Intuitive Mode Switching**: Simple toggle between safety and speed priorities
- **Responsive UI**: Adapts to different screen sizes and orientations
- **Bottom Sheet Interactions**: Expandable panels for additional information
- **Smooth Animations**: Transitions between states and route changes
- **Error Handling**: Fallbacks for network issues or missing data
- **Tab Navigation**: Easy switching between map, analysis, and comparison views
- **User Preferences**: Customizable settings for display and analysis preferences
- **Progressive Loading**: Graceful handling of data loading with progress indicators
- **Informative Tooltips**: Contextual information on hover/tap for data points
- **Accessibility Features**: Support for screen readers and alternative input methods

## Overview

The Safe Routing System is designed to provide route navigation options that consider both efficiency (time and distance) and safety (crime risk levels). The system integrates with Google Maps Directions API to fetch route geometries, processes them through a crime risk assessment engine, and presents users with optimized routes based on their preferences.

## Core Components

### 1. **SafeRoutePlanner** (`app/src/main/java/com/universityofreading/demo/navigation/SafeRoutePlanner.kt`)

The central orchestration component that manages route planning, risk calculation, and route selection.

**Key Features:**
- Mode-based routing (safest vs. fastest)
- Time-sensitive risk assessment
- Adaptive route sampling
- Position-weighted risk calculation
- High-risk segment detection

**Configuration Parameters:**
```kotlin
private const val SAMPLE_DISTANCE_METERS = 25.0      // Sample points every 25m for accuracy
private const val RISK_RADIUS_METERS = 150.0         // Search radius for crime data
private const val MAX_NORMALIZED_RISK = 100.0        // Maximum risk value (matching CrimeSpatialIndex scale)
private const val RISK_SIGMOID_FACTOR = 0.03         // Controls sigmoid curve steepness
private const val MIN_SCALING_FACTOR = 0.3           // Minimum efficiency factor
private const val HIGH_RISK_PENALTY = 0.4            // Penalty for routes with high-risk segments
private const val FASTEST_SAFETY_WEIGHT = 0.05       // Still consider minimal safety in fastest mode
private const val SAFEST_SAFETY_WEIGHT = 0.95        // Prioritize safety in safest mode
const val HIGH_RISK_THRESHOLD = 0.5                  // Threshold for considering a segment high risk (0-1 scale)
```

**Core Methods:**
- `safestRoute()`: Primary entry point that returns the best route and alternatives
- `calculateRouteRiskAndSegments()`: Calculates risk score and counts high-risk segments
- `calculateRouteCost()`: Determines route cost based on safety and efficiency
- `getTimeBasedRiskMultiplier()`: Adjusts risk based on time of day

### 2. **DirectionsClient** (`app/src/main/java/com/universityofreading/demo/navigation/DirectionsClient.kt`)

Provides an interface to the Google Maps Directions API for retrieving route geometries.

**Key Features:**
- Multiple travel mode support (walking, cycling, driving)
- Alternative routes retrieval
- Route diversity maximization
- Fallback mechanisms for API failures

**Core Methods:**
- `getAlternatives()`: Gets diverse route alternatives using multiple travel modes
- `getRoutes()`: Gets routes for a specific travel mode
- `createStraightLineRoute()`: Creates a fallback route when API fails

**API Handling:**
```kotlin
suspend fun getAlternatives(
    origin: LatLng,
    dest: LatLng,
    isSafestMode: Boolean = false
) = withContext(Dispatchers.IO) {
    // Always request walking routes
    val walkingResult = DirectionsApi.newRequest(ctx)
        .origin("${origin.latitude},${origin.longitude}")
        .destination("${dest.latitude},${dest.longitude}")
        .mode(TravelMode.WALKING)
        .alternatives(true)
        .await()
    
    // For safest mode: also request cycling routes for more options
    // For fastest mode: request driving routes
    // ...
}
```

### 3. **CrimeSpatialIndex** (`app/src/main/java/com/universityofreading/demo/navigation/CrimeSpatialIndex.kt`)

Efficient spatial index for querying crime data and calculating risk at specific locations.

**Key Features:**
- R-tree spatial indexing for fast geographic queries
- Crime severity weighting
- Crime type categorization
- Distance-based risk falloff (inverse square law)
- Recency weighting (newer crimes have more impact)
- Time of day risk adjustment

**Risk Calculation Parameters:**
```kotlin
private val MAX_CRIME_AGE_DAYS = 365 * 2 // 2 years
private val CRIME_RECENCY_FACTOR = 0.4   // REDUCED for less emphasis on recency
private val SEVERITY_MULTIPLIER = 1.2    // For high-severity crimes
private val DISTANCE_FALLOFF_FACTOR = 2.0 // Squared distance falloff (inverse square law)
private val RISK_RADIUS_METERS = 250.0    // Search radius for crimes
private val MIN_RISK_POINTS = 3           // Minimum crime points to consider an area risky
private val MIN_DISTANCE_METERS = 5.0
```

**Crime Type Weights:**
```kotlin
private val crimeTypeWeights = mapOf(
    "violent-crime" to 1.5,            // Highest weights for violent crimes
    "robbery" to 1.4,
    "violence-and-sexual-offences" to 1.5,
    // ... other crime types
)
```

**Core Methods:**
- `riskAt()`: Calculates risk score at a specific location
- `getTimeOfDayMultiplier()`: Static method that returns risk multiplier based on hour

### 4. **RouteModels** (`app/src/main/java/com/universityofreading/demo/navigation/RouteModels.kt`)

Data structures for representing routes and their associated metrics.

**Key Types:**
- `RouteCandidate`: Represents a potential route with risk metrics
```kotlin
data class RouteCandidate(
    val route: DirectionsRoute,
    val riskScore: Double,
    val distanceM: Double,
    val durationS: Double = 0.0,
    val highRiskSegments: Int = 0
)
```

### 5. **CrimeData** (`app/src/main/java/com/universityofreading/demo/data/CrimeData.kt`)

Data structure for crime information.

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

## Routing Process Workflow

### 1. **Route Retrieval**

The process begins in `SafeRoutePlanner.safestRoute()` which:
1. Gets the current time period and risk multiplier
2. Calls `DirectionsClient.getAlternatives()` to fetch route options
3. Different travel modes are requested based on the routing preference:
   - Safest mode: Walking + Cycling (prioritizing diversity)
   - Fastest mode: Walking + Driving (prioritizing speed)

### 2. **Mode-Based Optimization**

Depending on the selected mode, the system takes different approaches:

**Fastest Mode:**
```kotlin
// SHORT-CIRCUIT FOR FASTEST MODE: Optimize purely for duration
if (!isSafestMode) {
    // Calculate basic risk for UI display purposes, but optimize for speed
    val routeCandidates = routes.map { route ->
        // ...
    }
    
    // Find fastest route by pure duration
    val fastestRoute = routeCandidates.minByOrNull { it.durationS } ?: routeCandidates.first()
    // Sort remaining routes by duration for alternatives
    val alternatives = routeCandidates
        .filterNot { it == fastestRoute }
        .sortedBy { it.durationS }
        .take(2)
    
    return@withContext fastestRoute to alternatives
}
```

**Safest Mode:**
- Calculate risk scores for all routes
- Normalize metrics (risk, distance, duration) to 0-1 scale
- Apply high-risk segment penalties
- Calculate weighted cost using safety and efficiency components
- Sort routes by cost and select the best

### 3. **Risk Calculation**

For each route, risk is calculated in `calculateRouteRiskAndSegments()`:

1. Decode the route polyline to get the path geometry
2. Determine sampling parameters based on route length
3. Sample points along the route at regular intervals
4. For each sample point:
   - Query crime risk using `CrimeSpatialIndex.riskAt()`
   - Apply position weighting (bell curve - middle points matter more)
   - Track high-risk segments
   - Calculate segment contribution to overall risk
5. Normalize the risk integral by route length
6. Return the final risk score and high-risk segment count

### 4. **Cost Function**

The route cost function in `calculateRouteCost()` balances safety and efficiency:

```kotlin
private fun calculateRouteCost(
    normalizedRisk: Double,
    normalizedDistance: Double, 
    normalizedDuration: Double,
    safetyWeight: Double,
    distanceScalingFactor: Double
): Double {
    // Safety component using logistic function for better mid-range sensitivity
    val safetyComponent = if (safetyWeight > 0.9) {
        safetyWeight / (1.0 + exp(-8 * (normalizedRisk - 0.5)))
    } else {
        safetyWeight * normalizedRisk
    }
    
    // Efficiency component with time/distance balancing
    val timeWeight = if (safetyWeight > 0.9) {
        0.3  // In safest mode: 30% time-focused
    } else {
        0.9  // In fastest mode: 90% time-focused
    }
    
    val distanceWeight = 1.0 - timeWeight
    
    // Raw efficiency calculation
    val efficiencyRaw = timeWeight * normalizedDuration + 
                       distanceWeight * normalizedDistance
    
    // Apply scaling and weighting
    val efficiencyComponent = (1.0 - safetyWeight) * efficiencyRaw * distanceScalingFactor
    
    return safetyComponent + efficiencyComponent
}
```

### 5. **Time Sensitivity**

Both `SafeRoutePlanner` and `CrimeSpatialIndex` consider time of day:

```kotlin
// From SafeRoutePlanner.kt
private fun getTimeBasedRiskMultiplier(): Double {
    // Use simulated hour if set, otherwise use current hour
    val currentHour = simulatedHour ?: org.threeten.bp.LocalTime.now().hour
    return when(currentHour) {
        in 0..5 -> 1.8    // Very late night/early morning (highest risk)
        in 6..8 -> 1.0    // Morning commute (baseline risk)
        in 9..16 -> 0.7   // Daytime (lowest risk)
        in 17..19 -> 1.1  // Evening commute (slightly elevated)
        in 20..23 -> 1.5  // Evening/night (high risk)
        else -> 1.0       // Fallback
    }
}
```

## UI Integration

The routing system integrates with the UI through:

- **MapScreen** - Displays the routes with appropriate styling
- **SafeRouteBottomSheet** - Shows route details and allows mode toggling
- **ProgressiveMapScreen** - Manages the overall map state and user interactions

Routes are color-coded on the map:
```kotlin
// In MapScreen.kt
val routeColor = if (isSafestMode) 
    Color.rgb(0, 170, 0) // Green for safest
else 
    Color.rgb(0, 0, 220) // Blue for fastest
```

## Limitations and Constraints

1. **Google Directions API Limitations**
   - Limited number of alternative routes (typically 2-3)
   - Similar route geometries regardless of travel mode for short distances
   - No direct "safety" parameter for the API

2. **Crime Data Dependency**
   - System effectiveness depends on crime data density
   - Areas with few crime reports show minimal risk variation
   - Recency of crime data affects risk assessment accuracy

3. **Optimization Tradeoffs**
   - Complete safety optimization may lead to excessively long routes
   - Balancing safety vs. efficiency is subjective and context-dependent

## Future Enhancement Opportunities

1. **Route Diversity Enhancement**
   - Add strategic waypoints to force different route geometries
   - Implement minimum route difference requirements
   - Create custom route generator for small areas

2. **Advanced Risk Models**
   - Incorporate machine learning for risk prediction
   - Add temporal patterns (day of week, seasonal variations)
   - Consider demographic and environmental factors

3. **Performance Optimization**
   - Cache pre-calculated risk for common road segments
   - Implement local route database for faster responses
   - Add parallel processing for risk calculation

4. **User Experience**
   - Provide more granular control over safety/efficiency balance
   - Add visualization of risk levels along routes
   - Support route comparison with detailed safety metrics 