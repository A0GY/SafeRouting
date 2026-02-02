package com.universityofreading.demo.data.api

import android.util.Log
import com.google.gson.GsonBuilder
import com.universityofreading.demo.BuildConfig
import com.universityofreading.demo.data.CrimeData
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Response models for backend tile/crime endpoints
 */
data class BackendTileResponse(
    val tiles: List<String>,  // List of tile URLs for CDN/vector tiles
    val version: String,      // Data version for caching
    val timestamp: Long       // Server timestamp
)

data class CrimeIncident(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val severity: Double,
    val date: String,        // Format: "YYYY-MM-DD"
    val type: String,
    val region: String
)

data class BackendCrimesResponse(
    val crimes: List<CrimeIncident>,
    val version: String,
    val timestamp: Long,
    val totalCount: Int
)

data class TileMetadata(
    val zoom: Int,
    val x: Int,
    val y: Int,
    val url: String,
    val version: String
)

/**
 * Retrofit service interface for the SafeRouting backend API
 */
interface BackendCrimeService {
    @GET("api/tiles")
    suspend fun getTiles(
        @Query("zoom") zoom: Int = 13,
        @Query("bbox") bbox: String? = null  // e.g., "minLat,minLng,maxLat,maxLng"
    ): BackendTileResponse

    @GET("api/crimes")
    suspend fun getCrimes(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius_m") radiusMeters: Int = 500,
        @Query("limit") limit: Int = 1000,
        @Query("version") version: String? = null  // For cache validation
    ): BackendCrimesResponse

    @GET("api/analytics/aggregated")
    suspend fun getAggregatedCrimes(
        @Query("borough") borough: String? = null,
        @Query("crime_type") crimeType: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Map<String, Any>
}

/**
 * Client for interacting with the SafeRouting backend API
 * Handles crime data aggregation, tiling, and caching
 */
object BackendCrimeClient {
    private const val TAG = "BackendCrimeClient"
    
    // Get base URL from BuildConfig (configured in build.gradle.kts)
    private val BASE_URL: String
        get() = BuildConfig.API_BASE_URL

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

    private val apiService: BackendCrimeService by lazy {
        retrofit.create(BackendCrimeService::class.java)
    }

    /**
     * Fetch aggregated crime data from the backend
     * Replaces on-device Police API batching
     */
    suspend fun getCrimes(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 500,
        limit: Int = 1000
    ): List<CrimeData> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Fetching crimes from backend for $latitude, $longitude")

                val response = apiService.getCrimes(
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radiusMeters,
                    limit = limit
                )

                val crimeDataList = response.crimes.map { incident ->
                    CrimeData(
                        latitude = incident.latitude,
                        longitude = incident.longitude,
                        severity = incident.severity,
                        date = incident.date,
                        type = incident.type,
                        region = incident.region
                    )
                }

                DebugLogger.logDebug(
                    TAG,
                    "Fetched ${crimeDataList.size} crimes from backend (version: ${response.version})"
                )
                crimeDataList
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error fetching crimes from backend: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Fetch vector tile URLs from the backend
     * Replaces client-side heatmap generation
     */
    suspend fun getTiles(
        zoom: Int = 13,
        bbox: String? = null
    ): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Fetching tile URLs from backend (zoom=$zoom)")

                val response = apiService.getTiles(zoom = zoom, bbox = bbox)

                DebugLogger.logDebug(TAG, "Fetched ${response.tiles.size} tile URLs (version: ${response.version})")
                response.tiles
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error fetching tiles from backend: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Fetch aggregated crime statistics from the backend
     */
    suspend fun getAggregatedCrimes(
        borough: String? = null,
        crimeType: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Fetching aggregated crimes from backend")

                apiService.getAggregatedCrimes(
                    borough = borough,
                    crimeType = crimeType,
                    startDate = startDate,
                    endDate = endDate
                )
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error fetching aggregated crimes: ${e.message}", e)
                emptyMap()
            }
        }
    }
}
