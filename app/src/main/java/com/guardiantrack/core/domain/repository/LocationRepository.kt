package com.guardiantrack.core.domain.repository

import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.SamplingProfile
import com.guardiantrack.core.model.location.TrackingState
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeTrackingState(): Flow<TrackingState>
    fun observeLatestLocation(): Flow<LocationPoint?>
    fun observeBreadcrumbs(limit: Int): Flow<List<LocationPoint>>
    fun observeBreadcrumbsBetween(startMillis: Long, endMillis: Long): Flow<List<LocationPoint>>

    suspend fun startTracking(profile: SamplingProfile): Result<Unit>
    suspend fun stopTracking(): Result<Unit>
    suspend fun recordLocation(location: LocationPoint): Result<Long>

    fun getUnsyncedLocations(limit: Int): Flow<List<LocationPoint>>
    suspend fun markLocationsAsSynced(ids: List<Long>): Result<Unit>
    suspend fun markLocationsAsFailed(ids: List<Long>): Result<Unit>
    suspend fun pruneSyncedLocations(olderThanMillis: Long): Result<Int>
    fun countPendingLocations(): Flow<Int>
    fun countTotalLocations(): Flow<Int>
}
