package com.universityofreading.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.universityofreading.demo.data.api.BackendAnalyticsClient
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.launch

/**
 * Enum for date filter options for the compare screen
 */
enum class CompareDateFilterOption {
    LAST_7_DAYS,
    LAST_30_DAYS,
    ALL
}

/**
 * REFACTORED: Shows a UI to pick two boroughs and compares their crime statistics
 * using the backend analytics service instead of local JSON data
 */
@Composable
fun CrimeCompareScreen() {
    val scope = rememberCoroutineScope()
    
    // Two borough picks
    var selectedBoroughA by remember { mutableStateOf<String?>(null) }
    var selectedBoroughB by remember { mutableStateOf<String?>(null) }
    
    // Date filter
    var selectedFilter by remember { mutableStateOf(CompareDateFilterOption.ALL) }
    
    // Comparison state
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var availableBoroughs by remember { mutableStateOf<List<String>>(emptyList()) }
    var comparisonData by remember { mutableStateOf<Map<String, Any>?>(null) }
    
    // Load available boroughs on mount
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                DebugLogger.logDebug("CrimeCompareScreen", "Loading available boroughs...")
                val boroughs = BackendAnalyticsClient.getBoroughRanking()
                availableBoroughs = boroughs.map { it.borough }
                selectedBoroughA = availableBoroughs.getOrNull(0)
                selectedBoroughB = availableBoroughs.getOrNull(1)
                isLoading = false
            } catch (e: Exception) {
                DebugLogger.logError("CrimeCompareScreen", "Error loading boroughs: ${e.message}", e)
                errorMessage = "Failed to load boroughs: ${e.message}"
                isLoading = false
            }
        }
    }
    
    // Load comparison when boroughs change
    LaunchedEffect(selectedBoroughA, selectedBoroughB) {
        if (selectedBoroughA != null && selectedBoroughB != null) {
            scope.launch {
                try {
                    isLoading = true
                    DebugLogger.logDebug(
                        "CrimeCompareScreen",
                        "Comparing $selectedBoroughA vs $selectedBoroughB"
                    )
                    val comparison = BackendAnalyticsClient.compareBorough(
                        selectedBoroughA!!,
                        selectedBoroughB!!
                    )
                    comparisonData = mapOf(
                        "borough1" to (comparison.borough1 ?: ""),
                        "borough2" to (comparison.borough2 ?: ""),
                        "crimes1" to (comparison.borough1Crimes ?: 0),
                        "crimes2" to (comparison.borough2Crimes ?: 0),
                        "risk1" to (comparison.borough1Risk ?: 0.0),
                        "risk2" to (comparison.borough2Risk ?: 0.0),
                        "percent1" to (comparison.percentage1 ?: 0.0),
                        "percent2" to (comparison.percentage2 ?: 0.0)
                    )
                    isLoading = false
                } catch (e: Exception) {
                    DebugLogger.logError("CrimeCompareScreen", "Error comparing boroughs: ${e.message}", e)
                    errorMessage = "Failed to compare boroughs: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Compare Borough Crime Statistics",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Borough selectors
        if (!isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Open borough list */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(selectedBoroughA ?: "Select Borough A")
                }
                
                OutlinedButton(
                    onClick = { /* Open borough list */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(selectedBoroughB ?: "Select Borough B")
                }
            }
        }

        // Display comparison
        if (comparisonData != null && !isLoading) {
            @Suppress("UNCHECKED_CAST")
            val data = comparisonData as Map<String, Any>
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${data["borough1"]} vs ${data["borough2"]}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Crimes comparison
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total Crimes",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${data["crimes1"]}",
                                style = MaterialTheme.typography.displaySmall
                            )
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total Crimes",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${data["crimes2"]}",
                                style = MaterialTheme.typography.displaySmall
                            )
                        }
                    }

                    // Risk score comparison
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Risk Score",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "%.1f".format(data["risk1"]),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Risk Score",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "%.1f".format(data["risk2"]),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
            }
        }

        // Loading state
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Error state
        if (errorMessage != null) {
            Text(
                text = "Error: $errorMessage",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
