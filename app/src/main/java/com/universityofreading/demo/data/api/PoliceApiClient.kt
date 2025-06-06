package com.universityofreading.demo.data.api

import android.content.Context
import com.google.gson.GsonBuilder
import com.universityofreading.demo.data.CrimeData
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Classes representing the Police API response
 */
data class PoliceApiCrime(
    val category: String,
    val location_type: String,
    val location: Location,
    val context: String?,
    val outcome_status: OutcomeStatus?,
    val persistent_id: String,
    val id: Long,
    val location_subtype: String,
    val month: String // Format: "YYYY-MM"
)

data class Location(
    val latitude: String,
    val longitude: String,
    val street: Street
)

data class Street(
    val id: Long,
    val name: String
)

data class OutcomeStatus(
    val category: String,
    val date: String
)

/**
 * Retrofit service interface for the Police API
 */
interface PoliceApiService {
    @GET("crimes-street/all-crime")
    suspend fun getStreetLevelCrimes(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("date") date: String? = null
    ): List<PoliceApiCrime>
}

/**
 * Client for interacting with the Police API
 */
object PoliceApiClient {
    private const val TAG = "PoliceApiClient"
    private const val BASE_URL = "https://data.police.uk/api/"
    
    // Number of months to fetch (API typically has 36 months of data available)
    // The UK Police API has a "rolling 36 months of data" approach
    private const val MAX_MONTHS_TO_FETCH = 3
    
    // Rate limiting parameters
    private const val RATE_LIMIT_DELAY_MS = 1L // Small delay between requests to avoid rate limiting
    private const val RETRY_DELAY_MS = 2000L     // Delay before retrying after a 429 rate limit response
    private const val MAX_RETRIES = 3            // Maximum number of retries for rate limited requests
    
    // Map crime categories to severity scores (1-10 scale)
    private val crimeSeverityMap = mapOf(
        "anti-social-behaviour" to 3.0,
        "bicycle-theft" to 2.0,
        "burglary" to 4.0,
        "criminal-damage-arson" to 4.5,
        "drugs" to 3.0,
        "other-theft" to 2.0,
        "possession-of-weapons" to 5.0,
        "public-order" to 3.0,
        "robbery" to 5.0,
        "shoplifting" to 1.5,
        "theft-from-the-person" to 3.0,
        "vehicle-crime" to 3.0,
        "violent-crime" to 5.0,
        "other-crime" to 2.0
    )
    
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
    
    private val apiService: PoliceApiService by lazy {
        retrofit.create(PoliceApiService::class.java)
    }
    
    /**
     * Gets crime data from the Police API for the specified coordinates
     * Fetches data for multiple months to provide better coverage
     * Handles rate limiting with retries and delays
     */
    suspend fun getCrimesForArea(latitude: Double, longitude: Double): List<CrimeData> {
        return withContext(Dispatchers.IO) {
            try {
                coroutineScope {
                    // Generate list of dates to fetch (previous months)
                    val datesToFetch = generateDateList()
                    DebugLogger.logDebug(TAG, "Fetching crime data for ${datesToFetch.size} months at $latitude, $longitude")
                    
                    // Create async requests for each date with rate limiting consideration
                    val deferredResults = datesToFetch.mapIndexed { index, date ->
                        async {
                            // Add small delay between requests to prevent rate limiting
                            if (index > 0) delay(RATE_LIMIT_DELAY_MS)
                            
                            try {
                                fetchWithRetry(latitude, longitude, date)
                            } catch (e: Exception) {
                                DebugLogger.logError(TAG, "Error fetching crimes for date $date: ${e.message}", e)
                                emptyList()
                            }
                        }
                    }
                    
                    // Combine all results
                    val allResults = deferredResults.awaitAll().flatten()
                    
                    // Deduplicate by combining coordinates, date, and crime type
                    val uniqueResults = allResults.distinctBy { 
                        "${it.latitude},${it.longitude},${it.date},${it.type}" 
                    }
                    
                    DebugLogger.logDebug(TAG, "Fetched ${uniqueResults.size} unique crimes for $latitude, $longitude")
                    uniqueResults
                }
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error in getCrimesForArea: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    /**
     * Fetch crime data with retry logic for rate limiting
     */
    private suspend fun fetchWithRetry(
        latitude: Double, 
        longitude: Double, 
        date: String,
        retryCount: Int = 0
    ): List<CrimeData> {
        try {
            val apiCrimes = apiService.getStreetLevelCrimes(
                lat = latitude, 
                lng = longitude,
                date = date
            )
            
            return apiCrimes.map { convertToAppCrimeData(it) }
        } catch (e: HttpException) {
            if (e.code() == 429 && retryCount < MAX_RETRIES) {
                // Rate limited - wait and retry
                DebugLogger.logDebug(TAG, "Rate limited. Waiting before retry ${retryCount + 1}/${MAX_RETRIES}")
                delay(RETRY_DELAY_MS * (retryCount + 1))
                return fetchWithRetry(latitude, longitude, date, retryCount + 1)
            } else {
                throw e
            }
        }
    }
    
    /**
     * Generates a list of date strings in the format YYYY-MM
     * Returns the most recent available months for better data coverage
     */
    private fun generateDateList(): List<String> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        
        // Generate dates for the last few months, starting with most recent available
        // UK Police API typically has a 2 month lag
        return (0 until MAX_MONTHS_TO_FETCH).map { monthsBack ->
            // Start with the 2nd month back from current (police data typically has 2-month lag)
            val yearMonth = YearMonth.now().minusMonths(2 + monthsBack.toLong())
            yearMonth.format(formatter)
        }
    }
    
    /**
     * Converts API crime data to our application's CrimeData model
     */
    private fun convertToAppCrimeData(apiCrime: PoliceApiCrime): CrimeData {
        val latitude = apiCrime.location.latitude.toDouble()
        val longitude = apiCrime.location.longitude.toDouble()
        
        // Get severity from our mapping or default to 1.0
        val severity = crimeSeverityMap[apiCrime.category] ?: 1.0
        
        // Format date from "YYYY-MM" to "YYYY-MM-01" for compatibility
        val date = "${apiCrime.month}-01"
        
        // Format crime type for display
        val type = apiCrime.category.replace("-", " ").capitalizeWords()
        
        // Determine region
        val region = getRegionFromCoordinates(latitude, longitude)
        
        return CrimeData(
            latitude = latitude,
            longitude = longitude,
            severity = severity,
            date = date,
            type = type,
            region = region
        )
    }
    
    /**
     * Calculates the most likely London borough based on coordinates
     */
    private fun getRegionFromCoordinates(latitude: Double, longitude: Double): String {
        // London boroughs mapping (comprehensive list)
        val boroughs = mapOf(
            "Westminster" to Pair(51.5074, -0.1278),
            "Camden" to Pair(51.5390, -0.1425),
            "Islington" to Pair(51.5465, -0.1058),
            "Hackney" to Pair(51.5450, -0.0554),
            "Tower Hamlets" to Pair(51.5096, -0.0177),
            "Greenwich" to Pair(51.4826, 0.0077),
            "Lewisham" to Pair(51.4526, -0.0154),
            "Southwark" to Pair(51.5055, -0.0907),
            "Lambeth" to Pair(51.4900, -0.1221),
            "Wandsworth" to Pair(51.4567, -0.1910),
            "Hammersmith and Fulham" to Pair(51.4927, -0.2339),
            "Kensington and Chelsea" to Pair(51.5000, -0.1919),
            "Brent" to Pair(51.5588, -0.2817),
            "Ealing" to Pair(51.5130, -0.3089),
            "Hounslow" to Pair(51.4746, -0.3680),
            "Richmond upon Thames" to Pair(51.4479, -0.3260),
            "Kingston upon Thames" to Pair(51.4085, -0.2861),
            "Merton" to Pair(51.4097, -0.1978),
            "Sutton" to Pair(51.3618, -0.1945),
            "Croydon" to Pair(51.3762, -0.0982),
            "Bromley" to Pair(51.4039, 0.0198),
            "Barnet" to Pair(51.6252, -0.1517),
            "Harrow" to Pair(51.5898, -0.3346),
            "Hillingdon" to Pair(51.5441, -0.4760),
            "Enfield" to Pair(51.6521, -0.0807),
            "Waltham Forest" to Pair(51.5908, -0.0134),
            "Redbridge" to Pair(51.5590, 0.0741),
            "Havering" to Pair(51.5812, 0.1837),
            "Barking and Dagenham" to Pair(51.5462, 0.1313),
            "Newham" to Pair(51.5076, 0.0343),
            "Bexley" to Pair(51.4549, 0.1505),
            "Haringey" to Pair(51.5906, -0.1110)
        )
    
        // Find the closest borough
        return boroughs.minByOrNull { (_, coords) ->
            val latDiff = coords.first - latitude
            val lonDiff = coords.second - longitude
            (latDiff * latDiff) + (lonDiff * lonDiff)
        }?.key ?: "Unknown"
    }
    
    /**
     * Extension function to capitalize each word in a string
     */
    private fun String.capitalizeWords(): String {
        return this.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
} 