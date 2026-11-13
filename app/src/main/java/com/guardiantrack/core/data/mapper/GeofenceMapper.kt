package com.guardiantrack.core.data.mapper

import com.guardiantrack.core.database.entity.GeofenceEntity
import com.guardiantrack.core.database.entity.GeofenceEventEntity
import com.guardiantrack.core.model.geofence.GeofenceEventRecord
import com.guardiantrack.core.model.geofence.GeofenceTransitionType
import com.guardiantrack.core.model.geofence.GeofenceZone
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.sync.SyncStatus

fun GeofenceEntity.toDomain(): GeofenceZone {
    val transitions = transitionTypesCsv.split(",")
        .mapNotNull { name ->
            try {
                GeofenceTransitionType.valueOf(name.trim())
            } catch (_: Exception) {
                null
            }
        }.toSet()

    return GeofenceZone(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
        transitionTypes = transitions,
        loiteringDelayMillis = loiteringDelayMs,
        expirationDurationMillis = expirationDurationMs,
        isActive = isActive,
        createdAtMillis = createdAtMillis
    )
}

fun GeofenceZone.toEntity(): GeofenceEntity {
    return GeofenceEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
        transitionTypesCsv = transitionTypes.joinToString(",") { it.name },
        loiteringDelayMs = loiteringDelayMillis,
        expirationDurationMs = expirationDurationMillis,
        isActive = isActive,
        createdAtMillis = createdAtMillis
    )
}

fun GeofenceEventEntity.toDomain(): GeofenceEventRecord {
    val loc = if (triggerLatitude != null && triggerLongitude != null) {
        LocationPoint(
            latitude = triggerLatitude,
            longitude = triggerLongitude,
            accuracyMeters = triggerAccuracy ?: 0f,
            speedKmh = 0f,
            timestampMillis = timestampMillis,
            provider = LocationProvider.FUSED,
            syncStatus = try {
                SyncStatus.valueOf(syncStatus)
            } catch (_: Exception) {
                SyncStatus.PENDING
            }
        )
    } else null

    return GeofenceEventRecord(
        id = id,
        geofenceId = geofenceId,
        transitionType = try {
            GeofenceTransitionType.valueOf(transitionType)
        } catch (_: Exception) {
            GeofenceTransitionType.UNKNOWN
        },
        triggerLocation = loc,
        timestampMillis = timestampMillis,
        syncStatus = try {
            SyncStatus.valueOf(syncStatus)
        } catch (_: Exception) {
            SyncStatus.PENDING
        }
    )
}

fun GeofenceEventRecord.toEntity(): GeofenceEventEntity {
    return GeofenceEventEntity(
        id = id,
        geofenceId = geofenceId,
        transitionType = transitionType.name,
        triggerLatitude = triggerLocation?.latitude,
        triggerLongitude = triggerLocation?.longitude,
        triggerAccuracy = triggerLocation?.accuracyMeters,
        timestampMillis = timestampMillis,
        syncStatus = syncStatus.name
    )
}
