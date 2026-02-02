package com.universityofreading.demo.data.api

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
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Request/response models for routine management
 */
data class RoutineRequest(
    val userId: String,
    val name: String,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val mode: String,              // "walking", "cycling", "driving"
    val dayOfWeek: String? = null, // "monday", "tuesday", etc., null for every day
    val startTime: String,         // "HH:mm" format
    val endTime: String,           // "HH:mm" format
    val alertThreshold: Double = 0.5  // Risk threshold (0-1) for notifications
)

data class Routine(
    val id: String,
    val userId: String,
    val name: String,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val mode: String,
    val dayOfWeek: String?,
    val startTime: String,
    val endTime: String,
    val alertThreshold: Double,
    val lastCheckedAt: Long,
    val nextCheckAt: Long,
    val enabled: Boolean
)

data class RoutineRiskAlert(
    val routineId: String,
    val timestamp: Long,
    val currentRiskScore: Double,
    val previousRiskScore: Double,
    val changePercentage: Double,
    val riskFactors: List<String>,
    val recommendedAlternativeRouteId: String?
)

/**
 * Retrofit service interface for routine management
 */
interface BackendRoutineService {
    @POST("api/routines")
    suspend fun createRoutine(
        @Body request: RoutineRequest
    ): Routine

    @GET("api/routines/{routineId}")
    suspend fun getRoutine(
        @Path("routineId") routineId: String
    ): Routine

    @GET("api/routines")
    suspend fun listRoutines(
        @Query("userId") userId: String
    ): List<Routine>

    @PUT("api/routines/{routineId}")
    suspend fun updateRoutine(
        @Path("routineId") routineId: String,
        @Body request: RoutineRequest
    ): Routine

    @POST("api/routines/{routineId}/check")
    suspend fun checkRoutineRisk(
        @Path("routineId") routineId: String
    ): RoutineRiskAlert?

    @POST("api/routines/{routineId}/disable")
    suspend fun disableRoutine(
        @Path("routineId") routineId: String
    )
}

/**
 * Client for routine management (recurring safe routes with background monitoring)
 */
object BackendRoutineClient {
    private const val TAG = "BackendRoutineClient"
    
    // Get base URL from BuildConfig (configured in build.gradle.kts)
    private val BASE_URL: String
        get() = BuildConfig.API_BASE_URL

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

    private val apiService: BackendRoutineService by lazy {
        retrofit.create(BackendRoutineService::class.java)
    }

    /**
     * Create a new routine with background recalculation
     */
    suspend fun createRoutine(
        userId: String,
        name: String,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        mode: String = "walking",
        dayOfWeek: String? = null,
        startTime: String,
        endTime: String,
        alertThreshold: Double = 0.5
    ): Routine? {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Creating routine: $name")

                val request = RoutineRequest(
                    userId = userId,
                    name = name,
                    startLat = startLat,
                    startLng = startLng,
                    endLat = endLat,
                    endLng = endLng,
                    mode = mode,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    alertThreshold = alertThreshold
                )

                val routine = apiService.createRoutine(request)
                DebugLogger.logDebug(TAG, "Routine created with ID: ${routine.id}")
                routine
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error creating routine: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Get routine details
     */
    suspend fun getRoutine(routineId: String): Routine? {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getRoutine(routineId)
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error fetching routine: ${e.message}", e)
                null
            }
        }
    }

    /**
     * List all routines for a user
     */
    suspend fun listRoutines(userId: String): List<Routine> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Listing routines for user: $userId")
                apiService.listRoutines(userId)
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error listing routines: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Check risk level for a routine (triggers backend recalculation and alert if necessary)
     */
    suspend fun checkRoutineRisk(routineId: String): RoutineRiskAlert? {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Checking risk for routine: $routineId")
                apiService.checkRoutineRisk(routineId)
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error checking routine risk: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Disable a routine
     */
    suspend fun disableRoutine(routineId: String) {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Disabling routine: $routineId")
                apiService.disableRoutine(routineId)
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error disabling routine: ${e.message}", e)
            }
        }
    }
}
