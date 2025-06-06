package com.universityofreading.demo.navigation

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object SafeRoutePlanner {
    private const val TAG = "SafeRoutePlanner"
    
    // Configurable parameters - REVISED FOR IMPROVED RISK ASSESSMENT
    private const val SAMPLE_DISTANCE_METERS = 25.0      // Sample points every 25m for accuracy
    private const val RISK_RADIUS_METERS = 250.0         // INCREASED to 250m to capture more crime data, especially for short routes
    private const val MAX_NORMALIZED_RISK = 100.0        // Maximum risk value (matching CrimeSpatialIndex scale)
    private const val RISK_SIGMOID_FACTOR = 0.03         // Controls sigmoid curve steepness
    private const val MIN_SCALING_FACTOR = 0.3           // Minimum efficiency factor
    private const val HIGH_RISK_PENALTY = 0.4            // Penalty for routes with high-risk segments
    private const val RISK_AMPLIFICATION_FACTOR = 1.5    // Amplifies risk scores to make differences more noticeable
    
    // REVISED PARAMETERS for clearer distinction between modes
    private const val FASTEST_SAFETY_WEIGHT = 0.05       // Still consider minimal safety in fastest mode
    private const val SAFEST_SAFETY_WEIGHT = 0.95        // Prioritize safety in safest mode
    const val HIGH_RISK_THRESHOLD = 0.35                 // LOWERED threshold for considering a segment high risk (0-1 scale)
    
    // Store the current simulated hour (or null for real time)
    private var simulatedHour: Int? = null
    
    // Store a reference to the crime spatial index
    private var crimeSpatialIndex: CrimeSpatialIndex? = null
    
    // Set the crime spatial index
    fun setCrimeSpatialIndex(index: CrimeSpatialIndex) {
        crimeSpatialIndex = index
        DebugLogger.logDebug(TAG, "Set crime spatial index with ${index.getCrimeCount()} crime points")
    }
    
    // Set a simulated hour for risk calculation
    fun setSimulatedHour(hour: Int?) {
        simulatedHour = hour
        DebugLogger.logDebug(TAG, "Set simulated hour to: ${hour ?: "CURRENT_TIME"}")
    }
    
    // Time of day factors (more risk at night - now using actual time of day)
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
    
    // Get time period string for UI display
    fun getCurrentTimePeriod(): String {
        // Use simulated hour if set, otherwise use current hour
        val currentHour = simulatedHour ?: org.threeten.bp.LocalTime.now().hour
        return when(currentHour) {
            in 0..5 -> "Night"
            in 6..8 -> "Morning"
            in 9..11 -> "Mid-morning"
            in 12..16 -> "Afternoon"
            in 17..19 -> "Evening"
            in 20..23 -> "Night"
            else -> "Day"
        }
    }
    
    // Get risk level based on time for UI display
    fun getCurrentTimeRiskLevel(): String {
        val multiplier = getTimeBasedRiskMultiplier()
        return when {
            multiplier >= 1.5 -> "High"
            multiplier >= 1.0 -> "Medium"
            else -> "Low"
        }
    }

    /**
     * Calculate the safest or fastest route depending on preference.
     * Uses a binary toggle approach (isSafestMode) instead of a continuous weight.
     *
     * @param origin Starting point
     * @param dest Destination point
     * @param isSafestMode If true, prioritize safety over speed; if false, prioritize speed
     * @param index Spatial index for risk calculation
     * @return Pair of best route and alternative routes
     */
    suspend fun safestRoute(
        origin: LatLng,
        dest: LatLng,
        isSafestMode: Boolean,
        index: CrimeSpatialIndex
    ): Pair<RouteCandidate, List<RouteCandidate>> =
        withContext(Dispatchers.Default) {
            try {
                // Get current time period for logging
                val currentTimePeriod = getCurrentTimePeriod()
                val timeMultiplier = getTimeBasedRiskMultiplier()
                
                DebugLogger.logDebug(TAG, "Requesting routes from DirectionsClient (mode: ${if (isSafestMode) "Safest" else "Fastest"}, time period: $currentTimePeriod, multiplier: $timeMultiplier)")
                val routes = DirectionsClient.getAlternatives(origin, dest, isSafestMode)
                DebugLogger.logDebug(TAG, "Received ${routes.size} routes from DirectionsClient")
                
                if (routes.isEmpty()) {
                    DebugLogger.logDebug(TAG, "No routes returned from DirectionsClient")
                    return@withContext RouteCandidate(
                        StraightLineRouteCreator.createStraightLine(origin, dest),
                        1.0,
                        calculateHaversineDistance(origin, dest)
                    ) to emptyList()
                }
                
                // SHORT-CIRCUIT FOR FASTEST MODE: Optimize purely for duration
                if (!isSafestMode) {
                    // Calculate basic risk for UI display purposes, but optimize for speed
                    val routeCandidates = routes.map { route ->
                        val distanceM = route.legs[0].distance.inMeters.toDouble()
                        
                        // Use the actual duration from API for walking routes
                        val durationS = route.legs[0].duration.inSeconds.toDouble()
                        
                        // Still calculate risk (for visualization), but don't use it for ranking
                        val (risk, highRiskSegments) = calculateRouteRiskAndSegments(route, index, timeMultiplier)
                        
                        RouteCandidate(route, risk, distanceM, durationS, highRiskSegments)
                    }
                    
                    // Find fastest route by pure duration, ensuring we only consider walking-appropriate routes
                    val fastestRoute = routeCandidates.minByOrNull { it.durationS } ?: routeCandidates.first()
                    
                    // Sort remaining routes by duration for alternatives
                    val alternatives = routeCandidates
                        .filterNot { it == fastestRoute }
                        .sortedBy { it.durationS }
                        .take(2)
                    
                    DebugLogger.logDebug(TAG, "FASTEST mode: Selected route with duration: ${fastestRoute.durationS}s, distance: ${fastestRoute.distanceM}m")
                    return@withContext fastestRoute to alternatives
                }
                
                // SAFEST MODE: Continue with full risk calculation and ranking
                // Extract route distances and durations
                val routeDistances = routes.map { it.legs[0].distance.inMeters.toDouble() }
                val maxDist = routeDistances.maxOrNull() ?: 1.0
                val minDist = routeDistances.minOrNull() ?: 1.0
                
                val routeDurations = routes.map { it.legs[0].duration.inSeconds.toDouble() }
                val maxDuration = routeDurations.maxOrNull() ?: 1.0
                val minDuration = routeDurations.minOrNull() ?: 1.0
                
                DebugLogger.logDebug(TAG, "Route distances range: $minDist to $maxDist meters")
                DebugLogger.logDebug(TAG, "Route durations range: $minDuration to $maxDuration seconds")
                
                // FIXED: Improved distance scaling factor calculation
                val distanceRange = maxDist - minDist
                // Always maintain a reasonable scaling factor even when routes are similar
                val distanceScalingFactor = max(MIN_SCALING_FACTOR, min(1.0, distanceRange / maxDist))
                
                DebugLogger.logDebug(TAG, "Distance scaling factor: $distanceScalingFactor")

                // Calculate risk scores for all routes
                DebugLogger.logDebug(TAG, "Calculating risk scores for all routes")
                val routeCandidates = routes.mapIndexed { i, route ->
                    try {
                        val (risk, highRiskSegments) = calculateRouteRiskAndSegments(route, index, timeMultiplier)
                        val distanceM = route.legs[0].distance.inMeters.toDouble()
                        val durationS = route.legs[0].duration.inSeconds.toDouble()
                        
                        DebugLogger.logDebug(TAG, "Route $i: distance=${distanceM}m, duration=${durationS}s, risk=$risk, highRiskSegments=$highRiskSegments")
                        
                        RouteCandidate(
                            route, 
                            risk, 
                            distanceM,
                            durationS,
                            highRiskSegments
                        )
                    } catch (e: Exception) {
                        // Handle errors for individual routes
                        DebugLogger.logError(TAG, "Error calculating risk for route $i", e)
                        
                        // Create a fallback candidate with default risk score
                        val fallbackDistanceM = route.legs[0].distance.inMeters.toDouble()
                        val fallbackDurationS = route.legs[0].duration.inSeconds.toDouble() 
                        RouteCandidate(
                            route,
                            50.0, // Default medium risk
                            fallbackDistanceM,
                            fallbackDurationS
                        )
                    }
                }
                
                // Get the min and max risk values for this batch of routes
                val minRisk = routeCandidates.minOfOrNull { it.riskScore } ?: 0.0
                val maxRisk = routeCandidates.maxOfOrNull { it.riskScore } ?: MAX_NORMALIZED_RISK
                
                // Calculate costs for all routes (safest mode)
                val rankedRoutes = routeCandidates.map { candidate ->
                    // Normalize metrics to 0-1 range
                    val normalizedDistance = (candidate.distanceM - minDist) / max(1.0, maxDist - minDist)
                    val normalizedDuration = (candidate.durationS - minDuration) / max(1.0, maxDuration - minDuration)
                    
                    // Re-scale risk relative to this batch (per-search normalization)
                    val relativeRisk = (candidate.riskScore - minRisk) / (maxRisk - minRisk + 1e-3)
                    
                    // Calculate the high-risk segment penalty
                    val highRiskPenalty = if (candidate.highRiskSegments > 0) {
                        val routeSegments = candidate.route.legs[0].steps.size
                        val segmentRatio = if (routeSegments > 0) {
                            candidate.highRiskSegments.toDouble() / routeSegments
                        } else {
                            0.0
                        }
                        HIGH_RISK_PENALTY * segmentRatio
                    } else {
                        0.0
                    }
                    
                    // Calculate cost with safety vs. efficiency tradeoff
                    val cost = calculateRouteCost(
                        relativeRisk,
                        normalizedDistance,
                        normalizedDuration,
                        SAFEST_SAFETY_WEIGHT, // Always use safest weight here (we short-circuit fastest earlier)
                        distanceScalingFactor
                    ) + highRiskPenalty
                    
                    candidate to cost
                }.sortedBy { it.second }
                
                // Get top routes - limit to prevent excessive processing
                val bestRoute = rankedRoutes.first().first
                val alternativeRoutes = rankedRoutes.drop(1).take(2).map { it.first }  // Limit to 2 alternatives
                
                DebugLogger.logDebug(
                    TAG, 
                    "SAFEST mode: Selected route with risk: ${bestRoute.riskScore}, " +
                    "distance: ${bestRoute.distanceM}m, duration: ${bestRoute.durationS}s, " +
                    "highRiskSegments: ${bestRoute.highRiskSegments}"
                )

                bestRoute to alternativeRoutes
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error calculating route", e)
                // Return a default route rather than throwing
                val straightLine = StraightLineRouteCreator.createStraightLine(origin, dest) 
                val fallbackRoute = RouteCandidate(
                    straightLine,
                    50.0, // Medium risk
                    calculateHaversineDistance(origin, dest)
                )
                fallbackRoute to emptyList()
            }
        }
    
    /**
     * Helper method to calculate the risk score for a route
     * Extracted to avoid code duplication
     * 
     * @return The calculated risk score
     */
    private fun calculateRouteRisk(
        route: com.google.maps.model.DirectionsRoute,
        index: CrimeSpatialIndex,
        timeMultiplier: Double
    ): Double {
        val (risk, _) = calculateRouteRiskAndSegments(route, index, timeMultiplier)
        return risk
    }
    
    /**
     * Helper method to calculate the risk score and count high-risk segments for a route
     * 
     * @return Pair of (riskScore, highRiskSegmentCount)
     */
    private fun calculateRouteRiskAndSegments(
        route: com.google.maps.model.DirectionsRoute,
        index: CrimeSpatialIndex,
        timeMultiplier: Double
    ): Pair<Double, Int> {
        val pts = PolylineUtils.decode(route.overviewPolyline.encodedPath)
        
        // Calculate route length in meters
        val routeLengthM = route.legs.sumOf { it.distance.inMeters.toDouble() }
        
        // Adaptive sampling based on route length (1 sample per 25-50m)
        val maxSamples = (routeLengthM / 25).toInt().coerceIn(20, 400)
        val sampleDistance = routeLengthM / maxSamples
        
        // Use length-proportional sampling
        val samples = PolylineUtils.sampleEvery(pts, sampleDistance)
        
        // Progressive risk calculation
        var totalRiskIntegral = 0.0
        var highRiskSegmentCount = 0 // Count high-risk segments for better differentiation
        
        // Calculate risk for each point along the route
        samples.forEachIndexed { idx, point ->
            // Get risk at this point with increased radius for better crime detection
            val pointRisk = index.riskAt(point, RISK_RADIUS_METERS) * timeMultiplier
            
            // Use a lower threshold for high-risk detection to be more sensitive
            // Track high-risk segments for route evaluation
            if (pointRisk > MAX_NORMALIZED_RISK * HIGH_RISK_THRESHOLD) {
                // Count more severely for very high risk areas
                if (pointRisk > MAX_NORMALIZED_RISK * 0.7) {
                    highRiskSegmentCount += 2  // Double count very high risk areas
                } else {
                highRiskSegmentCount++
                }
            }
            
            // Points near start/end get less weight than middle of route
            val positionInRoute = idx.toDouble() / (samples.size - 1)
            val positionWeight = calculatePositionalWeight(positionInRoute)
            
            // For risk integral: calculate segment length if not first point
            val segmentContribution = if (idx > 0) {
                val prevPoint = samples[idx - 1]
                val segmentLength = calculateHaversineDistance(prevPoint, point)
                pointRisk * segmentLength * positionWeight
            } else {
                // First point contributes based on risk only
                pointRisk * positionWeight
            }
            
            totalRiskIntegral += segmentContribution
        }
        
        // Calculate total route length for samples
        var totalSampleLength = 0.0
        for (i in 1 until samples.size) {
            totalSampleLength += calculateHaversineDistance(samples[i-1], samples[i])
        }
        
        // Normalize the risk integral by route length for a final value
        // This prioritizes both risk level AND route length
        val finalRisk = if (totalSampleLength > 0) {
            // Apply risk amplification to make differences more noticeable, especially on shorter routes
            (totalRiskIntegral / totalSampleLength * RISK_AMPLIFICATION_FACTOR).coerceIn(0.0, MAX_NORMALIZED_RISK)
        } else {
            0.0
        }
        
        return Pair(finalRisk, highRiskSegmentCount)
    }
    
    // Calculate position weight using a bell curve - middle of route gets highest weight
    private fun calculatePositionalWeight(position: Double): Double {
        // Bell curve with peak at 0.5 (middle of route)
        return 0.5 + 0.5 * exp(-16 * (position - 0.5) * (position - 0.5))
    }
    
    // Calculate route cost considering safety, distance, and time - COMPLETELY REVISED
    private fun calculateRouteCost(
        normalizedRisk: Double,
        normalizedDistance: Double, 
        normalizedDuration: Double,
        safetyWeight: Double,
        distanceScalingFactor: Double
    ): Double {
        // Enhance sensitivity to risk by using a steeper logistic function
        val safetyComponent = if (safetyWeight > 0.9) {
            // Use steeper logistic function for better risk sensitivity
            safetyWeight / (1.0 + exp(-10 * (normalizedRisk - 0.4)))
        } else {
            // More responsive linear model for fastest mode
            safetyWeight * normalizedRisk * 1.2
        }
        
        // Efficiency component with better balance
        val timeWeight = if (safetyWeight > 0.9) {
            0.25  // In safest mode: reduce time focus to 25%
        } else {
            0.9  // In fastest mode: 90% time-focused
        }
        
        val distanceWeight = 1.0 - timeWeight
        
        // Raw efficiency calculation (always on a consistent 0-1 scale)
        val efficiencyRaw = timeWeight * normalizedDuration + 
                           distanceWeight * normalizedDistance
        
        // Apply scaling and weighting
        val efficiencyComponent = (1.0 - safetyWeight) * efficiencyRaw * distanceScalingFactor
        
        DebugLogger.logDebug(TAG, "Route cost calculation - safetyWeight: $safetyWeight, " +
                "safetyComponent: $safetyComponent, efficiencyComponent: $efficiencyComponent, " +
                "risk: $normalizedRisk, distance: $normalizedDistance, duration: $normalizedDuration")
        
        return safetyComponent + efficiencyComponent
    }
        
    // Helper function to calculate direct distance as fallback
    private fun calculateHaversineDistance(point1: LatLng, point2: LatLng): Double {
        val R = 6371e3 // Earth radius in meters
        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLng = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLng/2) * Math.sin(deltaLng/2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
        
        return R * c // Distance in meters
    }

    /**
     * Find routes between two points
     * 
     * @param start Starting location
     * @param end Destination location
     * @param simulatedHour Hour of day (0-23) for risk calculation
     * @param preferSafeRoute Whether to prefer a safer route (true) or faster route (false)
     * @return List of RouteCandidate objects, sorted by preferred order
     */
    suspend fun findRoutes(
        start: LatLng,
        end: LatLng,
        simulatedHour: Int,
        preferSafeRoute: Boolean = true
    ): List<RouteCandidate> = coroutineScope {
        // Get time multiplier for risk calculation
        val timeMultiplier = CrimeSpatialIndex.getTimeOfDayMultiplier(simulatedHour)

        // Adjust alternatives parameter based on mode
        val alternatives = true
        
        try {
            val routes = withContext(Dispatchers.IO) {
                val routeMode = "walking" // Always use walking mode for consistent route calculations
                DirectionsClient.getRoutes(start, end, routeMode, alternatives)
            }

            if (routes.isEmpty()) {
                Log.w(TAG, "No routes found")
                return@coroutineScope emptyList()
            }

            // Make sure we have a valid spatial index
            val spatialIndex = crimeSpatialIndex ?: run {
                Log.e(TAG, "No crime spatial index available")
                return@coroutineScope emptyList()
            }

            // Calculate metrics for each route
            val routeCandidates = routes.map { route ->
                // Calculate distance in meters
                val distanceM = route.legs.sumOf { it.distance.inMeters.toDouble() }
                
                // Calculate travel time in seconds
                val durationS = route.legs.sumOf { it.duration.inSeconds.toDouble() }
                
                // Calculate route risk and high-risk segments
                val (riskScore, highRiskSegments) = calculateRouteRiskAndSegments(route, spatialIndex, timeMultiplier)
                
                // Create route candidate with all metrics
                RouteCandidate(
                    route = route,
                    riskScore = riskScore,
                    distanceM = distanceM,
                    durationS = durationS,
                    highRiskSegments = highRiskSegments
                )
            }

            // Sort routes by risk (for safest) or duration (for fastest)
            val sortedRoutes = if (preferSafeRoute) {
                // For safe mode, primarily sort by risk score
                routeCandidates.sortedBy { it.riskScore }
            } else {
                // For fast mode, primarily sort by duration
                routeCandidates.sortedBy { it.durationS }
            }
            
            return@coroutineScope sortedRoutes
        } catch (e: Exception) {
            Log.e(TAG, "Error finding routes", e)
            return@coroutineScope emptyList()
        }
    }
}

// Helper to create a direct route as fallback when no route is available
private object StraightLineRouteCreator {
    fun createStraightLine(origin: LatLng, dest: LatLng): com.google.maps.model.DirectionsRoute {
        val distance = calculateDistance(origin, dest)
        
        return com.google.maps.model.DirectionsRoute().apply {
            legs = arrayOf(com.google.maps.model.DirectionsLeg().apply {
                this.distance = com.google.maps.model.Distance().apply {
                    inMeters = distance.toLong()
                }
                // Estimate duration based on walking speed (5 km/h = 1.4 m/s)
                this.duration = com.google.maps.model.Duration().apply {
                    inSeconds = (distance / 1.4).toLong()
                }
            })
            overviewPolyline = com.google.maps.model.EncodedPolyline(
                PolylineUtils.encode(listOf(origin, dest))
            )
        }
    }
    
    private fun calculateDistance(origin: LatLng, dest: LatLng): Double {
        val R = 6371e3 // Earth radius in meters
        val lat1 = Math.toRadians(origin.latitude)
        val lat2 = Math.toRadians(dest.latitude)
        val deltaLat = Math.toRadians(dest.latitude - origin.latitude)
        val deltaLng = Math.toRadians(dest.longitude - origin.longitude)
        
        val a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLng/2) * Math.sin(deltaLng/2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
        
        return R * c // Distance in meters
    }
    
    // Simple polyline encoder for the straight line case
    private fun PolylineUtils.encode(points: List<LatLng>): String {
        return com.google.maps.android.PolyUtil.encode(points)
        }
}
