package com.guardiantrack.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Telemetry", Icons.Default.Dashboard)
    data object Geofences : Screen("geofences", "Geofences", Icons.Default.LocationOn)
    data object OemGuide : Screen("oem_guide", "OEM Guide", Icons.Default.BatteryAlert)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
