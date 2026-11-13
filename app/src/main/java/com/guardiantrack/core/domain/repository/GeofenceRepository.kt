package com.guardiantrack.core.domain.repository

import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.model.geofence.GeofenceEventRecord
import com.guardiantrack.core.model.geofence.GeofenceZone
import kotlinx.coroutines.flow.Flow

interface GeofenceRepository {
    fun observeAllGeofences(): Flow<List<GeofenceZone>>
    fun observeActiveGeofences(): Flow<List<GeofenceZone>>
    fun getGeofenceById(id: String): Flow<GeofenceZone?>

    suspend fun addGeofence(geofence: GeofenceZone): Result<Unit>
    suspend fun removeGeofence(id: String): Result<Unit>
    suspend fun updateGeofenceStatus(id: String, isActive: Boolean): Result<Unit>

    suspend fun recordGeofenceEvent(event: GeofenceEventRecord): Result<Long>
    fun observeRecentGeofenceEvents(limit: Int): Flow<List<GeofenceEventRecord>>
    fun getUnsyncedGeofenceEvents(limit: Int): Flow<List<GeofenceEventRecord>>
    suspend fun markGeofenceEventsAsSynced(ids: List<Long>): Result<Unit>
    fun countPendingGeofenceEvents(): Flow<Int>
}
