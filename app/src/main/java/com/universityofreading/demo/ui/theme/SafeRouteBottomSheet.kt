package com.universityofreading.demo.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import org.threeten.bp.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeRouteBottomSheet(
    distanceKm: Double,
    riskScore: Double,
    highRiskSegments: Int = 0,
    isSafestMode: Boolean,
    onSafestModeChanged: (Boolean) -> Unit,
    onStartNavigationClick: () -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Route details
            RouteDetails(distanceKm, riskScore, highRiskSegments)
            
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            
            // Route preference toggle
            RoutePreferenceToggle(isSafestMode, onSafestModeChanged)
            
            Spacer(Modifier.height(24.dp))
            
            // Start navigation button
            Button(
                onClick = onStartNavigationClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Filled.Navigation,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Start Navigation")
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RouteDetails(distanceKm: Double, risk: Double, highRiskSegments: Int = 0) {
    // REVISED risk thresholds for the new 0-100 scale
    val riskLevel = when {
        risk < 20 -> "Low"
        risk < 50 -> "Medium"
        else -> "High"
    }
    
    val riskColor = when {
        risk < 20 -> Color(0xFF4CAF50) // Green
        risk < 50 -> Color(0xFFFFC107) // Yellow
        else -> Color(0xFFF44336)      // Red
    }
    
    // Calculate estimated walking time based on distance (avg walking speed 5km/h)
    val estimatedMinutes = (distanceKm / 5.0 * 60).toInt()
    val timeString = if (estimatedMinutes < 60) {
        "$estimatedMinutes min"
    } else {
        val hours = estimatedMinutes / 60
        val mins = estimatedMinutes % 60
        "$hours h $mins min"
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Route Summary",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Distance info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${"%.2f".format(distanceKm)} km",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Distance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Time info (added)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Est. Time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Risk info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = riskLevel,
                    style = MaterialTheme.typography.headlineSmall,
                    color = riskColor
                )
                Text(
                    text = "Risk Level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Additional risk information
        Spacer(Modifier.height(12.dp))
        
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = riskColor.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        risk < 5 -> "This route passes through areas with very little to no reported crime"
                        risk < 20 -> "This route generally avoids areas with significant crime reports"
                        risk < 50 -> "This route passes through some areas with moderate crime activity"
                        risk < 70 -> "This route includes areas with significant crime history"
                        else -> "This route passes through high-crime areas - exercise caution"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = riskColor
                )
                
                // Display high-risk segment info if there are any
                if (highRiskSegments > 0) {
                    Spacer(Modifier.height(6.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = riskColor,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(Modifier.width(4.dp))
                        
                        Text(
                            text = "Contains $highRiskSegments high-risk ${if (highRiskSegments == 1) "area" else "areas"}",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = riskColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutePreferenceToggle(isSafestMode: Boolean, onSafestModeChanged: (Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Route Preference",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fastest",
                style = MaterialTheme.typography.bodyLarge,
                color = if (!isSafestMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            
            Switch(
                checked = isSafestMode,
                onCheckedChange = onSafestModeChanged,
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.secondaryContainer,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            
            Text(
                text = "Safest",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSafestMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Enhanced description based on selected mode - more specific details
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSafestMode) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isSafestMode) {
                        "Safety First Route"
                    } else {
                        "Time-Optimized Route"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSafestMode) Color(0xFF2E7D32) else Color(0xFF1565C0)
                )
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    text = if (isSafestMode) {
                        "This route avoids areas with high crime rates, even if it means a slightly longer journey. Look for the red markers indicating high-risk areas that couldn't be avoided."
                    } else {
                        "This route prioritizes getting you to your destination as quickly as possible. It may pass through higher-risk areas to save time. Blue dotted line indicates the fastest path."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = if (isSafestMode) Color(0xFF2E7D32) else Color(0xFF1565C0)
                )
            }
        }
    }
}
