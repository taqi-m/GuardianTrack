package com.guardiantrack.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guardiantrack.core.database.entity.GeofenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofence(geofence: GeofenceEntity)

    @Query("SELECT * FROM geofences ORDER BY created_at_millis DESC")
    fun observeAllGeofences(): Flow<List<GeofenceEntity>>

    @Query("SELECT * FROM geofences WHERE is_active = 1")
    fun observeActiveGeofences(): Flow<List<GeofenceEntity>>

    @Query("SELECT * FROM geofences WHERE id = :id LIMIT 1")
    fun getGeofenceById(id: String): Flow<GeofenceEntity?>

    @Query("DELETE FROM geofences WHERE id = :id")
    suspend fun deleteGeofenceById(id: String)

    @Query("UPDATE geofences SET is_active = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: String, isActive: Boolean)
}
