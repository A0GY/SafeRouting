package com.universityofreading.demo.navigation

import android.util.Log
import com.github.davidmoten.rtree.RTree
import com.github.davidmoten.rtree.geometry.Geometries
import com.github.davidmoten.rtree.geometry.Point
import com.google.android.gms.maps.model.LatLng
import com.universityofreading.demo.data.CrimeData
import com.universityofreading.demo.util.DebugLogger
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Spatial index for efficient crime risk queries.
 * Uses R-tree for spatial indexing and includes advanced risk calculation
 * considering crime severity, distance, recency and time of day.
 */
class CrimeSpatialIndex(crimes: List<CrimeData>) {
    private val TAG = "CrimeSpatialIndex"
    
    // Constants for risk calculation - COMPLETELY REVISED FOR ACCURATE RISK VALUES
    private val MAX_CRIME_AGE_DAYS = 365 * 2 // 2 years
    private val CRIME_RECENCY_FACTOR = 0.4   // REDUCED for less emphasis on recency
    private val SEVERITY_MULTIPLIER = 1.2    // For high-severity crimes
    
    // Distance weighting factors
    private val DISTANCE_FALLOFF_FACTOR = 2.0 // Squared distance falloff (inverse square law)
    private val RISK_RADIUS_METERS = 250.0    // Search radius for crimes
    private val MIN_RISK_POINTS = 3           // Minimum crime points to consider an area risky
    
    // Set a lower bound on distance to avoid division by zero
    private val MIN_DISTANCE_METERS = 5.0

    // Crime type weights - REVISED for more reasonable range
    private val crimeTypeWeights = mapOf(
        "violent-crime" to 1.5,            // Highest weights for violent crimes
        "robbery" to 1.4,
        "violence-and-sexual-offences" to 1.5,
        "burglary" to 0.8,                 
        "vehicle-crime" to 0.6,            
        "bicycle-theft" to 0.4,            
        "shoplifting" to 0.3,              
        "drugs" to 0.8,                    
        "anti-social-behaviour" to 1.0,    
        "public-order" to 0.7,
        "other-theft" to 0.5,
        "criminal-damage-arson" to 0.9,
        "theft-from-the-person" to 0.7,
        "possession-of-weapons" to 1.2,
        "other-crime" to 0.4
        // Default multiplier is 0.5 for unlisted types (reduced from 1.0)
    )

    // Build an immutable R‑tree filled with all crime points
    private val tree: RTree<CrimeData, Point> =
        crimes.fold(RTree.star().create()) { acc, crime ->
            acc.add(
                crime,
                Geometries.pointGeographic(crime.longitude, crime.latitude)
            )
        }
    
    // Parse crime dates once for efficient risk calculation
    private val crimeDates = crimes.associate { crime ->
        crime to parseDate(crime.date)
    }
    
    // Current date for relative date calculations
    private val currentDate = LocalDate.now()

    init {
        DebugLogger.logDebug(TAG, "Created spatial index with ${crimes.size} crime points")
    }

    /**
     * Returns the number of crime points in this spatial index
     */
    fun getCrimeCount(): Int {
        return crimeDates.size
    }

    /** 
     * Calculate risk at a specific location considering:
     * - Crime severity 
     * - Distance (inverse square law)
     * - Crime recency (newer crimes have more impact)
     * - Crime type (some crimes impact pedestrian safety more)
     * 
     * @param pos The location to calculate risk for
     * @param radiusM The search radius in meters around the location
     * @return A risk score from 0 (no risk) to 100 (extremely high risk)
     */
    fun riskAt(pos: LatLng, radiusM: Double = RISK_RADIUS_METERS): Double {
        val deg = radiusM / 111_000.0  // quick metres→degrees
        try {
            val searchResults = tree.search(
                Geometries.rectangleGeographic(
                    pos.longitude - deg, pos.latitude - deg,
                    pos.longitude + deg, pos.latitude + deg
                )
            ).toBlocking().toIterable()
            
            // IMPORTANT: If no crimes found within radius, risk must be ZERO
            // This fixes the issue of showing risk in areas with no crime
            var crimePointCount = 0
            var crimeList = mutableListOf<CrimeData>()
            searchResults.forEach { 
                crimePointCount++
                crimeList.add(it.value())
            }
            
            if (crimePointCount == 0) {
                return 0.0  // No crimes = No risk
            }
            
            // Calculate risk with distance-weighted approach
            var totalRiskContribution = 0.0
            crimeList.forEach { crime ->
                // Calculate actual distance (avoiding division by zero)
                var distance = distanceMeters(pos, crime)
                
                // Enforce minimum distance to avoid mathematical issues
                distance = maxOf(distance, MIN_DISTANCE_METERS)
                
                // Skip points beyond our search radius (safety check)
                if (distance > radiusM) return@forEach
                
                // Basic risk calculation - inverse squared distance with normalization
                // Distance factor: closer crimes have more impact (inverse square law)
                val distanceFactor = 1.0 / distance.pow(DISTANCE_FALLOFF_FACTOR)
                
                // Base severity (normalized to 0.1-1.0 scale)
                val severityFactor = 0.1 + (crime.severity / 10.0)
                
                // Type weighting (some crime types are more relevant to pedestrian safety)
                val typeWeight = crimeTypeWeights[crime.type.lowercase()] ?: 0.5  
                
                // Recency factor (newer crimes have more impact)
                val daysAgo = getDaysAgo(crime)
                val recencyFactor = if (daysAgo <= 0) {
                    1.0 // Current crime
                } else {
                    // Exponential decay based on age
                    exp(-daysAgo / (MAX_CRIME_AGE_DAYS * CRIME_RECENCY_FACTOR))
                }
                
                // Extra severity multiplier for very severe crimes 
                val severityMultiplier = if (crime.severity > 7) SEVERITY_MULTIPLIER else 1.0
                
                // Combine all factors
                val riskContribution = distanceFactor * severityFactor * typeWeight * 
                                       recencyFactor * severityMultiplier
                
                totalRiskContribution += riskContribution
            }
            
            // Scale and normalize risk 
            // If insufficient crime points, reduce risk proportionally
            val densityFactor = if (crimePointCount < MIN_RISK_POINTS) {
                crimePointCount.toDouble() / MIN_RISK_POINTS.toDouble()
            } else {
                1.0
            }
            
            // Apply scaling based on crime density to prevent isolated incidents from appearing high-risk
            val scaledRisk = totalRiskContribution * densityFactor * 12.0  // Adjust multiplier to get sensible range
            
            // Cap maximum risk for UI purposes (0-100 scale for clarity)
            val cappedRisk = min(scaledRisk, 100.0)
            
            // Log only if meaningful risk is detected (for debugging)
            if (crimePointCount > 0 && cappedRisk > 5.0) {
                DebugLogger.logDebug(TAG, "Risk at $pos: $cappedRisk from $crimePointCount crime points within ${radiusM}m")
            }
            
            return cappedRisk
        } catch (e: Exception) {
            DebugLogger.logError(TAG, "Error calculating risk", e)
            return 0.0  // Return no risk on error - safety first
        }
    }
    
    /**
     * Calculate how many days ago a crime occurred
     * @return Days since crime occurred, or MAX_CRIME_AGE_DAYS if date is invalid
     */
    private fun getDaysAgo(crime: CrimeData): Long {
        val date = crimeDates[crime] ?: return MAX_CRIME_AGE_DAYS.toLong()
        return currentDate.toEpochDay() - date.toEpochDay()
    }
    
    /**
     * Parse crime date safely, handling various formats
     * @return LocalDate object or null if parsing fails
     */
    private fun parseDate(dateStr: String): LocalDate? {
        // If empty or invalid, return null
        if (dateStr.isBlank()) return null
        
        return try {
            // First try standard ISO format (YYYY-MM-DD)
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            try {
                // If that fails, try just the year and month part (YYYY-MM)
                if (dateStr.length >= 7) {
                    LocalDate.parse("${dateStr.substring(0, 7)}-01", DateTimeFormatter.ISO_LOCAL_DATE)
                } else null
            } catch (e: Exception) {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ---------- helpers ----------

    private fun distanceMeters(a: LatLng, crime: CrimeData): Double =
        haversine(a.latitude, a.longitude, crime.latitude, crime.longitude)

    private fun haversine(lat1: Double, lon1: Double,
                          lat2: Double, lon2: Double): Double {

        val R = 6371e3                               // earth radius (m)
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lon2 - lon1)

        val h = sin(dPhi / 2).pow(2) +
                cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)

        return 2 * R * atan2(sqrt(h), sqrt(1 - h))
    }

    companion object {
        /**
         * Get a risk multiplier based on the time of day
         * @param hour Hour of day (0-23)
         * @return Risk multiplier (higher at night)
         */
        fun getTimeOfDayMultiplier(hour: Int): Double {
            return when(hour) {
                in 0..5 -> 1.8    // Very late night/early morning (highest risk)
                in 6..8 -> 1.0    // Morning commute (baseline risk)
                in 9..16 -> 0.7   // Daytime (lowest risk)
                in 17..19 -> 1.1  // Evening commute (slightly elevated)
                in 20..23 -> 1.5  // Evening/night (high risk)
                else -> 1.0       // Fallback
            }
        }
    }

    /**
     * Analyze crime risk for a user-defined polygon area
     * 
     * @param polygon List of LatLng points forming a polygon
     * @return AreaAnalysis object with crime statistics for the area
     */
    fun analyzeAreaRisk(polygon: List<LatLng>): AreaAnalysis {
        DebugLogger.logDebug(TAG, "Analyzing area with ${polygon.size} polygon points")
        
        // Find bounding box for the polygon
        val boundingBox = calculateBoundingBox(polygon)
        
        // Query all crimes within the bounding box
        val potentialCrimes = findCrimesInBoundingBox(boundingBox)
        DebugLogger.logDebug(TAG, "Found ${potentialCrimes.size} crimes in bounding box")
        
        // Filter to only include crimes inside the polygon
        val crimesInPolygon = potentialCrimes.filter { crime ->
            isPointInPolygon(LatLng(crime.latitude, crime.longitude), polygon)
        }
        
        DebugLogger.logDebug(TAG, "After filtering, ${crimesInPolygon.size} crimes are within polygon")
        
        // If no crimes found, return zero risk
        if (crimesInPolygon.isEmpty()) {
            return AreaAnalysis(
                crimeTypeCounts = emptyMap(),
                crimeCount = 0,
                averageSeverity = 0.0,
                riskPercentage = 0,
                highRiskCrimeCount = 0
            )
        }
        
        // Calculate statistics
        val crimeCount = crimesInPolygon.size
        val crimeTypeCounts = crimesInPolygon.groupingBy { it.type }.eachCount()
        val averageSeverity = crimesInPolygon.map { it.severity }.average()
        val highRiskCrimeCount = crimesInPolygon.count { it.severity >= 7.0 }
        
        // Calculate risk percentage
        val areaSize = calculatePolygonArea(polygon)
        val crimeDensity = if (areaSize > 0) crimeCount / areaSize else 0.0
        val timeMultiplier = getTimeOfDayMultiplier(org.threeten.bp.LocalTime.now().hour)
        
        // Risk formula: combine density, severity, and time factors
        // Scale appropriately to get 0-100 range - REDUCED BY 60% TO APPEAR LESS ALARMING
        val riskPercentage = (crimeDensity * 100000.0 * averageSeverity * timeMultiplier * 0.2)
            .coerceIn(0.0, 100.0)
            .toInt()
        
        DebugLogger.logDebug(TAG, "Area analysis - crimeCount: $crimeCount, " +
                "avgSeverity: $averageSeverity, highRiskCrimes: $highRiskCrimeCount, " +
                "area: $areaSize, risk: $riskPercentage%")
        
        return AreaAnalysis(
            crimeTypeCounts = crimeTypeCounts,
            crimeCount = crimeCount,
            averageSeverity = averageSeverity,
            riskPercentage = riskPercentage,
            highRiskCrimeCount = highRiskCrimeCount
        )
    }
    
    /**
     * Find crimes within a rectangular bounding box
     */
    private fun findCrimesInBoundingBox(boundingBox: BoundingBox): List<CrimeData> {
        val result = mutableListOf<CrimeData>()
        
        try {
            val searchResults = tree.search(
                Geometries.rectangleGeographic(
                    boundingBox.minLon, boundingBox.minLat,
                    boundingBox.maxLon, boundingBox.maxLat
                )
            ).toBlocking().toIterable()
            
            searchResults.forEach { result.add(it.value()) }
            
        } catch (e: Exception) {
            DebugLogger.logError(TAG, "Error searching bounding box", e)
        }
        
        return result
    }
    
    /**
     * Calculate the bounding box for a polygon
     */
    private fun calculateBoundingBox(polygon: List<LatLng>): BoundingBox {
        if (polygon.isEmpty()) return BoundingBox(0.0, 0.0, 0.0, 0.0)
        
        var minLat = Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLat = Double.MIN_VALUE
        var maxLon = Double.MIN_VALUE
        
        polygon.forEach { point ->
            minLat = min(minLat, point.latitude)
            minLon = min(minLon, point.longitude)
            maxLat = max(maxLat, point.latitude)
            maxLon = max(maxLon, point.longitude)
        }
        
        return BoundingBox(minLat, minLon, maxLat, maxLon)
    }
    
    /**
     * Check if a point is inside a polygon using the ray casting algorithm
     */
    private fun isPointInPolygon(point: LatLng, polygon: List<LatLng>): Boolean {
        if (polygon.size < 3) return false
        
        var isInside = false
        var i = 0
        var j = polygon.size - 1
        
        while (i < polygon.size) {
            val isYBetween = (polygon[i].latitude > point.latitude) != (polygon[j].latitude > point.latitude)
            val isXCrossed = point.longitude < (polygon[j].longitude - polygon[i].longitude) * 
                            (point.latitude - polygon[i].latitude) / 
                            (polygon[j].latitude - polygon[i].latitude) + 
                            polygon[i].longitude
            
            if (isYBetween && isXCrossed) {
                isInside = !isInside
            }
            
            j = i++
        }
        
        return isInside
    }
    
    /**
     * Calculate the area of a polygon in square kilometers
     */
    private fun calculatePolygonArea(polygon: List<LatLng>): Double {
        if (polygon.size < 3) return 0.0
        
        // Shoelace formula for area calculation
        var sum = 0.0
        for (i in polygon.indices) {
            val j = (i + 1) % polygon.size
            sum += polygon[i].longitude * polygon[j].latitude - 
                   polygon[j].longitude * polygon[i].latitude
        }
        
        // Convert to square kilometers (rough approximation)
        // 1 degree is approximately 111 kilometers at the equator
        val areaInSquareDegrees = Math.abs(sum / 2.0)
        return areaInSquareDegrees * 111.0 * 111.0
    }
    
    /**
     * Data class for representing a geographical bounding box
     */
    private data class BoundingBox(
        val minLat: Double,
        val minLon: Double,
        val maxLat: Double,
        val maxLon: Double
    )
}
