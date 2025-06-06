package com.universityofreading.demo.navigation

import com.google.maps.model.DirectionsRoute

/**
 * Represents a potential route with safety and distance metrics.
 *
 * @param route The Google DirectionsRoute data
 * @param riskScore The normalized risk score (0-100, higher = more dangerous)
 * @param distanceM Distance in meters
 * @param durationS Travel time in seconds
 * @param highRiskSegments Number of high-risk segments in the route (optional)
 */
data class RouteCandidate(
    val route: DirectionsRoute,
    val riskScore: Double,
    val distanceM: Double,
    val durationS: Double = 0.0,
    val highRiskSegments: Int = 0
) {
    /**
     * Legacy cost calculation method - kept for backwards compatibility.
     * New code should use the improved cost calculation in SafeRoutePlanner.
     */
    fun cost(weight: Double, maxDist: Double): Double =
        weight * riskScore + (1 - weight) * (distanceM / maxDist)
}
