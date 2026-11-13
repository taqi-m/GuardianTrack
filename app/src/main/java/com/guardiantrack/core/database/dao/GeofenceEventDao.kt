package com.guardiantrack.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guardiantrack.core.database.entity.GeofenceEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: GeofenceEventEntity): Long

    @Query("SELECT * FROM geofence_events ORDER BY timestamp_millis DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int): Flow<List<GeofenceEventEntity>>

    @Query("SELECT * FROM geofence_events WHERE sync_status IN ('PENDING', 'FAILED') ORDER BY timestamp_millis ASC LIMIT :limit")
    fun getPendingEvents(limit: Int): Flow<List<GeofenceEventEntity>>

    @Query("SELECT * FROM geofence_events WHERE sync_status IN ('PENDING', 'FAILED') ORDER BY timestamp_millis ASC LIMIT :limit")
    suspend fun getPendingEventsSnapshot(limit: Int): List<GeofenceEventEntity>

    @Query("UPDATE geofence_events SET sync_status = :status WHERE id IN (:ids)")
    suspend fun updateSyncStatus(ids: List<Long>, status: String)

    @Query("DELETE FROM geofence_events WHERE sync_status = 'SYNCED' AND timestamp_millis < :thresholdMillis")
    suspend fun deleteSyncedEventsOlderThan(thresholdMillis: Long): Int

    @Query("SELECT COUNT(*) FROM geofence_events WHERE sync_status IN ('PENDING', 'FAILED')")
    fun countPendingEvents(): Flow<Int>
}
