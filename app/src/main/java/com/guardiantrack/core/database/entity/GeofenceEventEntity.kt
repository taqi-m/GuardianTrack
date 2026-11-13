package com.guardiantrack.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "geofence_events",
    indices = [
        Index(value = ["geofence_id"], name = "idx_events_geofence_id"),
        Index(value = ["timestamp_millis"], name = "idx_events_timestamp"),
        Index(value = ["sync_status"], name = "idx_events_sync_status")
    ]
)
data class GeofenceEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "geofence_id")
    val geofenceId: String,
    @ColumnInfo(name = "transition_type")
    val transitionType: String,
    @ColumnInfo(name = "trigger_latitude")
    val triggerLatitude: Double? = null,
    @ColumnInfo(name = "trigger_longitude")
    val triggerLongitude: Double? = null,
    @ColumnInfo(name = "trigger_accuracy")
    val triggerAccuracy: Float? = null,
    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long,
    @ColumnInfo(name = "sync_status")
    val syncStatus: String
)
