package com.universityofreading.demo.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import kotlinx.coroutines.delay
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * A chip-style indicator that shows the current time period and associated risk level.
 * Updates automatically every minute.
 */
@Composable
fun TimeRiskIndicator(
    timePeriod: String,
    riskLevel: String,
    modifier: Modifier = Modifier
) {
    // Determine colors and icon based on risk level
    val (backgroundColor, textColor, icon) = when (riskLevel) {
        "Low" -> Triple(
            Color(0xFF4CAF50).copy(alpha = 0.9f),  // Green background
            Color.White,                           // White text
            Icons.Filled.Check                     // Check icon for safety
        )
        "Medium" -> Triple(
            Color(0xFFFFC107).copy(alpha = 0.9f),  // Yellow/amber background
            Color.Black,                           // Black text for contrast
            Icons.Outlined.Info                    // Info icon for caution
        )
        else -> Triple(
            Color(0xFFF44336).copy(alpha = 0.9f),  // Red background
            Color.White,                           // White text
            Icons.Filled.Warning                   // Warning icon for danger
        )
    }

    // Current time for display
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val currentTime = remember { mutableStateOf(LocalTime.now().format(timeFormatter)) }

    // Update time every minute
    LaunchedEffect(Unit) {
        while(true) {
            currentTime.value = LocalTime.now().format(timeFormatter)
            delay(60000) // Update every minute
        }
    }

    Surface(
        modifier = modifier
            .padding(8.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Risk level: $riskLevel",
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = timePeriod,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$riskLevel risk",
                        color = textColor,
                        fontSize = 12.sp
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "• ${currentTime.value}",
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
} 