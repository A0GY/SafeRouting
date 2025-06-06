package com.universityofreading.demo

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Define screen routes
object Routes {
    const val MENU_SCREEN = "menu_screen"
    const val MAP_SCREEN = "map_screen"
    const val CRIME_STATS_SCREEN = "crime_stats_screen"

    // NEW ROUTE FOR REGION COMPARISON
    const val CRIME_COMPARE_SCREEN = "crime_compare_screen"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MENU_SCREEN
    ) {
        // 1) Menu/Home screen
        composable(Routes.MENU_SCREEN) {
            MenuScreen(navController)
        }

        // 2) Map screen - Using ProgressiveMapScreen with error handling
        composable(Routes.MAP_SCREEN) {
            ProgressiveMapScreen()
        }

        // 3) Crime Statistics screen
        composable(Routes.CRIME_STATS_SCREEN) {
            CrimeStatsScreen()
        }

        // 4) Crime Compare screen
        composable(Routes.CRIME_COMPARE_SCREEN) {
            CrimeCompareScreen()
        }
    }
}
