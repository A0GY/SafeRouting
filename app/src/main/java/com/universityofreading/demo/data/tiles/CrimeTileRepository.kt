package com.universityofreading.demo.data.tiles

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.universityofreading.demo.BuildConfig
import com.universityofreading.demo.data.CrimeData
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException

private const val TAG = "CrimeTileRepository"

data class TileIncidentDto(
    @SerializedName("incident_key") val incidentKey: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val severity: Double,
    @SerializedName("occurred_on") val occurredOn: String,
    @SerializedName("month_bucket") val monthBucket: String,
    val borough: String?
)

data class TileDto(
    @SerializedName("tile_id") val tileId: String,
    val revision: Int,
    val heatmap: List<HeatmapPoint>,
    val clusters: List<ClusterPayload>,
    val incidents: List<TileIncidentDto>
)

data class TileResponseDto(
    val tiles: List<TileDto>,
    @SerializedName("removed_tiles") val removedTiles: List<String>,
    @SerializedName("generated_at") val generatedAt: String,
    @SerializedName("next_refresh_seconds") val nextRefreshSeconds: Int
)

/**
 * Repository that talks to the aggregation service for tile-based crime data.
 */
class CrimeTileRepository(
    private val baseUrl: String = BuildConfig.CRIME_AGGREGATOR_BASE_URL,
    private val client: OkHttpClient = OkHttpClient.Builder().build(),
    private val gson: Gson = Gson()
) {

    suspend fun fetchTiles(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        zoom: Int,
        cached: Map<String, Int>
    ): TileResponseDto = withContext(Dispatchers.IO) {
        val urlBuilder = baseUrl.toHttpUrl().newBuilder()
            .addEncodedPathSegments("tiles/viewport")
            .addQueryParameter("north", north.toString())
            .addQueryParameter("south", south.toString())
            .addQueryParameter("east", east.toString())
            .addQueryParameter("west", west.toString())
            .addQueryParameter("zoom", zoom.toString())

        cached.forEach { (tileId, revision) ->
            urlBuilder.addQueryParameter("cached", "$tileId:$revision")
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()

        val response = execute(request)
        parseResponse(response)
    }

    private fun execute(request: Request): Response {
        return try {
            client.newCall(request).execute()
        } catch (ex: IOException) {
            DebugLogger.logError(TAG, "Network error requesting tiles", ex)
            throw ex
        }
    }

    private fun parseResponse(response: Response): TileResponseDto {
        response.use { resp ->
            if (!resp.isSuccessful) {
                val body = resp.body?.string()
                DebugLogger.logError(TAG, "Tile response error ${resp.code}: $body")
                throw IOException("Tile endpoint returned ${resp.code}")
            }
            val body: ResponseBody = resp.body ?: throw IOException("Missing body")
            body.use { stream ->
                val text = stream.string()
                return gson.fromJson(text, TileResponseDto::class.java)
            }
        }
    }

    fun mapToSnapshots(dto: TileResponseDto): Pair<List<CrimeTileSnapshot>, List<String>> {
        val snapshots = dto.tiles.map { tile ->
            CrimeTileSnapshot(
                tileId = tile.tileId,
                revision = tile.revision,
                heatmapPoints = tile.heatmap,
                clusters = tile.clusters,
                incidents = tile.incidents.map { incident ->
                    CrimeData(
                        latitude = incident.latitude,
                        longitude = incident.longitude,
                        severity = incident.severity,
                        date = incident.occurredOn.take(10),
                        type = incident.category,
                        region = incident.borough ?: "Unknown"
                    )
                }
            )
        }
        return snapshots to dto.removedTiles
    }
}
