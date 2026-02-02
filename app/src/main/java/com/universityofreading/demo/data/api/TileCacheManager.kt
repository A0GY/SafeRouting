package com.universityofreading.demo.data.api

import android.content.Context
import android.util.Log
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Tile cache manager for backend vector/raster tiles
 * 
 * Replaces on-device heatmap generation with:
 * - Backend-provided vector tiles (Mapbox, etc.)
 * - Disk cache for tiles with versioning
 * - Light-weight detail fetches on marker tap
 */
class TileCacheManager(private val cacheDir: File) {
    private const val TAG = "TileCacheManager"
    private const val CACHE_VERSION = "v1"
    private const val CACHE_SIZE_MB = 100
    
    // OkHttpClient for fetching tiles from backend
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
            DebugLogger.logDebug(TAG, "Created tile cache directory: ${cacheDir.absolutePath}")
        }
    }
    
    /**
     * Get tile from cache or fetch from backend
     */
    suspend fun getTile(
        zoom: Int,
        x: Int,
        y: Int,
        tileUrl: String,
        forceRefresh: Boolean = false
    ): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val cacheFile = getCacheFile(zoom, x, y)
                
                // Check cache first
                if (!forceRefresh && cacheFile.exists()) {
                    val cached = cacheFile.readBytes()
                    DebugLogger.logDebug(TAG, "Loaded tile from cache: $zoom/$x/$y (${cached.size} bytes)")
                    return@withContext cached
                }
                
                // Fetch from backend
                DebugLogger.logDebug(TAG, "Fetching tile from backend: $zoom/$x/$y")
                val tileData = fetchTileFromBackend(tileUrl)
                
                if (tileData != null) {
                    // Cache the tile
                    cacheFile.parentFile?.mkdirs()
                    cacheFile.writeBytes(tileData)
                    DebugLogger.logDebug(TAG, "Cached tile: $zoom/$x/$y (${tileData.size} bytes)")
                }
                
                tileData
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error loading tile $zoom/$x/$y: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Fetch tile from backend CDN/service using OkHttp
     * 
     * Handles:
     * - HTTP GET request to tile URL
     * - Gzip decompression (automatic with OkHttp)
     * - Timeout handling
     * - Error logging
     */
    private suspend fun fetchTileFromBackend(tileUrl: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Fetching tile from backend: $tileUrl")
                
                val request = Request.Builder()
                    .url(tileUrl)
                    .get()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    DebugLogger.logError(
                        TAG,
                        "Failed to fetch tile: HTTP ${response.code} from $tileUrl"
                    )
                    return@withContext null
                }
                
                val tileData = response.body?.bytes()
                
                if (tileData == null) {
                    DebugLogger.logError(TAG, "Tile response body is empty")
                    return@withContext null
                }
                
                DebugLogger.logDebug(
                    TAG,
                    "Successfully fetched tile from backend: ${tileData.size} bytes"
                )
                
                tileData
            } catch (e: Exception) {
                DebugLogger.logError(
                    TAG,
                    "Error fetching tile from backend: ${e.message}",
                    e
                )
                null
            }
        }
    }
    
    /**
     * Get tile cache file path
     */
    private fun getCacheFile(zoom: Int, x: Int, y: Int): File {
        return File(cacheDir, "$CACHE_VERSION/$zoom/$x/${y}.pbf")  // Assume MapBox .pbf format
    }
    
    /**
     * Clear all cached tiles
     */
    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
        DebugLogger.logDebug(TAG, "Tile cache cleared")
    }
    
    /**
     * Get cache size in bytes
     */
    fun getCacheSize(): Long {
        return cacheDir.walk().filter { it.isFile }.map { it.length() }.sum()
    }
    
    /**
     * Clear old tiles to maintain cache size limit
     */
    fun evictOldTiles() {
        val cacheSizeBytes = getCacheSize()
        val maxSizeBytes = CACHE_SIZE_MB * 1024 * 1024
        
        if (cacheSizeBytes > maxSizeBytes) {
            DebugLogger.logDebug(TAG, "Cache exceeds limit (${cacheSizeBytes} > ${maxSizeBytes}). Evicting old tiles.")
            
            // Delete oldest files first
            cacheDir.walk()
                .filter { it.isFile }
                .sortedBy { it.lastModified() }
                .forEach { file ->
                    if (getCacheSize() > maxSizeBytes) {
                        file.delete()
                        DebugLogger.logDebug(TAG, "Evicted tile: ${file.name}")
                    }
                }
        }
    }
}

/**
 * Backend tile endpoint for map layer display
 * Provides vector/raster tiles from server instead of client-side generation
 */
object BackendTileService {
    private const val TAG = "BackendTileService"
    
    // Tile server URL - configure based on deployment
    private const val TILE_BASE_URL = "https://tiles.saferouting.local/data/{z}/{x}/{y}.pbf"
    
    /**
     * Get tile URL for given zoom/x/y coordinates
     */
    fun getTileUrl(zoom: Int, x: Int, y: Int): String {
        return TILE_BASE_URL
            .replace("{z}", zoom.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
    }
    
    /**
     * Get tile layer information
     */
    fun getTileLayerInfo(): Map<String, String> {
        return mapOf(
            "name" to "SafeRouting Crime Data",
            "type" to "vector",
            "tiles" to listOf(TILE_BASE_URL),
            "minzoom" to "10",
            "maxzoom" to "18",
            "attribution" to "SafeRouting, UK Police Data"
        ) as Map<String, String>
    }
}
