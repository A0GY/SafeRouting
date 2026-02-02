package com.universityofreading.demo.navigation

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.universityofreading.demo.navigation.api.BackendRouteClient
import com.universityofreading.demo.navigation.api.LatLngBound
import com.universityofreading.demo.navigation.api.LocationPoint
import com.universityofreading.demo.navigation.api.RouteRequest
import com.universityofreading.demo.navigation.api.ScoredRoute
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * SafeRoutePlanner - REFACTORED to use backend route scoring
 * 
 * Changes from original:
 * - Removed on-device risk sampling and R-tree construction
 * - Removed CrimeSpatialIndex dependency
 * - Delegates route scoring to backend API
 * - Client maintains cache of scored routes
 * - Backend handles time-of-day and crime density calculations
 */
object SafeRoutePlanner {
    private const val TAG = "SafeRoutePlanner"
    
    // Simulated hour for testing different times of day
    private var simulatedHour: Int? = null
    
    // Cache for scored routes
    private val routeCache = mutableMapOf<String, ScoredRoute>()
    
    /**
     * Set a simulated hour for route scoring (for testing different times)
     */
    fun setSimulatedHour(hour: Int?) {
        simulatedHour = hour
        DebugLogger.logDebug(TAG, "Set simulated hour to: ${hour ?: "CURRENT_TIME"}")
    }
    
    /**
     * Get current time period for UI display
     */
    fun getCurrentTimePeriod(): String {
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
    
    /**
     * Get risk level for current time period
     */
    fun getCurrentTimeRiskLevel(): String {
        val currentHour = simulatedHour ?: org.threeten.bp.LocalTime.now().hour
        return when(currentHour) {
            in 0..5, in 20..23 -> "High"
            in 17..19 -> "Medium"
            else -> "Low"
        }
    }

    /**
     * Request safest or fastest route from backend
     * 
     * REFACTORED: Replaces on-device SafeRoutePlanner.safestRoute()
     * Backend now handles:
     * - Route geometry from Google Directions API
     * - Risk scoring based on crime data
     * - Time-of-day factors
     * - Segment-level risk analysis
     * - Explainability factors
     */
    suspend fun safestRoute(
        origin: LatLng,
        dest: LatLng,
        isSafestMode: Boolean,
        avoidAreas: List<LatLngBound>? = null
    ): Pair<RouteCandidate, List<RouteCandidate>> =
        withContext(Dispatchers.Default) {
            try {
                DebugLogger.logDebug(
                    TAG,
                    "Requesting routes from backend " +
                    "(mode: ${if (isSafestMode) "Safest" else "Fastest"}, " +
                    "time: ${getCurrentTimePeriod()})"
                )
                
                // Build time-of-day string for backend
                val timeOfDay = getTimeOfDayString(simulatedHour)
                
                // Request routes from backend
                val scoredRoutes = BackendRouteClient.getRoutes(
                    originLat = origin.latitude,
                    originLng = origin.longitude,
                    destLat = dest.latitude,
                    destLng = dest.longitude,
                    mode = "walking",
                    timeOfDay = timeOfDay,
                    avoidAreas = avoidAreas
                )
                
                if (scoredRoutes.isEmpty()) {
                    DebugLogger.logError(TAG, "No routes returned from backend")
                    throw Exception("No routes available from backend")
                }
                
                DebugLogger.logDebug(TAG, "Received ${scoredRoutes.size} scored routes from backend")
                
                // Cache routes
                scoredRoutes.forEach { route ->
                    routeCache[route.id] = route
                }
                
                // Convert backend routes to RouteCandidate objects
                val routeCandidates = scoredRoutes.map { backendRoute ->
                    convertToRouteCandidate(backendRoute)
                }
                
                // Sort based on mode
                val (bestRoute, alternatives) = if (isSafestMode) {
                    // Sort by risk score (lower is better)
                    val sorted = routeCandidates.sortedBy { it.riskScore }
                    sorted.first() to sorted.drop(1).take(2)
                } else {
                    // Sort by duration (faster routes first)
                    val sorted = routeCandidates.sortedBy { it.durationS }
                    sorted.first() to sorted.drop(1).take(2)
                }
                
                DebugLogger.logDebug(
                    TAG,
                    "Selected best route: risk=${bestRoute.riskScore}, " +
                    "distance=${bestRoute.distanceM}m, duration=${bestRoute.durationS}s"
                )
                
                bestRoute to alternatives
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error requesting routes from backend: ${e.message}", e)
                // Fallback to DirectionsClient if backend is unavailable
                return@withContext safestRouteFallback(origin, dest, isSafestMode)
            }
        }
    
    /**
     * Fallback to local DirectionsClient if backend is unavailable
     * This maintains backwards compatibility while new backend is being deployed
     */
    private suspend fun safestRouteFallback(
        origin: LatLng,
        dest: LatLng,
        isSafestMode: Boolean
    ): Pair<RouteCandidate, List<RouteCandidate>> {
        return withContext(Dispatchers.Default) {
            try {
                DebugLogger.logDebug(TAG, "Falling back to DirectionsClient (backend unavailable)")
                
                val directions = DirectionsClient.getAlternatives(origin, dest, isSafestMode)
                
                if (directions.isEmpty()) {
                    return@withContext RouteCandidate(
                        StraightLineRouteCreator.createStraightLine(origin, dest),
                        50.0,  // Default medium risk
                        calculateHaversineDistance(origin, dest)
                    ) to emptyList()
                }
                
                // Create candidates with default risk scores (no on-device calculation)
                val routeCandidates = directions.map { route ->
                    val distanceM = route.legs[0].distance.inMeters.toDouble()
                    val durationS = route.legs[0].duration.inSeconds.toDouble()
                    
                    RouteCandidate(
                        route,
                        50.0,  // Default risk - should use backend for accurate scores
                        distanceM,
                        durationS
                    )
                }
                
                val sorted = routeCandidates.sortedBy { 
                    if (isSafestMode) it.riskScore else it.durationS
                }
                
                sorted.first() to sorted.drop(1).take(2)
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Fallback route selection failed: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * Convert backend ScoredRoute to RouteCandidate for UI
     * 
     * Reconstructs a DirectionsRoute from backend response:
     * - Decodes polyline geometry string to points
     * - Reconstructs legs/steps from segment data
     * - Maps backend data to Google Maps model classes
     */
    private fun convertToRouteCandidate(backendRoute: ScoredRoute): RouteCandidate {
        try {
            // Decode the polyline to get route points
            val decodedPoints = com.google.maps.android.PolyUtil.decode(backendRoute.geometry)
            
            if (decodedPoints.isEmpty()) {
                throw Exception("Decoded polyline is empty")
            }
            
            // Reconstruct DirectionsRoute from backend data
            val directionsRoute = com.google.maps.model.DirectionsRoute().apply {
                // Set overview polyline with original encoded geometry
                overviewPolyline = com.google.maps.model.EncodedPolyline(backendRoute.geometry)
                
                // Reconstruct legs from backend segment data
                legs = arrayOf(
                    com.google.maps.model.DirectionsLeg().apply {
                        // Set distance and duration from backend
                        distance = com.google.maps.model.Distance().apply {
                            inMeters = backendRoute.distanceMeters.toLong()
                            humanReadable = "${(backendRoute.distanceMeters / 1000).toInt()} km"
                        }
                        duration = com.google.maps.model.Duration().apply {
                            inSeconds = backendRoute.durationSeconds.toLong()
                            humanReadable = formatDuration(backendRoute.durationSeconds)
                        }
                        
                        // Reconstruct steps from segments
                        steps = backendRoute.segments.map { segment ->
                            com.google.maps.model.DirectionsStep().apply {
                                distance = com.google.maps.model.Distance().apply {
                                    inMeters = segment.distanceMeters.toLong()
                                }
                                startLocation = com.google.maps.model.LatLng(segment.startLat, segment.startLng)
                                endLocation = com.google.maps.model.LatLng(segment.endLat, segment.endLng)
                                // Encode the start->end coordinates for the step polyline
                                val stepPolyline = com.google.maps.android.PolyUtil.encode(
                                    listOf(
                                        com.google.android.gms.maps.model.LatLng(segment.startLat, segment.startLng),
                                        com.google.android.gms.maps.model.LatLng(segment.endLat, segment.endLng)
                                    )
                                )
                                polyline = com.google.maps.model.EncodedPolyline(stepPolyline)
                            }
                        }.toTypedArray()
                        
                        // Set start/end locations
                        startLocation = com.google.maps.model.LatLng(
                            decodedPoints.first().latitude,
                            decodedPoints.first().longitude
                        )
                        endLocation = com.google.maps.model.LatLng(
                            decodedPoints.last().latitude,
                            decodedPoints.last().longitude
                        )
                    }
                )
            }
            
            return RouteCandidate(
                route = directionsRoute,
                riskScore = backendRoute.riskScore,
                distanceM = backendRoute.distanceMeters,
                durationS = backendRoute.durationSeconds,
                highRiskSegments = backendRoute.segments.count { it.riskLevel == "high" }
            )
        } catch (e: Exception) {
            DebugLogger.logError(
                TAG,
                "Error converting backend route to RouteCandidate: ${e.message}",
                e
            )
            throw Exception("Cannot convert backend route - ${e.message}", e)
        }
    }
    
    /**
     * Format duration in seconds to human-readable string
     */
    private fun formatDuration(seconds: Double): String {
        val totalMinutes = (seconds / 60).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        
        return when {
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} $minutes min"
            else -> "$minutes min"
        }
    }
    
    /**
     * Get time-of-day string for backend request
     */
    private fun getTimeOfDayString(simHour: Int?): String {
        val hour = simHour ?: org.threeten.bp.LocalTime.now().hour
        return when(hour) {
            in 0..5 -> "night"
            in 6..8 -> "morning"
            in 9..16 -> "afternoon"
            in 17..19 -> "evening"
            else -> "night"
        }
    }
    
    /**
     * Calculate Haversine distance between two points
     */
    fun calculateHaversineDistance(origin: LatLng, dest: LatLng): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(dest.latitude - origin.latitude)
        val dLon = Math.toRadians(dest.longitude - origin.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(origin.latitude)) * Math.cos(Math.toRadians(dest.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadiusKm * c * 1000  // Return in meters
    }
    
    /**
     * Get cached route by ID
     */
    fun getCachedRoute(routeId: String): ScoredRoute? {
        return routeCache[routeId]
    }
    
    /**
     * Clear route cache
     */
    fun clearCache() {
        routeCache.clear()
        BackendRouteClient.clearCache()
        DebugLogger.logDebug(TAG, "Route cache cleared")
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
