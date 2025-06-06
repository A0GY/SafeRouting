package com.universityofreading.demo.navigation

/**
 * Data class representing the crime analysis for a user-defined area
 * 
 * @param crimeTypeCounts Map of crime types to their counts in the area
 * @param crimeCount Total number of crimes in the area
 * @param averageSeverity Average severity of crimes in the area
 * @param riskPercentage Overall risk percentage for the area (0-100)
 * @param highRiskCrimeCount Number of high-risk crimes (severity >= 7.0)
 */
data class AreaAnalysis(
    val crimeTypeCounts: Map<String, Int>,
    val crimeCount: Int,
    val averageSeverity: Double,
    val riskPercentage: Int,
    val highRiskCrimeCount: Int
) 