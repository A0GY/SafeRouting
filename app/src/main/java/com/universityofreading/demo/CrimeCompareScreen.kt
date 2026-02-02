package com.universityofreading.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.universityofreading.demo.data.CrimeData
import com.universityofreading.demo.data.api.BackendAnalyticsClient
import com.universityofreading.demo.charts.CrimeSeverityBarChart
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
import com.github.mikephil.charting.data.BarEntry

/**
 * Enum for date filter options for the compare screen
 */
enum class CompareDateFilterOption {
    LAST_7_DAYS,
    LAST_30_DAYS,
    ALL
}

/**
 * Shows a UI to pick two regions and a date filter, then compares their total crimes
 * and average severity side-by-side in bar charts.
 */
@Composable
fun CrimeCompareScreen() {
    val context = LocalContext.current

    // Load region list from backend
    var allRegions by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        try {
            val boroughs = BackendAnalyticsClient.getBoroughRanking()
            allRegions = boroughs.map { it.borough }
        } catch (e: Exception) {
            DebugLogger.logError("CrimeCompareScreen", "Failed to load regions: ${e.message}", e)
            allRegions = emptyList()
        }
    }

    // Two region picks
    var selectedRegionA by remember { mutableStateOf<String?>(null) }
    var selectedRegionB by remember { mutableStateOf<String?>(null) }

    // Date filter
    var selectedFilter by remember { mutableStateOf(CompareDateFilterOption.ALL) }

    // For per-region details, we will ask the analytics backend when a region is selected
    var regionAStats by remember { mutableStateOf<RegionStats?>(null) }
    var regionBStats by remember { mutableStateOf<RegionStats?>(null) }

    LaunchedEffect(selectedRegionA, selectedFilter) {
        if (selectedRegionA != null) {
            val stats = BackendAnalyticsClient.getBoroughStats(selectedRegionA!!)
            if (stats != null) {
                val avg = if (stats.timeSeriesData.values.isEmpty()) 0.0 else stats.timeSeriesData.values.average()
                regionAStats = RegionStats(stats.totalCrimes, avg)
            }
        }
    }

    LaunchedEffect(selectedRegionB, selectedFilter) {
        if (selectedRegionB != null) {
            val stats = BackendAnalyticsClient.getBoroughStats(selectedRegionB!!)
            if (stats != null) {
                val avg = if (stats.timeSeriesData.values.isEmpty()) 0.0 else stats.timeSeriesData.values.average()
                regionBStats = RegionStats(stats.totalCrimes, avg)
            }
        }
    }

    val statsA = regionAStats ?: RegionStats(0, 0.0)
    val statsB = regionBStats ?: RegionStats(0, 0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Compare Two Regions",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Region selection
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                RegionDropdown(
                    regions = allRegions,
                    selectedRegion = selectedRegionA,
                    onRegionSelected = { newReg -> selectedRegionA = newReg }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                RegionDropdown(
                    regions = allRegions,
                    selectedRegion = selectedRegionB,
                    onRegionSelected = { newReg -> selectedRegionB = newReg }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date filter
        CompareDateFilterRow(
            selectedOption = selectedFilter,
            onOptionSelected = { newFilter -> selectedFilter = newFilter }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Show summary info
        if (selectedRegionA != null && selectedRegionB != null) {
            Text(
                text = "Time Period: ${selectedFilter.name}\n\n" +
                        "Region A: $selectedRegionA\n" +
                        " - Total Crimes: ${statsA.totalCrimes}\n" +
                        " - Avg Severity: ${"%.2f".format(statsA.averageSeverity)}\n\n" +
                        "Region B: $selectedRegionB\n" +
                        " - Total Crimes: ${statsB.totalCrimes}\n" +
                        " - Avg Severity: ${"%.2f".format(statsB.averageSeverity)}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Compare with bar charts
        if (selectedRegionA != null && selectedRegionB != null) {
            //  Bar chart for total crimes
            val totalCrimesEntries = listOf(
                BarEntry(0f, statsA.totalCrimes.toFloat()),
                BarEntry(1f, statsB.totalCrimes.toFloat())
            )
            val totalCrimesLabels = listOf(selectedRegionA!!, selectedRegionB!!)

            Text(
                text = "Total Crimes Comparison",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                CrimeSeverityBarChart(context, totalCrimesEntries, totalCrimesLabels)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2) Bar chart for average severity
            val avgSeverityEntries = listOf(
                BarEntry(0f, statsA.averageSeverity.toFloat()),
                BarEntry(1f, statsB.averageSeverity.toFloat())
            )
            val avgSeverityLabels = listOf(selectedRegionA!!, selectedRegionB!!)

            Text(
                text = "Average Severity Comparison",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                CrimeSeverityBarChart(context, avgSeverityEntries, avgSeverityLabels)
            }
        } else {
            // Just text, no icon
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Please select both regions to compare.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Row of radio buttons for the compare date filter
 */
@Composable
fun CompareDateFilterRow(
    selectedOption: CompareDateFilterOption,
    onOptionSelected: (CompareDateFilterOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Last 7 days
        FilterRadioButtonCompare(
            text = "Last 7 days",
            option = CompareDateFilterOption.LAST_7_DAYS,
            selectedOption = selectedOption,
            onOptionSelected = onOptionSelected
        )
        Spacer(modifier = Modifier.width(16.dp))

        // Last 30 days
        FilterRadioButtonCompare(
            text = "Last 30 days",
            option = CompareDateFilterOption.LAST_30_DAYS,
            selectedOption = selectedOption,
            onOptionSelected = onOptionSelected
        )
        Spacer(modifier = Modifier.width(16.dp))

        // All
        FilterRadioButtonCompare(
            text = "All",
            option = CompareDateFilterOption.ALL,
            selectedOption = selectedOption,
            onOptionSelected = onOptionSelected
        )
    }
}

@Composable
fun FilterRadioButtonCompare(
    text: String,
    option: CompareDateFilterOption,
    selectedOption: CompareDateFilterOption,
    onOptionSelected: (CompareDateFilterOption) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .selectable(
                selected = (option == selectedOption),
                onClick = { onOptionSelected(option) }
            )
    ) {
        RadioButton(
            selected = (option == selectedOption),
            onClick = { onOptionSelected(option) }
        )
        Text(text = text, modifier = Modifier.padding(start = 4.dp))
    }
}

/**
 * Filter crimes by date range, similar to MapScreen logic.
 */
private fun filterCrimesByDate(
    crimes: List<CrimeData>,
    filterOption: CompareDateFilterOption
): List<CrimeData> {
    if (filterOption == CompareDateFilterOption.ALL) {
        return crimes
    }

    val now = LocalDate.now()
    return crimes.filter { crime ->
        val crimeDate = parseCrimeDate(crime.date) ?: return@filter false
        when (filterOption) {
            CompareDateFilterOption.LAST_7_DAYS ->
                !crimeDate.isBefore(now.minusDays(7))
            CompareDateFilterOption.LAST_30_DAYS ->
                !crimeDate.isBefore(now.minusDays(30))
            CompareDateFilterOption.ALL ->
                true
        }
    }
}

private fun parseCrimeDate(dateStr: String): LocalDate? {
    return try {
        LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: DateTimeParseException) {
        null
    }
}

/**
 * Compute summary stats for a region.
 */
data class RegionStats(
    val totalCrimes: Int,
    val averageSeverity: Double
)

private fun computeRegionStats(crimes: List<CrimeData>): RegionStats {
    if (crimes.isEmpty()) return RegionStats(0, 0.0)
    val totalCrimes = crimes.size
    val avgSeverity = crimes.map { it.severity }.average()
    return RegionStats(totalCrimes, avgSeverity)
}
