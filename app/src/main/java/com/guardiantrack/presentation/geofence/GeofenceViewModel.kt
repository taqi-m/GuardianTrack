package com.guardiantrack.presentation.geofence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.domain.repository.GeofenceRepository
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.location.manager.GeofencingEngine
import com.guardiantrack.core.model.geofence.GeofenceTransitionType
import com.guardiantrack.core.model.geofence.GeofenceZone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class GeofenceDialogDraft(
    val isOpen: Boolean = false,
    val name: String = "New HQ Zone",
    val lat: String = "37.7749",
    val lng: String = "-122.4194",
    val radius: Float = 150f
)

@HiltViewModel
class GeofenceViewModel @Inject constructor(
    private val geofenceRepository: GeofenceRepository,
    private val geofencingEngine: GeofencingEngine,
    private val locationRepository: LocationRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _dialogDraft = MutableStateFlow(GeofenceDialogDraft())
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GeofenceUiState> = combine(
        geofenceRepository.observeAllGeofences(),
        geofenceRepository.observeRecentGeofenceEvents(30),
        _dialogDraft,
        _userMessage
    ) { allZones, events, draft, message ->
        GeofenceUiState(
            geofences = allZones,
            recentBreachEvents = events,
            isAddDialogOpen = draft.isOpen,
            pendingZoneName = draft.name,
            pendingLatitude = draft.lat,
            pendingLongitude = draft.lng,
            pendingRadiusMeters = draft.radius,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GeofenceUiState()
    )

    fun onEvent(event: GeofenceUiEvent) {
        when (event) {
            is GeofenceUiEvent.OpenAddDialog -> {
                _dialogDraft.value = _dialogDraft.value.copy(isOpen = true)
            }
            is GeofenceUiEvent.CloseAddDialog -> {
                _dialogDraft.value = _dialogDraft.value.copy(isOpen = false)
            }
            is GeofenceUiEvent.NameChanged -> {
                _dialogDraft.value = _dialogDraft.value.copy(name = event.name)
            }
            is GeofenceUiEvent.LatitudeChanged -> {
                _dialogDraft.value = _dialogDraft.value.copy(lat = event.lat)
            }
            is GeofenceUiEvent.LongitudeChanged -> {
                _dialogDraft.value = _dialogDraft.value.copy(lng = event.lng)
            }
            is GeofenceUiEvent.RadiusChanged -> {
                _dialogDraft.value = _dialogDraft.value.copy(radius = event.radius)
            }
            is GeofenceUiEvent.ConfirmAddGeofence -> addGeofence()
            is GeofenceUiEvent.DeleteGeofence -> deleteGeofence(event.id)
            is GeofenceUiEvent.ToggleActive -> toggleGeofence(event.id, event.isActive)
            is GeofenceUiEvent.DismissMessage -> _userMessage.value = null
        }
    }

    private fun addGeofence() {
        val draft = _dialogDraft.value
        val lat = draft.lat.toDoubleOrNull()
        val lng = draft.lng.toDoubleOrNull()
        val name = draft.name.trim()

        if (lat == null || lng == null || name.isEmpty()) {
            _userMessage.value = "Please provide valid coordinates and a name"
            return
        }

        val zone = GeofenceZone(
            id = "geo-${UUID.randomUUID().toString().take(8)}",
            name = name,
            latitude = lat,
            longitude = lng,
            radiusMeters = draft.radius,
            transitionTypes = setOf(GeofenceTransitionType.ENTER, GeofenceTransitionType.EXIT, GeofenceTransitionType.DWELL),
            loiteringDelayMillis = 30_000L,
            isActive = true
        )

        viewModelScope.launch(dispatchers.io) {
            geofenceRepository.addGeofence(zone)
            val registerResult = geofencingEngine.registerGeofences(listOf(zone))
            when (registerResult) {
                is Result.Success -> {
                    _userMessage.value = "Geofence registered with hardware subsystem"
                }
                is Result.Error -> {
                    _userMessage.value = "Hardware geofence registration warning: ${registerResult.message}"
                }
                is Result.Loading -> {}
            }
            _dialogDraft.value = _dialogDraft.value.copy(isOpen = false)
        }
    }

    private fun deleteGeofence(id: String) {
        viewModelScope.launch(dispatchers.io) {
            geofencingEngine.removeGeofences(listOf(id))
            geofenceRepository.removeGeofence(id)
            _userMessage.value = "Geofence removed"
        }
    }

    private fun toggleGeofence(id: String, isActive: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            geofenceRepository.updateGeofenceStatus(id, isActive)
            if (isActive) {
                val zone = geofenceRepository.getGeofenceById(id).first()
                if (zone != null) {
                    geofencingEngine.registerGeofences(listOf(zone))
                }
            } else {
                geofencingEngine.removeGeofences(listOf(id))
            }
        }
    }
}
