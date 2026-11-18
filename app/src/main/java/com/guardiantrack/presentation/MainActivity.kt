package com.guardiantrack.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.guardiantrack.presentation.dashboard.DashboardScreen
import com.guardiantrack.presentation.dashboard.DashboardUiEvent
import com.guardiantrack.presentation.dashboard.DashboardViewModel
import com.guardiantrack.presentation.geofence.GeofenceScreen
import com.guardiantrack.presentation.geofence.GeofenceUiEvent
import com.guardiantrack.presentation.geofence.GeofenceViewModel
import com.guardiantrack.presentation.navigation.Screen
import com.guardiantrack.presentation.oem.OemWizardScreen
import com.guardiantrack.presentation.oem.OemWizardUiEvent
import com.guardiantrack.presentation.oem.OemWizardViewModel
import com.guardiantrack.presentation.settings.SettingsScreen
import com.guardiantrack.presentation.settings.SettingsUiEvent
import com.guardiantrack.presentation.settings.SettingsViewModel
import com.guardiantrack.presentation.theme.GuardianTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var showBackgroundRationale by mutableStateOf(false)

    private val backgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        showBackgroundRationale = false
    }

    private val foregroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkBackgroundLocationPermission()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestForegroundPermissions()

        setContent {
            GuardianTrackTheme {
                MainAppShell()

                if (showBackgroundRationale) {
                    BackgroundLocationRationaleDialog(
                        onConfirm = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                            showBackgroundRationale = false
                        },
                        onDismiss = {
                            showBackgroundRationale = false
                        }
                    )
                }
            }
        }
    }

    private fun requestForegroundPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            foregroundPermissionLauncher.launch(ungranted.toTypedArray())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkBackgroundLocationPermission()
        }
    }

    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBg = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasBg) {
                showBackgroundRationale = true
            }
        }
    }
}

@Composable
fun BackgroundLocationRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Background Location & Safety Disclosure") },
        text = {
            Text(
                "GuardianTrack collects background location data to enable 24/7 geofence perimeter alerts, real-time breadcrumb tracking, and lone-worker safety even when the app is closed or not in use.\n\nTo ensure alarms function properly, please select 'Allow all the time' in the next system prompt."
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}

@Composable
fun MainAppShell() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val screens = listOf(Screen.Dashboard, Screen.Geofences, Screen.OemGuide, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(state.userMessage) {
                    state.userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.onEvent(DashboardUiEvent.DismissMessage)
                    }
                }

                DashboardScreen(state = state, onEvent = viewModel::onEvent)
            }

            composable(Screen.Geofences.route) {
                val viewModel: GeofenceViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(state.userMessage) {
                    state.userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.onEvent(GeofenceUiEvent.DismissMessage)
                    }
                }

                GeofenceScreen(state = state, onEvent = viewModel::onEvent)
            }

            composable(Screen.OemGuide.route) {
                val viewModel: OemWizardViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(state.userMessage) {
                    state.userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.onEvent(OemWizardUiEvent.DismissMessage)
                    }
                }

                OemWizardScreen(state = state, onEvent = viewModel::onEvent)
            }

            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(state.userMessage) {
                    state.userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.onEvent(SettingsUiEvent.DismissMessage)
                    }
                }

                SettingsScreen(state = state, onEvent = viewModel::onEvent)
            }
        }
    }
}
