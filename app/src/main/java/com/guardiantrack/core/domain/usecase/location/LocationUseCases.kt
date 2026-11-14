package com.guardiantrack.core.domain.usecase.location

import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.SamplingProfile
import com.guardiantrack.core.model.location.TrackingState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StartLocationTrackingUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(profile: SamplingProfile = SamplingProfile.STANDARD): Result<Unit> {
        return locationRepository.startTracking(profile)
    }
}

class StopLocationTrackingUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return locationRepository.stopTracking()
    }
}

class GetLocationHistoryUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    operator fun invoke(limit: Int = 100): Flow<List<LocationPoint>> {
        return locationRepository.observeBreadcrumbs(limit)
    }
}

class ObserveTrackingStateUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    operator fun invoke(): Flow<TrackingState> {
        return locationRepository.observeTrackingState()
    }
}
