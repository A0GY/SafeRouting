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
    fun computeStatistics(region: String?, crimeData: List<CrimeData>): List<CrimeStatistic> {
        if (region == null || crimeData.isEmpty()) return emptyList()

        val crimesByType = crimeData.groupingBy { it.type }.eachCount()
        val mostCommonType = crimesByType.maxByOrNull { it.value }
        val averageSeverity = crimeData.map { it.severity }.average()

        val pieEntries = crimesByType.map { (type, count) ->
            PieEntry(count.toFloat(), type)
        }

        val highSeverity = crimeData.count { it.severity >= 7.0 }
        val mediumSeverity = crimeData.count { it.severity in 4.0..6.9999 }
        val lowSeverity = crimeData.count { it.severity < 4.0 }

        val barEntries = listOf(
            BarEntry(0f, highSeverity.toFloat()),
            BarEntry(1f, mediumSeverity.toFloat()),
            BarEntry(2f, lowSeverity.toFloat())
        )
        val barLabels = listOf("High", "Medium", "Low")

        val sortedByDate = crimeData.sortedBy { it.date }
        
        // Sample points to make chart less dense
        val sampledCrimeData = if (sortedByDate.size > 30) {
            // If we have a lot of data, take fewer samples
            val samplingRate = (sortedByDate.size / 20).coerceAtLeast(1)
            sortedByDate.filterIndexed { index, _ -> index % samplingRate == 0 }
        } else {
            sortedByDate
        }
        
        val lineEntries = sampledCrimeData.mapIndexed { index, crime ->
            Entry(index.toFloat(), crime.severity.toFloat())
        }
        val dateLabels = sampledCrimeData.map { it.date }

        return listOf(
            CrimeStatistic(
                title = "Crime Types in $region",
                value = crimesByType.size.toDouble(),
                description = buildString {
                    append("Most common crime: ${mostCommonType?.key} (${mostCommonType?.value} incidents)\n")
                    append("Types breakdown:\n")
                    crimesByType.forEach { (type, count) ->
                        append("$type: $count\n")
                    }
                },
                displayChart = { ctx -> CrimeTypesPieChart(ctx, pieEntries) }
            ),
            CrimeStatistic(
                title = "Crime Severity Distribution in $region",
                value = averageSeverity,
                description = buildString {
                    append("High severity (7-10): $highSeverity\n")
                    append("Medium severity (4-7): $mediumSeverity\n")
                    append("Low severity (0-4): $lowSeverity")
                },
                displayChart = { ctx -> CrimeSeverityBarChart(ctx, barEntries, barLabels) }
            ),
            CrimeStatistic(
                title = "Crime Severity Trend in $region",
                value = averageSeverity,
                description = "Crime severity trend over time, showing how severity changes across incidents.",
                displayChart = { ctx -> CrimeSeverityLineChart(ctx, lineEntries, dateLabels) }
            )
        )
    }
}
