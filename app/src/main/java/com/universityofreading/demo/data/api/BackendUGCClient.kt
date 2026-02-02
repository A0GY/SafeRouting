package com.universityofreading.demo.data.api

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.GsonBuilder
import com.universityofreading.demo.BuildConfig
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Body
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Request/response models for UGC and moderation
 */
data class UGCIncidentReport(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,       // "crime", "hazard", "suspicious-activity"
    val severity: String,       // "low", "medium", "high"
    val description: String,
    val photoUrl: String?,      // Server-stored redacted photo URL
    val userId: String,
    val timestamp: Long,
    val status: String,         // "pending", "approved", "rejected"
    val moderationNotes: String? = null,
    val dataVersion: String
)

data class UGCSubmissionRequest(
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val severity: String,
    val description: String,
    val redactedPhotoHash: String? = null  // Hash of redacted photo for dedup
)

data class ModerationQueueItem(
    val reportId: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val severity: String,
    val description: String,
    val timestamp: Long,
    val status: String,
    val verificationCount: Int,  // Number of users corroborating
    val photoHash: String?
)

/**
 * Retrofit service interface for UGC and moderation
 */
interface BackendUGCService {
    @Multipart
    @POST("api/ugc/report")
    suspend fun submitReport(
        @Part("latitude") latitude: Double,
        @Part("longitude") longitude: Double,
        @Part("category") category: String,
        @Part("severity") severity: String,
        @Part("description") description: String,
        @Part photo: MultipartBody.Part?
    ): UGCIncidentReport

    @POST("api/ugc/verify")
    suspend fun verifyReport(
        @Query("reportId") reportId: String
    ): UGCIncidentReport

    @GET("api/moderation/queue")
    suspend fun getModerationQueue(
        @Query("limit") limit: Int = 50,
        @Query("status") status: String? = null
    ): List<ModerationQueueItem>

    @POST("api/moderation/approve")
    suspend fun approveReport(
        @Query("reportId") reportId: String,
        @Body notes: Map<String, String>? = null
    ): UGCIncidentReport

    @POST("api/moderation/reject")
    suspend fun rejectReport(
        @Query("reportId") reportId: String,
        @Body notes: Map<String, String>? = null
    ): UGCIncidentReport
}

/**
 * Client for User-Generated Content (UGC) submission and moderation
 * Handles client-side redaction and server-side approval workflow
 */
object BackendUGCClient {
    private const val TAG = "BackendUGCClient"
    
    // Get base URL from BuildConfig (configured in build.gradle.kts)
    private val BASE_URL: String
        get() = BuildConfig.API_BASE_URL

    private val retrofit by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val gson = GsonBuilder().setLenient().create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private val apiService: BackendUGCService by lazy {
        retrofit.create(BackendUGCService::class.java)
    }

    /**
     * Submit a UGC incident report with optional redacted photo
     * Performs client-side redaction before submission
     */
    suspend fun submitReport(
        latitude: Double,
        longitude: Double,
        category: String,        // "crime", "hazard", "suspicious-activity"
        severity: String,        // "low", "medium", "high"
        description: String,
        redactedPhotoBitmap: Bitmap? = null
    ): UGCIncidentReport? {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Submitting UGC report: $category at ($latitude, $longitude)")

                var photoPart: MultipartBody.Part? = null
                if (redactedPhotoBitmap != null) {
                    photoPart = createImagePart(redactedPhotoBitmap)
                }

                val report = apiService.submitReport(
                    latitude = latitude,
                    longitude = longitude,
                    category = category,
                    severity = severity,
                    description = description,
                    photo = photoPart
                )

                DebugLogger.logDebug(TAG, "UGC report submitted with ID: ${report.id}, status: ${report.status}")
                report
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error submitting UGC report: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Verify/corroborate an existing report (user confirms the incident)
     */
    suspend fun verifyReport(reportId: String): UGCIncidentReport? {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Verifying report: $reportId")

                val report = apiService.verifyReport(reportId)
                DebugLogger.logDebug(TAG, "Report verified, verification count increased")
                report
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error verifying report: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Get moderation queue (moderator-only endpoint)
     */
    suspend fun getModerationQueue(limit: Int = 50, status: String? = null): List<ModerationQueueItem> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Fetching moderation queue")

                apiService.getModerationQueue(limit = limit, status = status)
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error fetching moderation queue: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Approve a reported incident (moderator-only)
     */
    suspend fun approveReport(reportId: String, notes: String? = null): UGCIncidentReport? {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Approving report: $reportId")

                val notesMap = notes?.let { mapOf("moderationNotes" to it) }
                val report = apiService.approveReport(reportId, notesMap)
                DebugLogger.logDebug(TAG, "Report approved")
                report
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error approving report: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Reject a reported incident (moderator-only)
     */
    suspend fun rejectReport(reportId: String, notes: String? = null): UGCIncidentReport? {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.logDebug(TAG, "Rejecting report: $reportId")

                val notesMap = notes?.let { mapOf("moderationNotes" to it) }
                val report = apiService.rejectReport(reportId, notesMap)
                DebugLogger.logDebug(TAG, "Report rejected")
                report
            } catch (e: Exception) {
                DebugLogger.logError(TAG, "Error rejecting report: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Convert Bitmap to redacted image for submission
     * This is a client-side helper; actual redaction logic should be in UI layer
     */
    private fun createImagePart(bitmap: Bitmap): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val imageBytes = stream.toByteArray()

        val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData("photo", "report.jpg", requestBody)
    }

    /**
     * Compute hash of redacted photo for deduplication
     */
    fun computePhotoHash(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val imageBytes = stream.toByteArray()
        return imageBytes.hashCode().toString()
    }
}
