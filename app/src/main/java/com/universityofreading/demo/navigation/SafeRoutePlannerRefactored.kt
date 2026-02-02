package com.universityofreading.demo.navigation

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.universityofreading.demo.navigation.api.BackendRouteClient
import com.universityofreading.demo.navigation.api.LatLngBound
import com.universityofreading.demo.navigation.api.LocationPoint
import com.universityofreading.demo.navigation.api.RouteRequest
import com.universityofreading.demo.navigation.api.ScoredRoute
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * SafeRoutePlanner - REFACTORED to use backend route scoring
 * 
 * Changes from original:
 * - Removed on-device risk sampling and R-tree construction
 * - Removed CrimeSpatialIndex dependency
 * - Delegates route scoring to backend API
 * - Client maintains cache of scored routes
 * - Backend handles time-of-day and crime density calculations
 */
/**
 * Legacy refactored file: kept as a compatibility shim that delegates to the canonical
 * `SafeRoutePlanner` implementation. This file intentionally does not declare
 * `object SafeRoutePlanner` to avoid duplicate top-level objects.
 */
object SafeRoutePlannerLegacy {
    suspend fun safestRoute(
        origin: LatLng,
        dest: LatLng,
        isSafestMode: Boolean,
        avoidAreas: List<LatLngBound>? = null
    ): Pair<RouteCandidate, List<RouteCandidate>> =
        SafeRoutePlanner.safestRoute(origin, dest, isSafestMode, avoidAreas)

    fun setSimulatedHour(hour: Int?) = SafeRoutePlanner.setSimulatedHour(hour)
    fun getCurrentTimePeriod() = SafeRoutePlanner.getCurrentTimePeriod()
    fun getCurrentTimeRiskLevel() = SafeRoutePlanner.getCurrentTimeRiskLevel()
    fun getCachedRoute(routeId: String) = SafeRoutePlanner.getCachedRoute(routeId)
    fun clearCache() = SafeRoutePlanner.clearCache()
}
