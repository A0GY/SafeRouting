package com.universityofreading.demo.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.universityofreading.demo.data.api.BackendCrimeClient
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.coroutineScope


private data class RawCrimeData(
    val latitude: Double,
    val longitude: Double,
    val severity: Double,
    val date: String,
    val type: String
)

/**
 * Repository to handle crime data loading from the SafeRouting backend
 * 
 * REFACTORED: Replaces direct Police API calls with backend aggregation.
 * The backend handles multi-borough fetching, deduplication, and data versioning.
 * On-device R-tree construction removed - client now requests pre-scored data.
 */
object CrimeDataRepository {
    
    private const val TAG = "CrimeDataRepository"
    
    // Central London coordinates for initial backend request
    // Backend handles all borough coverage - no more multi-location batching
    private const val CENTER_LAT = 51.5074
    private const val CENTER_LNG = -0.1278
    private const val SEARCH_RADIUS_M = 15000  // Backend returns all London crimes within radius
    
    // Comprehensive list of London boroughs for fallback to local data only
    private val londonBoroughs = mapOf(
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
    
    // Cache loaded data with versioning
    private var cachedCrimeData: List<CrimeData>? = null
    private var cachedDataVersion: String? = null
    
    /**
     * Load crime data from the SafeRouting backend
     * 
     * The backend handles:
     * - Aggregation from all London boroughs
     * - Deduplication of crime records
     * - Data versioning for cache invalidation
     * - Removal of on-device Police API batching risk
     */
    suspend fun loadCrimeData(context: Context, forceRefresh: Boolean = false): List<CrimeData> {
        // Return cached data if available, fresh, and refresh not forced
        if (!forceRefresh && cachedCrimeData != null && cachedDataVersion != null) {
            DebugLogger.logDebug(TAG, "Returning cached crime data (${cachedCrimeData!!.size} records, version: $cachedDataVersion)")
            return cachedCrimeData!!
        }
        
        return coroutineScope {
            try {
                DebugLogger.logDebug(TAG, "Loading crime data from SafeRouting backend")
                
                // Single request to backend - replaces multi-location Police API batching
                val crimeData = BackendCrimeClient.getCrimes(
                    latitude = CENTER_LAT,
                    longitude = CENTER_LNG,
                    radiusMeters = SEARCH_RADIUS_M,
                    limit = 10000
                )
                
                DebugLogger.logDebug(TAG, "Fetched ${crimeData.size} crime records from backend")
                
                // Cache the results with versioning
                cachedCrimeData = crimeData
                // In production, extract version from response header or metadata
                cachedDataVersion = "${System.currentTimeMillis()}"
                
                crimeData
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error loading crime data from backend", e)
                
                // Fallback to local data only if backend is unavailable
                DebugLogger.logDebug(TAG, "Falling back to local crime data")
                loadLocalCrimeData(context)
            }
        }
    }
    



    private fun getRegionFromCoordinates(latitude: Double, longitude: Double): String {
        // Find the closest borough
        return londonBoroughs.minByOrNull { (_, coords) ->
            val latDiff = coords.first - latitude
            val lonDiff = coords.second - longitude
            (latDiff * latDiff) + (lonDiff * lonDiff)
        }?.key ?: "Unknown"
    }
    
    /**
     * Fallback method to load data from local JSON if API fails
     */
    private fun loadLocalCrimeData(context: Context): List<CrimeData> {
        return try {
            // Update to use the new crime data file with expanded coverage
            context.resources.openRawResource(com.universityofreading.demo.R.raw.crime_data_updated)
                .bufferedReader()
                .use { it.readText() }
                .let {
                    // Parse JSON to raw crime data list first
                    val gson = Gson()
                    val type = object : TypeToken<List<RawCrimeData>>() {}.type
                    val rawList: List<RawCrimeData> = gson.fromJson(it, type)
                    
                    // Map to our CrimeData class with region added
                    rawList.map { raw ->
                        CrimeData(
                            latitude = raw.latitude,
                            longitude = raw.longitude,
                            severity = raw.severity,
                            date = raw.date,
                            type = raw.type,
                            region = getRegionFromCoordinates(raw.latitude, raw.longitude)
                        )
                    }
                }
        } catch (e: Exception) {
            DebugLogger.logError(TAG, "Error loading local crime data", e)
            emptyList()
        }
    }
} 