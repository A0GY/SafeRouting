package com.universityofreading.demo.navigation

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.universityofreading.demo.R   // for google_maps_key

/**
 * Call once (e.g. from MapScreen) with any Context to initialise.
 * After that you can use the suspend `getAlternatives()` helper.
 */
object DirectionsClient {
    private const val TAG = "DirectionsClient"
    private var geoCtx: GeoApiContext? = null

    fun init(context: Context) {
        if (geoCtx == null) {
            try {
                val apiKey = context.getString(R.string.google_maps_key)
                Log.d(TAG, "Initializing DirectionsClient with API key")
                geoCtx = GeoApiContext.Builder()
                    .apiKey(apiKey)
                    .build()
                Log.d(TAG, "DirectionsClient successfully initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize DirectionsClient", e)
                throw e
            }
        } else {
            Log.d(TAG, "DirectionsClient already initialized")
        }
    }

    /**
     * Get route alternatives with different optimization strategies based on isSafestMode parameter
     * 
     * @param origin Starting point
     * @param dest Destination point
     * @param isSafestMode If true, prioritize alternatives that might be longer but safer; if false, prioritize quickest route
     * @return List of route alternatives
     */
    suspend fun getAlternatives(
        origin: LatLng,
        dest: LatLng,
        isSafestMode: Boolean = false
    ) = withContext(Dispatchers.IO) {
        try {
            val ctx = geoCtx ?: throw IllegalStateException("DirectionsClient not initialized")
            Log.d(TAG, "Requesting directions from ${origin.latitude},${origin.longitude} to ${dest.latitude},${dest.longitude} (mode: ${if (isSafestMode) "SAFEST" else "FASTEST"})")
            
            val combinedRoutes = mutableListOf<com.google.maps.model.DirectionsRoute>()
            
            // First travel mode - Always request walking
            try {
                val walkingResult = DirectionsApi.newRequest(ctx)
                    .origin("${origin.latitude},${origin.longitude}")
                    .destination("${dest.latitude},${dest.longitude}")
                    .mode(TravelMode.WALKING)
                    .alternatives(true)
                    .await()
                
                combinedRoutes.addAll(walkingResult.routes)
                Log.d(TAG, "Received ${walkingResult.routes.size} walking routes")
            } catch (e: Exception) {
                Log.w(TAG, "Error getting walking directions: ${e.message}")
            }
            
            // For safest mode, try additional travel modes to get more diverse geometries
            if (isSafestMode) {
                // Try cycling routes - often provide different paths than walking
                try {
                    val cyclingResult = DirectionsApi.newRequest(ctx)
                        .origin("${origin.latitude},${origin.longitude}")
                        .destination("${dest.latitude},${dest.longitude}")
                        .mode(TravelMode.BICYCLING)
                        .alternatives(true)
                        .await()
                    
                    combinedRoutes.addAll(cyclingResult.routes)
                    Log.d(TAG, "Added ${cyclingResult.routes.size} cycling routes")
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting cycling directions: ${e.message}")
                }
            } else {
                // In fastest mode, request walking routes with different parameters to get more alternatives
                try {
                    // Request more walking routes with different parameters
                    val altWalkingResult = DirectionsApi.newRequest(ctx)
                        .origin("${origin.latitude},${origin.longitude}")
                        .destination("${dest.latitude},${dest.longitude}")
                        .mode(TravelMode.WALKING)
                        .alternatives(true)
                        .optimizeWaypoints(true) // Try to optimize for efficiency
                        .await()
                    
                    combinedRoutes.addAll(altWalkingResult.routes)
                    Log.d(TAG, "Added ${altWalkingResult.routes.size} additional walking routes for fastest mode")
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting additional walking directions: ${e.message}")
                }
            }
            
            // If we have routes, return them
            if (combinedRoutes.isNotEmpty()) {
                Log.d(TAG, "Returning combined total of ${combinedRoutes.size} routes")
                return@withContext combinedRoutes.distinctBy { it.overviewPolyline.encodedPath }
            }
            
            // Fallback: Try direct route without alternatives
            try {
                val result = DirectionsApi.newRequest(ctx)
                    .origin("${origin.latitude},${origin.longitude}")
                    .destination("${dest.latitude},${dest.longitude}")
                    .alternatives(false) // Just get one route
                    .mode(TravelMode.WALKING)
                    .await()
                
                val routes = result.routes.toList()
                Log.d(TAG, "Fallback successful, got ${routes.size} routes")
                
                if (routes.isNotEmpty()) {
                    return@withContext routes
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error getting directions with fallback: ${e.message}")
                // Continue to final fallback
            }
            
            // Ultimate fallback: create a straight line route
            Log.w(TAG, "No routes found from API, creating straight line fallback")
            val straightLine = createStraightLineRoute(origin, dest)
            return@withContext listOf(straightLine)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting route alternatives", e)
            // Return an empty list instead of throwing
            emptyList()
        }
    }
    
    /**
     * Creates a fallback straight line route when no routes are available
     */
    private fun createStraightLineRoute(origin: LatLng, dest: LatLng): com.google.maps.model.DirectionsRoute {
        // Calculate direct distance
        val distance = calculateDistance(origin, dest)
        
        // Create a simple route
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
            // Create an encoded polyline with just start and end points
            overviewPolyline = com.google.maps.model.EncodedPolyline(
                com.google.maps.android.PolyUtil.encode(listOf(origin, dest))
            )
        }
    }
    
    /**
     * Calculates distance between two points using the Haversine formula
     */
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

    /**
     * Get routes with a specified travel mode
     * 
     * @param origin Starting point
     * @param dest Destination point
     * @param mode The travel mode ("walking", "driving", "bicycling", "transit")
     * @param alternatives Whether to request alternative routes
     * @return List of route alternatives
     */
    suspend fun getRoutes(
        origin: LatLng,
        dest: LatLng,
        mode: String = "walking",
        alternatives: Boolean = true
    ) = withContext(Dispatchers.IO) {
        try {
            val ctx = geoCtx ?: throw IllegalStateException("DirectionsClient not initialized")
            Log.d(TAG, "Requesting directions from ${origin.latitude},${origin.longitude} to ${dest.latitude},${dest.longitude} with mode: $mode")
            
            // Convert string mode to TravelMode
            val travelMode = when (mode.lowercase()) {
                "driving" -> TravelMode.DRIVING
                "bicycling" -> TravelMode.BICYCLING
                "transit" -> TravelMode.TRANSIT
                else -> TravelMode.WALKING
            }
            
            // Make the request
            try {
                val result = DirectionsApi.newRequest(ctx)
                    .origin("${origin.latitude},${origin.longitude}")
                    .destination("${dest.latitude},${dest.longitude}")
                    .mode(travelMode)
                    .alternatives(alternatives)
                    .await()
                
                val routes = result.routes.toList()
                Log.d(TAG, "Received ${routes.size} routes with mode: $mode")
                
                if (routes.isNotEmpty()) {
                    return@withContext routes
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error getting directions with mode $mode: ${e.message}")
                // Continue to fallback
            }
            
            // Fallback: Try with walking mode if not already tried
            if (mode.lowercase() != "walking") {
                try {
                    val result = DirectionsApi.newRequest(ctx)
                        .origin("${origin.latitude},${origin.longitude}")
                        .destination("${dest.latitude},${dest.longitude}")
                        .mode(TravelMode.WALKING)
                        .alternatives(alternatives)
                        .await()
                    
                    val routes = result.routes.toList()
                    Log.d(TAG, "Fallback to walking mode successful, got ${routes.size} routes")
                    
                    if (routes.isNotEmpty()) {
                        return@withContext routes
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting directions with fallback walking mode: ${e.message}")
                    // Continue to final fallback
                }
            }
            
            // Ultimate fallback: create a straight line route
            Log.w(TAG, "No routes found from API, creating straight line fallback")
            val straightLine = createStraightLineRoute(origin, dest)
            return@withContext listOf(straightLine)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting routes", e)
            // Return an empty list instead of throwing
            emptyList()
        }
    }
}
