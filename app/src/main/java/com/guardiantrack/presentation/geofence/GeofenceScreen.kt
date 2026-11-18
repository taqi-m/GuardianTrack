package com.guardiantrack.presentation.geofence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardiantrack.core.model.geofence.GeofenceEventRecord
import com.guardiantrack.core.model.geofence.GeofenceZone
import com.guardiantrack.presentation.theme.AccentRed
import com.guardiantrack.presentation.theme.PrimaryGreen
import com.guardiantrack.presentation.theme.SecondaryTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GeofenceScreen(
    state: GeofenceUiState,
    onEvent: (GeofenceUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(GeofenceUiEvent.OpenAddDialog) },
                containerColor = PrimaryGreen
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Geofence")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Active Geofence Zones (${state.geofences.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.geofences.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Geofences Configured",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the '+' button to register a new circular perimeter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.geofences, key = { it.id }) { zone ->
                    GeofenceItemCard(
                        zone = zone,
                        onDelete = { onEvent(GeofenceUiEvent.DeleteGeofence(zone.id)) },
                        onToggle = { active -> onEvent(GeofenceUiEvent.ToggleActive(zone.id, active)) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Breach Logs (${state.recentBreachEvents.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.recentBreachEvents.isEmpty()) {
                item {
                    Text(
                        text = "No transition events recorded yet. Cross a registered perimeter to trigger zero-network local alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.recentBreachEvents, key = { it.id }) { event ->
                    GeofenceEventLogCard(event = event)
                }
            }
        }

        if (state.isAddDialogOpen) {
            AddGeofenceDialog(
                state = state,
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun GeofenceItemCard(
    zone: GeofenceZone,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = zone.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Radius: ${zone.radiusMeters.toInt()}m • Lat: ${zone.latitude}, Lng: ${zone.longitude}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Transitions: ${zone.transitionTypes.joinToString()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryTeal
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = zone.isActive,
                    onCheckedChange = onToggle
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                }
            }
        }
    }
}

@Composable
private fun GeofenceEventLogCard(event: GeofenceEventRecord) {
    val dateFormatted = remember(event.timestampMillis) {
        SimpleDateFormat("HH:mm:ss • MMM dd", Locale.US).format(Date(event.timestampMillis))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PrimaryGreen)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "${event.transitionType.name} • ${event.geofenceId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$dateFormatted • Local Alert Fired",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddGeofenceDialog(
    state: GeofenceUiState,
    onEvent: (GeofenceUiEvent) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onEvent(GeofenceUiEvent.CloseAddDialog) },
        title = { Text("Register Geofence Zone") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.pendingZoneName,
                    onValueChange = { onEvent(GeofenceUiEvent.NameChanged(it)) },
                    label = { Text("Zone Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.pendingLatitude,
                        onValueChange = { onEvent(GeofenceUiEvent.LatitudeChanged(it)) },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.pendingLongitude,
                        onValueChange = { onEvent(GeofenceUiEvent.LongitudeChanged(it)) },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(text = "Perimeter Radius: ${state.pendingRadiusMeters.toInt()} meters")
                Slider(
                    value = state.pendingRadiusMeters,
                    onValueChange = { onEvent(GeofenceUiEvent.RadiusChanged(it)) },
                    valueRange = 50f..1000f
                )
            }
        },
        confirmButton = {
            Button(onClick = { onEvent(GeofenceUiEvent.ConfirmAddGeofence) }) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(GeofenceUiEvent.CloseAddDialog) }) {
                Text("Cancel")
            }
        }
    )
}
