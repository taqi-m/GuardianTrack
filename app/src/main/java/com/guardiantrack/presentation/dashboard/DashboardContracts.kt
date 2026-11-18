package com.guardiantrack.presentation.dashboard

import androidx.compose.runtime.Immutable
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.location.SamplingProfile
import com.guardiantrack.core.model.location.TrackingState
import com.guardiantrack.core.model.telemetry.MotionState

@Immutable
data class DashboardUiState(
    val trackingState: TrackingState = TrackingState.STOPPED,
    val selectedProfile: SamplingProfile = SamplingProfile.STANDARD,
    val latestLocation: LocationPoint? = null,
    val recentBreadcrumbs: List<LocationPoint> = emptyList(),
    val batteryDrainPerHour: Float = 0.8f,
    val batteryLevelPercent: Float = 100.0f,
    val isCharging: Boolean = false,
    val activeProvider: LocationProvider = LocationProvider.FUSED,
    val horizontalAccuracyMeters: Float = 0.0f,
    val pendingSyncCount: Int = 0,
    val totalRecordedCount: Int = 0,
    val trackingUptimeFormatted: String = "00:00:00",
    val motionState: MotionState = MotionState.STATIONARY,
    val isOemExempted: Boolean = true,
    val userMessage: String? = null
)

sealed interface DashboardUiEvent {
    data object ToggleTrackingClicked : DashboardUiEvent
    data class ChangeSamplingProfile(val profile: SamplingProfile) : DashboardUiEvent
    data object ForceSyncClicked : DashboardUiEvent
    data object DismissMessage : DashboardUiEvent
}
