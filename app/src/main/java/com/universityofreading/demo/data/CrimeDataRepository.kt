package com.universityofreading.demo.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.universityofreading.demo.data.api.PoliceApiClient
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


private data class RawCrimeData(
    val latitude: Double,
    val longitude: Double,
    val severity: Double,
    val date: String,
    val type: String
)

/**
 * Repository to handle crime data loading from the Police API
 */
object CrimeDataRepository {
    
    private const val TAG = "CrimeDataRepository"
    
    // Comprehensive list of London borough center coordinates for better coverage
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
    
    // Additional grid points to ensure coverage between borough centers

    private val additionalGridPoints = listOf(
        // Central London additional points
        Pair(51.5185, -0.1562),
        Pair(51.5270, -0.1115),
        Pair(51.5274, -0.0754),
        Pair(51.5169, -0.0730),
        
        // North London additional points
        Pair(51.5697, -0.1064),
        Pair(51.6079, -0.2160),
        
        // East London additional points
        Pair(51.5284, 0.0552),
        Pair(51.5645, 0.1277),
        
        // South London additional points
        Pair(51.4726, -0.0515),
        Pair(51.4216, -0.0568),
        Pair(51.3922, -0.1463),
        
        // West London additional points
        Pair(51.5059, -0.2629),
        Pair(51.4917, -0.3385),
        Pair(51.4312, -0.3470)
    )
    
    // Maximum number of API calls (to avoid rate limiting)
    private const val MAX_PARALLEL_REQUESTS = 8
    
    // Cache loaded data
    private var cachedCrimeData: List<CrimeData>? = null
    
    /**
     * Load crime data from the Police API for multiple London areas

     */
    suspend fun loadCrimeData(context: Context, forceRefresh: Boolean = false): List<CrimeData> {
        // Return cached data if available and refresh not forced
        if (!forceRefresh && cachedCrimeData != null) {
            DebugLogger.logDebug(TAG, "Returning cached crime data (${cachedCrimeData!!.size} records)")
            return cachedCrimeData!!
        }
        
        return coroutineScope {
            try {
                DebugLogger.logDebug(TAG, "Starting to load crime data from Police API")
                
                // Combine borough centers and grid points for complete coverage
                val allCoordinates = londonBoroughs.values.toList() + additionalGridPoints
                
                // Process in batches to avoid overwhelming
                val batchSize = MAX_PARALLEL_REQUESTS
                val results = mutableListOf<CrimeData>()
                
                // Process batches of coordinates
                for (i in allCoordinates.indices step batchSize) {
                    val endIndex = minOf(i + batchSize, allCoordinates.size)
                    val batchCoordinates = allCoordinates.subList(i, endIndex)
                    
                    DebugLogger.logDebug(TAG, "Processing batch ${i/batchSize + 1} with ${batchCoordinates.size} locations")
                    
                    // Create multiple parallel
                    val deferredResults = batchCoordinates.map { (lat, lng) ->
                        async { 
                            try {
                                val result = PoliceApiClient.getCrimesForArea(lat, lng)
                                DebugLogger.logDebug(TAG, "Retrieved ${result.size} crimes for coordinates $lat, $lng")
                                result
                            } catch (e: Exception) {
                                DebugLogger.logError(TAG, "Error fetching crimes for coordinates $lat, $lng", e)
                                emptyList()
                            }
                        }
                    }
                    
                    // Wait for  to complete addresults
                    val batchResults = deferredResults.awaitAll()
                    results.addAll(batchResults.flatten())
                    
                    DebugLogger.logDebug(TAG, "Batch complete. Total crimes collected so far: ${results.size}")
                }
                
                // Deduplicate results 
                val combinedResults = results
                    .distinctBy { "${it.latitude},${it.longitude},${it.date},${it.type}" }
                
                DebugLogger.logDebug(TAG, "Finished loading crime data. Total unique records: ${combinedResults.size}")
                
                // Cache the results
                cachedCrimeData = combinedResults
                combinedResults
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error loading crime data", e)
                
                // Fallback to local data if API fails
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