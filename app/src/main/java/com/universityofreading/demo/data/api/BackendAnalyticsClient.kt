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
import retrofit2.http.Query
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Request/response models for analytics endpoints
 */
data class CrimeStats(
    val borough: String,
    val totalCrimes: Int,
    val crimesByType: Map<String, Int>,
    val riskScore: Double,
    val trendPercentage: Double,
    val dataVersion: String,
    val timestamp: Long
)

data class CrimeTimeSeries(
    val dates: List<String>,
    val values: List<Int>,
    val crimeType: String? = null,
    val borough: String? = null,
    val dataVersion: String,
    val timestamp: Long
)

data class ComparisonStats(
    val borough1: String,
    val borough2: String,
    val borough1Crimes: Int,
    val borough2Crimes: Int,
    val borough1Risk: Double,
    val borough2Risk: Double,
    val percentage1: Double,
    val percentage2: Double,
    val dataVersion: String,
    val timestamp: Long
)

data class DetailedCrimeStats(
    val borough: String,
    val totalCrimes: Int,
    val crimeCategories: List<CrimeCategory>,
    val timeSeriesData: CrimeTimeSeries,
    val riskFactors: List<String>,
    val safetyRecommendations: List<String>,
    val dataVersion: String,
    val timestamp: Long
)

data class CrimeCategory(
    val name: String,
    val count: Int,
    val percentage: Double,
    val trend: String  // "increasing", "stable", "decreasing"
)

/**
 * Retrofit service interface for analytics endpoints
 */
interface BackendAnalyticsService {
    @GET("api/analytics/borough/{borough}")
    suspend fun getBoroughStats(
        @Path("borough") borough: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): DetailedCrimeStats

    @GET("api/analytics/compare")
    suspend fun compareBorough(
        @Query("borough1") borough1: String,
        @Query("borough2") borough2: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): ComparisonStats

    @GET("api/analytics/timeseries")
    suspend fun getTimeSeries(
        @Query("borough") borough: String? = null,
        @Query("crime_type") crimeType: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("interval") interval: String = "day"  // "day", "week", "month"
    ): CrimeTimeSeries

    @GET("api/analytics/top-crimes")
    suspend fun getTopCrimes(
        @Query("limit") limit: Int = 10,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Map<String, Int>

    @GET("api/analytics/borough-ranking")
    suspend fun getBoroughRanking(
        @Query("metric") metric: String = "crime_count",  // "crime_count", "risk_score"
        @Query("order") order: String = "desc"
    ): List<CrimeStats>
}

/**
 * Client for analytics endpoints
 * Consumes backend-aggregated statistics with data versioning for cache invalidation
 */
object BackendAnalyticsClient {
    private const val TAG = "BackendAnalyticsClient"
    
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

    private val apiService: BackendAnalyticsService by lazy {
        retrofit.create(BackendAnalyticsService::class.java)
    }

    // Local cache with versioning
    private val statsCache = mutableMapOf<String, Pair<DetailedCrimeStats, String>>()
    private var lastCacheVersion: String? = null

    /**
     * Get detailed crime statistics for a borough
     */
    suspend fun getBoroughStats(
        borough: String,
        startDate: String? = null,
        endDate: String? = null,
        forceRefresh: Boolean = false
    ): DetailedCrimeStats? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache
                if (!forceRefresh && statsCache.containsKey(borough)) {
                    val (cached, version) = statsCache[borough]!!
                    // Check if version is still valid
                    if (version == lastCacheVersion) {
                        DebugLogger.logDebug(TAG, "Using cached borough stats for $borough")
                        return@withContext cached
                    }
                }

                DebugLogger.logDebug(TAG, "Fetching borough stats for $borough")

                val stats = apiService.getBoroughStats(borough, startDate, endDate)

                // Cache with versioning
                statsCache[borough] = Pair(stats, stats.dataVersion)
                lastCacheVersion = stats.dataVersion

                DebugLogger.logDebug(TAG, "Borough stats fetched (version: ${stats.dataVersion})")
                stats
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error fetching borough stats: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Compare crime statistics between two boroughs
     */
    suspend fun compareBorough(
        borough1: String,
        borough2: String,
        startDate: String? = null,
        endDate: String? = null
    ): ComparisonStats? {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Comparing boroughs: $borough1 vs $borough2")

                val comparison = apiService.compareBorough(
                    borough1, borough2, startDate, endDate
                )

                DebugLogger.logDebug(TAG, "Comparison fetched (version: ${comparison.dataVersion})")
                comparison
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error comparing boroughs: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Get time series data for crime trends
     */
    suspend fun getTimeSeries(
        borough: String? = null,
        crimeType: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        interval: String = "day"
    ): CrimeTimeSeries? {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Fetching time series (borough=$borough, type=$crimeType)")

                val series = apiService.getTimeSeries(
                    borough, crimeType, startDate, endDate, interval
                )

                DebugLogger.logDebug(TAG, "Time series fetched (${series.dates.size} data points)")
                series
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error fetching time series: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Get top crimes by count
     */
    suspend fun getTopCrimes(
        limit: Int = 10,
        startDate: String? = null,
        endDate: String? = null
    ): Map<String, Int> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Fetching top crimes (limit=$limit)")

                val topCrimes = apiService.getTopCrimes(limit, startDate, endDate)

                DebugLogger.logDebug(TAG, "Top crimes fetched: ${topCrimes.size} crime types")
                topCrimes
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error fetching top crimes: ${e.message}", e)
                emptyMap()
            }
        }
    }

    /**
     * Get borough ranking by crime or risk metric
     */
    suspend fun getBoroughRanking(
        metric: String = "crime_count",
        order: String = "desc"
    ): List<CrimeStats> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Fetching borough ranking (metric=$metric, order=$order)")

                val ranking = apiService.getBoroughRanking(metric, order)

                DebugLogger.logDebug(TAG, "Borough ranking fetched: ${ranking.size} boroughs")
                ranking
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error fetching borough ranking: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Clear cache and force fresh data on next request
     */
    fun clearCache() {
        statsCache.clear()
        lastCacheVersion = null
        DebugLogger.logDebug(TAG, "Analytics cache cleared")
    }

    /**
     * Check if data version has changed (for cache invalidation)
     */
    fun isDataVersionChanged(currentVersion: String): Boolean {
        return currentVersion != lastCacheVersion
    }
}
