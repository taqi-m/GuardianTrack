package com.guardiantrack.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guardiantrack.core.database.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Query("SELECT * FROM locations ORDER BY timestamp_millis DESC LIMIT 1")
    fun observeLatestLocation(): Flow<LocationEntity?>

    @Query("SELECT * FROM locations ORDER BY timestamp_millis DESC LIMIT :limit")
    fun observeRecentBreadcrumbs(limit: Int): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE timestamp_millis BETWEEN :startMillis AND :endMillis ORDER BY timestamp_millis ASC")
    fun observeBreadcrumbsBetween(startMillis: Long, endMillis: Long): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE sync_status IN ('PENDING', 'FAILED') ORDER BY timestamp_millis ASC LIMIT :limit")
    fun getPendingLocations(limit: Int): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE sync_status IN ('PENDING', 'FAILED') ORDER BY timestamp_millis ASC LIMIT :limit")
    suspend fun getPendingLocationsSnapshot(limit: Int): List<LocationEntity>

    @Query("UPDATE locations SET sync_status = :status WHERE id IN (:ids)")
    suspend fun updateSyncStatus(ids: List<Long>, status: String)

    @Query("DELETE FROM locations WHERE sync_status = 'SYNCED' AND timestamp_millis < :thresholdMillis")
    suspend fun deleteSyncedLocationsOlderThan(thresholdMillis: Long): Int

    @Query("SELECT COUNT(*) FROM locations WHERE sync_status IN ('PENDING', 'FAILED')")
    fun countPendingLocations(): Flow<Int>

    @Query("SELECT COUNT(*) FROM locations")
    fun countTotalLocations(): Flow<Int>
}
