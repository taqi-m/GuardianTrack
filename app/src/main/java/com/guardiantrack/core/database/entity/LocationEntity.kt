package com.guardiantrack.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    indices = [
        Index(value = ["sync_status"], name = "idx_locations_sync_status"),
        Index(value = ["timestamp_millis"], name = "idx_locations_timestamp")
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    @ColumnInfo(name = "altitude")
    val altitude: Double? = null,
    @ColumnInfo(name = "accuracy_meters")
    val accuracyMeters: Float,
    @ColumnInfo(name = "speed_kmh")
    val speedKmh: Float,
    @ColumnInfo(name = "bearing_degrees")
    val bearingDegrees: Float? = null,
    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long,
    @ColumnInfo(name = "provider")
    val provider: String,
    @ColumnInfo(name = "sync_status")
    val syncStatus: String,
    @ColumnInfo(name = "is_mocked")
    val isMocked: Boolean = false
)
