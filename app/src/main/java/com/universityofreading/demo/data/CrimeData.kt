package com.universityofreading.demo.data

/**
 * Data class representing crime information
 * Used throughout the app for displaying crime data and route planning
 */
data class CrimeData(
    val latitude: Double,
    val longitude: Double,
    val severity: Double,
    val date: String,  // Format: "YYYY-MM-DD"
    val type: String,  // Human-readable crime type
    val region: String // London borough or area name
) 