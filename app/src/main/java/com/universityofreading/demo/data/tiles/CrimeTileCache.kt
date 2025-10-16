package com.universityofreading.demo.data.tiles

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Maintains a local cache of tile snapshots and produces diffs suitable for
 * incremental updates of the [CrimeSpatialIndex] and map overlays.
 */
class CrimeTileCache {
    private val tiles = ConcurrentHashMap<String, CrimeTileSnapshot>()
    private val revision = AtomicLong(0L)

    fun applyResponse(responseTiles: List<CrimeTileSnapshot>, removed: Collection<String>): CrimeTileDiff {
        val upserts = mutableListOf<CrimeTileSnapshot>()
        responseTiles.forEach { tile ->
            val cached = tiles[tile.tileId]
            if (cached == null || cached.revision != tile.revision) {
                tiles[tile.tileId] = tile
                upserts += tile
            }
        }

        val removedIds = removed.toSet()
        removedIds.forEach { tileId -> tiles.remove(tileId) }

        val snapshotRevision = revision.incrementAndGet()

        val incidents = tiles.values.flatMap { it.incidents }
        val heatmap = tiles.values.flatMap { it.heatmapPoints }
        val clusters = tiles.values.flatMap { it.clusters }

        return CrimeTileDiff(
            upserts = upserts,
            removedTiles = removedIds,
            combinedIncidents = incidents,
            combinedHeatmap = heatmap,
            combinedClusters = clusters,
            cacheRevision = snapshotRevision
        )
    }

    fun invalidateAll(): CrimeTileDiff {
        val removed = tiles.keys.toSet()
        tiles.clear()
        val snapshotRevision = revision.incrementAndGet()
        return CrimeTileDiff(
            upserts = emptyList(),
            removedTiles = removed,
            combinedIncidents = emptyList(),
            combinedHeatmap = emptyList(),
            combinedClusters = emptyList(),
            cacheRevision = snapshotRevision
        )
    }

    fun currentRevisions(): Map<String, Int> = tiles.mapValues { it.value.revision }
}
