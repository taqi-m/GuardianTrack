package com.guardiantrack.presentation.dashboard

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.domain.repository.OemSurvivalRepository
import com.guardiantrack.core.domain.repository.SyncRepository
import com.guardiantrack.core.domain.repository.TelemetryRepository
import com.guardiantrack.core.location.service.GuardianLocationService
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.SamplingProfile
import com.guardiantrack.core.model.location.TrackingState
import com.guardiantrack.core.model.sync.SyncResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class LocationOverview(
    val trackingState: TrackingState = TrackingState.STOPPED,
    val latestLoc: LocationPoint? = null,
    val breadcrumbs: List<LocationPoint> = emptyList(),
    val pendingCount: Int = 0,
    val totalCount: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val syncRepository: SyncRepository,
    private val oemSurvivalRepository: OemSurvivalRepository,
    private val telemetryRepository: TelemetryRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _selectedProfile = MutableStateFlow(SamplingProfile.STANDARD)
    private val _userMessage = MutableStateFlow<String?>(null)

    private val locationOverview: Flow<LocationOverview> = combine(
        locationRepository.observeTrackingState(),
        locationRepository.observeLatestLocation(),
        locationRepository.observeBreadcrumbs(30),
        locationRepository.countPendingLocations(),
        locationRepository.countTotalLocations()
    ) { trackingState, latestLoc, breadcrumbs, pendingCount, totalCount ->
        LocationOverview(trackingState, latestLoc, breadcrumbs, pendingCount, totalCount)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        locationOverview,
        telemetryRepository.observeTelemetryStats(),
        _selectedProfile,
        _userMessage
    ) { loc, telemetry, profile, msg ->
        val isExempted = oemSurvivalRepository.isBatteryOptimizationExempted()

        DashboardUiState(
            trackingState = loc.trackingState,
            selectedProfile = profile,
            latestLocation = loc.latestLoc,
            recentBreadcrumbs = loc.breadcrumbs,
            batteryDrainPerHour = telemetry.batteryDrainPerHourPercent,
            batteryLevelPercent = telemetry.batteryLevelPercent,
            isCharging = telemetry.isCharging,
            activeProvider = loc.latestLoc?.provider ?: telemetry.activeProvider,
            horizontalAccuracyMeters = loc.latestLoc?.accuracyMeters ?: telemetry.currentHorizontalAccuracyMeters,
            pendingSyncCount = loc.pendingCount,
            totalRecordedCount = loc.totalCount,
            motionState = telemetry.currentMotionState,
            isOemExempted = isExempted,
            userMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            is DashboardUiEvent.ToggleTrackingClicked -> toggleTracking()
            is DashboardUiEvent.ChangeSamplingProfile -> changeProfile(event.profile)
            is DashboardUiEvent.ForceSyncClicked -> triggerManualSync()
            is DashboardUiEvent.DismissMessage -> _userMessage.value = null
        }
    }

    private fun toggleTracking() {
        val currentState = uiState.value.trackingState
        if (currentState == TrackingState.STOPPED) {
            val serviceIntent = Intent(context, GuardianLocationService::class.java).apply {
                action = GuardianLocationService.ACTION_START_TRACKING
                putExtra(GuardianLocationService.EXTRA_SAMPLING_PROFILE, _selectedProfile.value.name)
            }
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                _userMessage.value = "Failed to start service: ${e.message}"
            }
        } else {
            val stopIntent = Intent(context, GuardianLocationService::class.java).apply {
                action = GuardianLocationService.ACTION_STOP_TRACKING
            }
            context.stopService(stopIntent)
            viewModelScope.launch {
                locationRepository.stopTracking()
            }
        }
    }

    private fun changeProfile(profile: SamplingProfile) {
        _selectedProfile.value = profile
        if (uiState.value.trackingState != TrackingState.STOPPED) {
            val restartIntent = Intent(context, GuardianLocationService::class.java).apply {
                action = GuardianLocationService.ACTION_START_TRACKING
                putExtra(GuardianLocationService.EXTRA_SAMPLING_PROFILE, profile.name)
            }
            ContextCompat.startForegroundService(context, restartIntent)
        }
    }

    private fun triggerManualSync() {
        viewModelScope.launch(dispatchers.io) {
            val result = syncRepository.syncPendingData(batchSize = 50)
            when (result) {
                is SyncResult.Success -> {
                    _userMessage.value = "Synced ${result.pointsSynced} breadcrumbs successfully"
                }
                is SyncResult.Failure -> {
                    _userMessage.value = "Sync failed: ${result.error.message}"
                }
                is SyncResult.NoDataPending -> {
                    _userMessage.value = "All data is already synchronized"
                }
                is SyncResult.OfflineSkipped -> {
                    _userMessage.value = "Device is offline. Queued for next connection"
                }
            }
        }
    }
}
