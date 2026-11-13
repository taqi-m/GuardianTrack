package com.guardiantrack.core.model.location

import com.guardiantrack.core.model.sync.SyncStatus

enum class LocationProvider {
    GPS,
    NETWORK_CELL_WIFI,
    FUSED,
    UNKNOWN
}

enum class TrackingState {
    STOPPED,
    STARTING,
    TRACKING_HIGH_ACCURACY,
    TRACKING_BALANCED,
    TRACKING_STATIONARY,
    ERROR
}

enum class SamplingProfile(
    val intervalMillis: Long,
    val fastestIntervalMillis: Long,
    val displacementMeters: Float
) {
    STANDARD(intervalMillis = 10_000L, fastestIntervalMillis = 5_000L, displacementMeters = 10f),
    ECO(intervalMillis = 60_000L, fastestIntervalMillis = 30_000L, displacementMeters = 50f),
    STATIONARY_ADAPTIVE(intervalMillis = 300_000L, fastestIntervalMillis = 60_000L, displacementMeters = 100f)
}

data class LocationPoint(
    val id: Long = 0L,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracyMeters: Float,
    val speedKmh: Float,
    val bearingDegrees: Float? = null,
    val timestampMillis: Long,
    val provider: LocationProvider,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isMocked: Boolean = false
)
