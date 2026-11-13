package com.guardiantrack.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "telemetry_logs",
    indices = [
        Index(value = ["timestamp_millis"], name = "idx_telemetry_timestamp")
    ]
)
data class TelemetryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "battery_level")
    val batteryLevel: Float,
    @ColumnInfo(name = "is_charging")
    val isCharging: Boolean,
    @ColumnInfo(name = "hourly_drain_rate")
    val hourlyDrainRate: Float,
    @ColumnInfo(name = "active_provider")
    val activeProvider: String,
    @ColumnInfo(name = "motion_state")
    val motionState: String,
    @ColumnInfo(name = "tag")
    val tag: String,
    @ColumnInfo(name = "message")
    val message: String,
    @ColumnInfo(name = "is_error")
    val isError: Boolean = false,
    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long
)
