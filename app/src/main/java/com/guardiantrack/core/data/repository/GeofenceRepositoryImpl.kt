package com.guardiantrack.core.data.repository

import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.data.mapper.toDomain
import com.guardiantrack.core.data.mapper.toEntity
import com.guardiantrack.core.database.dao.GeofenceDao
import com.guardiantrack.core.database.dao.GeofenceEventDao
import com.guardiantrack.core.domain.repository.GeofenceRepository
import com.guardiantrack.core.model.geofence.GeofenceEventRecord
import com.guardiantrack.core.model.geofence.GeofenceZone
import com.guardiantrack.core.model.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceRepositoryImpl @Inject constructor(
    private val geofenceDao: GeofenceDao,
    private val geofenceEventDao: GeofenceEventDao,
    private val dispatchers: CoroutineDispatchers
) : GeofenceRepository {

    override fun observeAllGeofences(): Flow<List<GeofenceZone>> {
        return geofenceDao.observeAllGeofences()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeActiveGeofences(): Flow<List<GeofenceZone>> {
        return geofenceDao.observeActiveGeofences()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun getGeofenceById(id: String): Flow<GeofenceZone?> {
        return geofenceDao.getGeofenceById(id)
            .map { it?.toDomain() }
            .flowOn(dispatchers.io)
    }

    override suspend fun addGeofence(geofence: GeofenceZone): Result<Unit> = withContext(dispatchers.io) {
        try {
            geofenceDao.insertGeofence(geofence.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeGeofence(id: String): Result<Unit> = withContext(dispatchers.io) {
        try {
            geofenceDao.deleteGeofenceById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateGeofenceStatus(id: String, isActive: Boolean): Result<Unit> = withContext(dispatchers.io) {
        try {
            geofenceDao.updateActiveStatus(id, isActive)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun recordGeofenceEvent(event: GeofenceEventRecord): Result<Long> = withContext(dispatchers.io) {
        try {
            val id = geofenceEventDao.insertEvent(event.toEntity())
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeRecentGeofenceEvents(limit: Int): Flow<List<GeofenceEventRecord>> {
        return geofenceEventDao.observeRecentEvents(limit)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun getUnsyncedGeofenceEvents(limit: Int): Flow<List<GeofenceEventRecord>> {
        return geofenceEventDao.getPendingEvents(limit)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override suspend fun markGeofenceEventsAsSynced(ids: List<Long>): Result<Unit> = withContext(dispatchers.io) {
        try {
            geofenceEventDao.updateSyncStatus(ids, SyncStatus.SYNCED.name)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun countPendingGeofenceEvents(): Flow<Int> = geofenceEventDao.countPendingEvents().flowOn(dispatchers.io)
}
