package com.guardiantrack.core.model.telemetry

import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.location.TrackingState

enum class MotionState {
    STATIONARY,
    WALKING,
    RUNNING,
    IN_VEHICLE,
    UNKNOWN
}

data class TelemetryStats(
    val batteryLevelPercent: Float = 100f,
    val batteryDrainPerHourPercent: Float = 0f,
    val isCharging: Boolean = false,
    val activeProvider: LocationProvider = LocationProvider.UNKNOWN,
    val currentHorizontalAccuracyMeters: Float = 0f,
    val unsyncedBreadcrumbsCount: Int = 0,
    val totalRecordedBreadcrumbsCount: Int = 0,
    val unsyncedGeofenceEventsCount: Int = 0,
    val trackingUptimeMillis: Long = 0L,
    val currentTrackingState: TrackingState = TrackingState.STOPPED,
    val currentMotionState: MotionState = MotionState.UNKNOWN
)
