package com.universityofreading.demo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MenuScreen(navController: NavController) {
    // Create a more visually appealing gradient background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF5F2C82),  // Deep purple
            Color(0xFF1A1A3A)   // Dark blue/purple
        )
    )

    // Animation states for entrance animations
    val animationState = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo and title with animation
            AnimatedVisibility(
                visibleState = animationState,
                enter = fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + 
                       slideInVertically(
                           initialOffsetY = { -100 },
                           animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                       )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App logo (replace R.drawable.app_logo with your actual logo resource)
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // This is a placeholder - replace with your actual logo
                        Icon(
                            imageVector = Icons.Filled.Map,
                            contentDescription = "App Logo",
                            modifier = Modifier.size(60.dp),
                            tint = Color(0xFF5F2C82)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Safe Travel App",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Navigate your world safely",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Menu options with cards and animations
            val staggerDelay = 100 // stagger delay in ms
            
            // Map Screen Navigation
            AnimatedVisibility(
                visibleState = animationState,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                MenuCard(
                    icon = Icons.Filled.Map,
                    title = "Open Map",
                    description = "View safety map and plan routes",
                    onClick = { navController.navigate(Routes.MAP_SCREEN) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Crime Statistics
            AnimatedVisibility(
                visibleState = animationState,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                MenuCard(
                    icon = Icons.Filled.BarChart,
                    title = "View Crime Statistics",
                    description = "See detailed crime data analysis",
                    onClick = { navController.navigate(Routes.CRIME_STATS_SCREEN) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Compare Regions
            AnimatedVisibility(
                visibleState = animationState,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                MenuCard(
                    icon = Icons.Filled.Compare,
                    title = "Compare Regions",
                    description = "Compare safety across different areas",
                    onClick = { navController.navigate(Routes.CRIME_COMPARE_SCREEN) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with gradient background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF5F2C82),
                                Color(0xFF4A00E0)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}
