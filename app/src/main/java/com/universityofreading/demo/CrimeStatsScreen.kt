package com.universityofreading.demo

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universityofreading.demo.data.api.BackendAnalyticsClient
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.launch

@Composable
fun CrimeStatsScreen(viewModel: CrimeStatsViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedBorough by remember { mutableStateOf<String?>(null) }
    var currentStatIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var boroughStats by remember { mutableStateOf<Map<String, Any>?>(null) }
    
    // Load available boroughs on screen load
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                DebugLogger.logDebug("CrimeStatsScreen", "Loading borough ranking from backend...")
                val boroughs = BackendAnalyticsClient.getBoroughRanking()
                selectedBorough = boroughs.firstOrNull()?.borough
                isLoading = false
            } catch (e: Exception) {
                DebugLogger.logError("CrimeStatsScreen", "Error loading boroughs: ${e.message}", e)
                errorMessage = "Failed to load borough data: ${e.message}"
                isLoading = false
            }
        }
    }
    
    // Load stats when borough changes
    LaunchedEffect(selectedBorough) {
        if (selectedBorough != null) {
            scope.launch {
                try {
                    isLoading = true
                    DebugLogger.logDebug("CrimeStatsScreen", "Loading stats for borough: $selectedBorough")
                    val stats = BackendAnalyticsClient.getBoroughStats(selectedBorough!!)
                    boroughStats = mapOf(
                        "borough" to (stats.borough ?: ""),
                        "totalCrimes" to (stats.totalCrimes ?: 0),
                        "riskScore" to (stats.riskScore ?: 0.0),
                        "categories" to (stats.crimeCategories ?: emptyList()),
                        "timeseries" to (stats.timeSeriesData),
                        "recommendations" to (stats.safetyRecommendations ?: emptyList())
                    )
                    currentStatIndex = 0
                    isLoading = false
                } catch (e: Exception) {
                    DebugLogger.logError("CrimeStatsScreen", "Error loading stats: ${e.message}", e)
                    errorMessage = "Failed to load crime stats: ${e.message}"
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
        // Borough selector
        if (!isLoading && errorMessage == null) {
            OutlinedButton(
                onClick = { /* Open borough list */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedBorough ?: "Select Borough")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (boroughStats != null) {
                // Display selected stats
                @Suppress("UNCHECKED_CAST")
                val categories = (boroughStats?.get("categories") as? List<Any>?) ?: emptyList()
                val totalCrimes = (boroughStats?.get("totalCrimes") as? Int) ?: 0
                val riskScore = (boroughStats?.get("riskScore") as? Double) ?: 0.0
                val recommendations = (boroughStats?.get("recommendations") as? List<String>?) ?: emptyList()

                Text(
                    text = "Borough: $selectedBorough",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Total Crimes: $totalCrimes | Risk Score: %.1f".format(riskScore),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Display safety recommendations
                if (recommendations.isNotEmpty()) {
                    Text(
                        text = "Safety Recommendations:",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    recommendations.forEach { rec ->
                        Text(
                            text = "• $rec",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = { /* Previous */ }, enabled = currentStatIndex > 0) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    }
                    Button(onClick = { /* Next */ }, enabled = currentStatIndex < 2) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }

        // Error state
        if (errorMessage != null) {
            Text(
                text = "Error: $errorMessage",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }

        // Loading state
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
