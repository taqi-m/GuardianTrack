package com.guardiantrack.presentation.geofence

import androidx.compose.runtime.Immutable
import com.guardiantrack.core.model.geofence.GeofenceEventRecord
import com.guardiantrack.core.model.geofence.GeofenceZone

@Immutable
data class GeofenceUiState(
    val geofences: List<GeofenceZone> = emptyList(),
    val recentBreachEvents: List<GeofenceEventRecord> = emptyList(),
    val isAddDialogOpen: Boolean = false,
    val pendingZoneName: String = "",
    val pendingLatitude: String = "37.7749",
    val pendingLongitude: String = "-122.4194",
    val pendingRadiusMeters: Float = 150f,
    val userMessage: String? = null
)

sealed interface GeofenceUiEvent {
    data object OpenAddDialog : GeofenceUiEvent
    data object CloseAddDialog : GeofenceUiEvent
    data class NameChanged(val name: String) : GeofenceUiEvent
    data class LatitudeChanged(val lat: String) : GeofenceUiEvent
    data class LongitudeChanged(val lng: String) : GeofenceUiEvent
    data class RadiusChanged(val radius: Float) : GeofenceUiEvent
    data object ConfirmAddGeofence : GeofenceUiEvent
    data class DeleteGeofence(val id: String) : GeofenceUiEvent
    data class ToggleActive(val id: String, val isActive: Boolean) : GeofenceUiEvent
    data object DismissMessage : GeofenceUiEvent
}
