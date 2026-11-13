package com.guardiantrack.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "geofences",
    indices = [
        Index(value = ["is_active"], name = "idx_geofences_active")
    ]
)
data class GeofenceEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    @ColumnInfo(name = "radius_meters")
    val radiusMeters: Float,
    @ColumnInfo(name = "transition_types")
    val transitionTypesCsv: String,
    @ColumnInfo(name = "loitering_delay_ms")
    val loiteringDelayMs: Long,
    @ColumnInfo(name = "expiration_duration_ms")
    val expirationDurationMs: Long,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long
)
