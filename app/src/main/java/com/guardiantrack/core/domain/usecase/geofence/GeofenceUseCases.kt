package com.guardiantrack.core.domain.usecase.geofence

import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.domain.repository.GeofenceRepository
import com.guardiantrack.core.model.geofence.GeofenceEventRecord
import com.guardiantrack.core.model.geofence.GeofenceZone
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AddGeofenceZoneUseCase @Inject constructor(
    private val geofenceRepository: GeofenceRepository
) {
    suspend operator fun invoke(geofence: GeofenceZone): Result<Unit> {
        return geofenceRepository.addGeofence(geofence)
    }
}

class RemoveGeofenceZoneUseCase @Inject constructor(
    private val geofenceRepository: GeofenceRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return geofenceRepository.removeGeofence(id)
    }
}

class ObserveGeofencesUseCase @Inject constructor(
    private val geofenceRepository: GeofenceRepository
) {
    operator fun invoke(): Flow<List<GeofenceZone>> {
        return geofenceRepository.observeAllGeofences()
    }
}

class ObserveGeofenceEventsUseCase @Inject constructor(
    private val geofenceRepository: GeofenceRepository
) {
    operator fun invoke(limit: Int = 50): Flow<List<GeofenceEventRecord>> {
        return geofenceRepository.observeRecentGeofenceEvents(limit)
    }
}
