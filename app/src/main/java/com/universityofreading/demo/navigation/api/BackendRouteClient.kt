package com.universityofreading.demo.navigation.api

import android.util.Log
import com.google.gson.GsonBuilder
import com.universityofreading.demo.BuildConfig
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Request/response models for backend routing API
 */
data class RouteRequest(
    val origin: LocationPoint,
    val destination: LocationPoint,
    val mode: String,              // "walking", "cycling", "driving"
    val timeOfDay: String? = null, // "morning", "afternoon", "evening", "night"
    val avoidAreas: List<LatLngBound>? = null
)

data class LocationPoint(
    val latitude: Double,
    val longitude: Double
)

data class LatLngBound(
    val minLat: Double,
    val minLng: Double,
    val maxLat: Double,
    val maxLng: Double
)

data class RouteResponse(
    val routes: List<ScoredRoute>,
    val requestHash: String,  // For caching
    val timestamp: Long,
    val dataVersion: String   // For cache invalidation
)

data class ScoredRoute(
    val id: String,
    val geometry: String,     // Encoded polyline geometry
    val distanceMeters: Double,
    val durationSeconds: Double,
    val riskScore: Double,    // 0-100, backend-calculated
    val riskExplanation: List<RiskFactor>,
    val segments: List<RouteSegment>
)

data class RouteSegment(
    val index: Int,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val distanceMeters: Double,
    val riskLevel: String,    // "low", "medium", "high"
    val riskScore: Double,
    val crimeTypes: List<String>
)

data class RiskFactor(
    val type: String,         // "crime_density", "time_of_day", "isolated_area"
    val severity: String,     // "low", "medium", "high"
    val description: String,
    val value: Double         // Numeric value (e.g., crime count)
)

/**
 * Retrofit service interface for the SafeRouting backend route API
 */
interface BackendRouteService {
    @POST("api/routes")
    suspend fun postRoute(
        @Body request: RouteRequest
    ): RouteResponse

    @GET("api/routes/{routeId}")
    suspend fun getRoute(
        @Path("routeId") routeId: String
    ): ScoredRoute

    @POST("api/routes/batch")
    suspend fun postBatchRoutes(
        @Body requests: List<RouteRequest>
    ): List<RouteResponse>
}

/**
 * Client for interacting with the SafeRouting backend route/scoring API
 * Handles route computation, risk scoring, and caching
 */
object BackendRouteClient {
    private const val TAG = "BackendRouteClient"
    
    // Get base URL from BuildConfig (configured in build.gradle.kts)
    private val BASE_URL: String
        get() = BuildConfig.API_BASE_URL

    // In-memory cache for routes
    private val routeCache = mutableMapOf<String, ScoredRoute>()

    // Initialize Retrofit service
    private val retrofit by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val gson = GsonBuilder().setLenient().create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private val apiService: BackendRouteService by lazy {
        retrofit.create(BackendRouteService::class.java)
    }

    /**
     * Request scored routes from the backend
     * Replaces on-device SafeRoutePlanner risk sampling
     */
    suspend fun getRoutes(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        mode: String = "walking",
        timeOfDay: String? = null,
        avoidAreas: List<LatLngBound>? = null
    ): List<ScoredRoute> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(
                    TAG,
                    "Requesting routes from backend: ($originLat, $originLng) -> ($destLat, $destLng)"
                )

                val request = RouteRequest(
                    origin = LocationPoint(originLat, originLng),
                    destination = LocationPoint(destLat, destLng),
                    mode = mode,
                    timeOfDay = timeOfDay,
                    avoidAreas = avoidAreas
                )

                val response = apiService.postRoute(request)

                DebugLogger.logDebug(
                    TAG,
                    "Received ${response.routes.size} routes from backend (version: ${response.dataVersion})"
                )

                // Cache routes for quick access
                response.routes.forEach { route ->
                    routeCache[route.id] = route
                }

                response.routes
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error requesting routes from backend: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Get a cached route by ID
     */
    fun getCachedRoute(routeId: String): ScoredRoute? {
        return routeCache[routeId]
    }

    /**
     * Clear route cache
     */
    fun clearCache() {
        routeCache.clear()
        DebugLogger.logDebug(TAG, "Route cache cleared")
    }

    /**
     * Request multiple routes in a single batch
     */
    suspend fun getRoutesBatch(
        requests: List<RouteRequest>
    ): List<RouteResponse> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Requesting batch of ${requests.size} routes from backend")

                val responses = apiService.postBatchRoutes(requests)

                DebugLogger.logDebug(TAG, "Received batch response for ${responses.size} requests")

                // Cache all routes
                responses.forEach { response ->
                    response.routes.forEach { route ->
                        routeCache[route.id] = route
                    }
                }

                responses
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error requesting batch routes: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Compute cache key for route request (for disk caching)
     */
    fun computeRequestHash(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        mode: String,
        timeOfDay: String? = null
    ): String {
        val key = "$originLat,$originLng,$destLat,$destLng,$mode,$timeOfDay"
        return key.hashCode().toString()
    }
}
