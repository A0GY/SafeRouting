package com.universityofreading.demo

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.universityofreading.demo.data.CrimeData
import com.github.mikephil.charting.data.*
import com.universityofreading.demo.charts.CrimeTypesPieChart
import com.universityofreading.demo.charts.CrimeSeverityBarChart
import com.universityofreading.demo.charts.CrimeSeverityLineChart

data class CrimeStatistic(
    val title: String,
    val value: Double,
    val description: String,
    val displayChart: @Composable (context: Context) -> Unit
)

class CrimeStatsViewModel : ViewModel() {
    suspend fun fetchBoroughStats(borough: String) =
        com.universityofreading.demo.data.api.BackendAnalyticsClient.getBoroughStats(borough)

    suspend fun fetchTimeSeries(borough: String?, crimeType: String? = null) =
        com.universityofreading.demo.data.api.BackendAnalyticsClient.getTimeSeries(borough, crimeType)

    // Helper to adapt backend timeseries values to average safely
    private fun List<Int>.averageOrZero(): Double = if (this.isEmpty()) 0.0 else this.average()
}
