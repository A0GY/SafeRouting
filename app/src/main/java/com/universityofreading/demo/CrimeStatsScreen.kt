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
import com.universityofreading.demo.utils.loadCrimeData
import com.universityofreading.demo.utils.loadRegions

@Composable
fun CrimeStatsScreen(viewModel: CrimeStatsViewModel = viewModel()) {
    val context = LocalContext.current
    val regions = remember { loadRegions(context) }
    var selectedRegion by remember { mutableStateOf(regions.firstOrNull()) }
    var currentStatIndex by remember { mutableIntStateOf(0) }

    val allCrimeData = remember { loadCrimeData(context) }

    // Filter by region
    val filteredCrimeData = remember(selectedRegion) {
        allCrimeData.filter { it.region == selectedRegion }
    }

    val statistics = remember(filteredCrimeData) {
        viewModel.computeStatistics(selectedRegion, filteredCrimeData)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        RegionDropdown(
            regions = regions,
            selectedRegion = selectedRegion,
            onRegionSelected = { newRegion ->
                selectedRegion = newRegion
                currentStatIndex = 0
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (statistics.isNotEmpty()) {
            val stat = statistics[currentStatIndex]

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stat.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (stat.title.contains("Trend")) 400.dp else 300.dp)
                        .padding(if (stat.title.contains("Trend")) 2.dp else 8.dp)
                ) {
                    stat.displayChart(context)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stat.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(
                        horizontal = if (stat.title.contains("Trend")) 8.dp else 16.dp,
                        vertical = 4.dp
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStatIndex > 0) {
                        IconButton(onClick = { currentStatIndex-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Text(
                        text = "${currentStatIndex + 1}/${statistics.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    if (currentStatIndex < statistics.size - 1) {
                        IconButton(onClick = { currentStatIndex++ }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No crime data available for ${selectedRegion ?: "selected region"}.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
