package com.armadio.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.armadio.feature.garments.InventoryRoute

object Routes {
    const val INVENTORY = "inventory"
    // F1: garment detail ("garment/{id}"), categories, settings…
}

@Composable
fun ArmarioApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.INVENTORY) {
        composable(Routes.INVENTORY) { InventoryRoute() }
    }
}
