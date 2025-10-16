package com.universityofreading.demo.data.tiles

import com.google.android.gms.maps.model.LatLng
import com.universityofreading.demo.data.CrimeData

/**
 * Representation of aggregated crime data for a single tile as provided by the
 * backend aggregation service.
 */
data class CrimeTileSnapshot(
    val tileId: String,
    val revision: Int,
    val heatmapPoints: List<HeatmapPoint>,
    val clusters: List<ClusterPayload>,
    val incidents: List<CrimeData>
)

data class HeatmapPoint(
    val latitude: Double,
    val longitude: Double,
    val intensity: Double
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

data class ClusterPayload(
    val clusterId: String,
    val latitude: Double,
    val longitude: Double,
    val count: Int,
    val averageSeverity: Double
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

/**
 * Update object emitted by [CrimeTileCache] describing new and removed tiles.
 */
data class CrimeTileDiff(
    val upserts: List<CrimeTileSnapshot>,
    val removedTiles: Set<String>,
    val combinedIncidents: List<CrimeData>,
    val combinedHeatmap: List<HeatmapPoint>,
    val combinedClusters: List<ClusterPayload>,
    val cacheRevision: Long
)
