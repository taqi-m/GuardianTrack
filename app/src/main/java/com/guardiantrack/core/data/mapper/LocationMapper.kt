package com.guardiantrack.core.data.mapper

import com.guardiantrack.core.database.entity.LocationEntity
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.sync.SyncStatus

fun LocationEntity.toDomain(): LocationPoint {
    return LocationPoint(
        id = id,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        accuracyMeters = accuracyMeters,
        speedKmh = speedKmh,
        bearingDegrees = bearingDegrees,
        timestampMillis = timestampMillis,
        provider = try {
            LocationProvider.valueOf(provider)
        } catch (_: Exception) {
            LocationProvider.UNKNOWN
        },
        syncStatus = try {
            SyncStatus.valueOf(syncStatus)
        } catch (_: Exception) {
            SyncStatus.PENDING
        },
        isMocked = isMocked
    )
}

fun LocationPoint.toEntity(): LocationEntity {
    return LocationEntity(
        id = id,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        accuracyMeters = accuracyMeters,
        speedKmh = speedKmh,
        bearingDegrees = bearingDegrees,
        timestampMillis = timestampMillis,
        provider = provider.name,
        syncStatus = syncStatus.name,
        isMocked = isMocked
    )
}
