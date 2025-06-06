package com.universityofreading.demo.util

import android.util.Log

/**
 * Helper class for easily identifiable debug logs
 */
object DebugLogger {
    private const val DEBUG_MARKER = "🔍 ROUTE-DEBUG 🔍"
    private const val CLICK_MARKER = "👆 CLICK 👆"
    private const val ERROR_MARKER = "⚠️ ERROR ⚠️"
    
    /**
     * Log a click event with a special marker
     */
    fun logClick(tag: String, message: String) {
        Log.d(tag, "$CLICK_MARKER $message")
    }
    
    /**
     * Log a debug message with a special marker
     */
    fun logDebug(tag: String, message: String) {
        Log.d(tag, "$DEBUG_MARKER $message")
    }
    
    /**
     * Log an error with a special marker
     */
    fun logError(tag: String, message: String, e: Exception? = null) {
        if (e != null) {
            Log.e(tag, "$ERROR_MARKER $message", e)
        } else {
            Log.e(tag, "$ERROR_MARKER $message")
        }
    }
} 