package com.guardiantrack.core.data.repository

import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.data.mapper.toDomain
import com.guardiantrack.core.data.mapper.toEntity
import com.guardiantrack.core.database.dao.LocationDao
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.SamplingProfile
import com.guardiantrack.core.model.location.TrackingState
import com.guardiantrack.core.model.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
    private val dispatchers: CoroutineDispatchers
) : LocationRepository {

    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)

    override fun observeTrackingState(): Flow<TrackingState> = _trackingState.asStateFlow()

    override fun observeLatestLocation(): Flow<LocationPoint?> {
        return locationDao.observeLatestLocation()
            .map { it?.toDomain() }
            .flowOn(dispatchers.io)
    }

    override fun observeBreadcrumbs(limit: Int): Flow<List<LocationPoint>> {
        return locationDao.observeRecentBreadcrumbs(limit)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeBreadcrumbsBetween(startMillis: Long, endMillis: Long): Flow<List<LocationPoint>> {
        return locationDao.observeBreadcrumbsBetween(startMillis, endMillis)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override suspend fun startTracking(profile: SamplingProfile): Result<Unit> = withContext(dispatchers.io) {
        _trackingState.value = TrackingState.TRACKING_HIGH_ACCURACY
        Result.Success(Unit)
    }

    override suspend fun stopTracking(): Result<Unit> = withContext(dispatchers.io) {
        _trackingState.value = TrackingState.STOPPED
        Result.Success(Unit)
    }

    override suspend fun recordLocation(location: LocationPoint): Result<Long> = withContext(dispatchers.io) {
        try {
            val id = locationDao.insertLocation(location.toEntity())
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getUnsyncedLocations(limit: Int): Flow<List<LocationPoint>> {
        return locationDao.getPendingLocations(limit)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override suspend fun markLocationsAsSynced(ids: List<Long>): Result<Unit> = withContext(dispatchers.io) {
        try {
            locationDao.updateSyncStatus(ids, SyncStatus.SYNCED.name)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun markLocationsAsFailed(ids: List<Long>): Result<Unit> = withContext(dispatchers.io) {
        try {
            locationDao.updateSyncStatus(ids, SyncStatus.FAILED.name)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun pruneSyncedLocations(olderThanMillis: Long): Result<Int> = withContext(dispatchers.io) {
        try {
            val pruned = locationDao.deleteSyncedLocationsOlderThan(olderThanMillis)
            Result.Success(pruned)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun countPendingLocations(): Flow<Int> = locationDao.countPendingLocations().flowOn(dispatchers.io)

    override fun countTotalLocations(): Flow<Int> = locationDao.countTotalLocations().flowOn(dispatchers.io)
}
