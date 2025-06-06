package com.universityofreading.demo.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.universityofreading.demo.R
import com.universityofreading.demo.navigation.AreaAnalysis

/**
 * Draw mode floating action button that toggles between active (purple) and inactive (white) states
 */
@Composable
fun DrawModeButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = if (isActive) Color(0xFF9C27B0) else Color.White,
        contentColor = if (isActive) Color.White else Color(0xFF9C27B0)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_pencil),
            contentDescription = "Draw Area"
        )
    }
}

/**
 * Done button for completing the drawing when in draw mode
 */
@Composable
fun DoneButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = Color(0xFF4CAF50),
        contentColor = Color.White
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_done),
            contentDescription = "Complete Drawing"
        )
    }
}

/**
 * Dialog that displays crime analysis for a user-defined area
 */
@Composable
fun CustomAreaAnalysisDialog(
    analysis: AreaAnalysis,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Title
                Text(
                    text = "Area Crime Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summary statistics - now the primary focus
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    StatisticItem(
                        value = analysis.crimeCount.toString(),
                        label = "Total Crimes",
                        modifier = Modifier
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Crime type breakdown
                Text(
                    text = "Crime Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (analysis.crimeTypeCounts.isEmpty()) {
                    Text(
                        text = "No crimes found in this area",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    // Sort crime types by count (descending)
                    analysis.crimeTypeCounts
                        .entries
                        .sortedByDescending { it.value }
                        .forEach { (type, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatCrimeType(type),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

/**
 * Helper component for displaying a statistic with a label
 */
@Composable
private fun StatisticItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Helper function to format crime type strings for better display
 */
private fun formatCrimeType(type: String): String {
    return type
        .replace("-", " ")
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
} 