package com.universityofreading.demo.navigation

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlin.math.*

object PolylineUtils {
    private const val TAG = "PolylineUtils"

    fun decode(encoded: String): List<LatLng> {
        try {
            val result = PolyUtil.decode(encoded)
            Log.d(TAG, "Decoded polyline with ${result.size} points")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding polyline", e)
            return emptyList()
        }
    }

    fun sampleEvery(path: List<LatLng>, stepM: Double): List<LatLng> {
        if (path.size < 2) {
            Log.d(TAG, "Path too short to sample, returning as-is")
            return path
        }
        
        val out = mutableListOf(path.first())
        var acc = 0.0
        for (i in 1 until path.size) {
            val d = distance(path[i - 1], path[i])
            acc += d
            if (acc >= stepM) {
                out += path[i]; acc = 0.0
            }
        }
        
        Log.d(TAG, "Sampled path: ${path.size} points -> ${out.size} points with step=$stepM meters")
        return out
    }
    
    private fun distance(a: LatLng, b: LatLng): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat/2).pow(2) + cos(lat1)*cos(lat2)*sin(dLon/2).pow(2)
        return 2 * R * atan2(sqrt(h), sqrt(1-h))
    }
}
