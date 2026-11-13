package com.guardiantrack.core.model.geofence

import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.sync.SyncStatus

enum class GeofenceTransitionType {
    ENTER,
    EXIT,
    DWELL,
    UNKNOWN
}

data class GeofenceZone(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val transitionTypes: Set<GeofenceTransitionType>,
    val loiteringDelayMillis: Long = 30_000L,
    val expirationDurationMillis: Long = -1L, // -1L = NEVER EXPIRE
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class GeofenceEventRecord(
    val id: Long = 0L,
    val geofenceId: String,
    val transitionType: GeofenceTransitionType,
    val triggerLocation: LocationPoint? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
