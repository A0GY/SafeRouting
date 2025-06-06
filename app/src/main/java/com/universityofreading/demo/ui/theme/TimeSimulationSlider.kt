package com.universityofreading.demo.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.outlined.Refresh
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * A component that allows users to simulate different times of day
 * to see how safety risks change throughout the day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSimulationSlider(
    simulatedHour: Int,
    onHourChange: (Int) -> Unit,
    isUsingCurrentTime: Boolean,
    onUseCurrentTime: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTime = LocalTime.of(simulatedHour, 0)
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Time Simulation",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Current time display
        Text(
            text = "Time: ${currentTime.format(formatter)}",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Hour slider
        Slider(
            value = simulatedHour.toFloat(),
            onValueChange = { onHourChange(it.toInt()) },
            valueRange = 0f..23f,
            steps = 23,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "00:00")
            Text(text = "12:00")
            Text(text = "23:00")
        }
        
        // Use current time switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Use current time")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isUsingCurrentTime,
                onCheckedChange = onUseCurrentTime
            )
        }
    }
}

/**
 * A compact version of the time simulation controls for the top bar
 */
@Composable
fun CompactTimeControls(
    simulatedHour: Int,
    isUsingCurrentTime: Boolean,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeString = String.format("%02d:00", simulatedHour)
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Show clock icon
        Icon(
            imageVector = Icons.Filled.AccessTime,
            contentDescription = "Time Controls",
            tint = if (isUsingCurrentTime) MaterialTheme.colorScheme.primary else Color(0xFFF57C00),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(Modifier.width(4.dp))
        
        // Show the current time
        Text(
            text = timeString,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (isUsingCurrentTime) MaterialTheme.colorScheme.primary else Color(0xFFF57C00)
        )
    }
} 