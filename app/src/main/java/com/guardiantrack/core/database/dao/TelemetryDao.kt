package com.guardiantrack.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guardiantrack.core.database.entity.TelemetryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TelemetryLogEntity): Long

    @Query("SELECT * FROM telemetry_logs ORDER BY timestamp_millis DESC LIMIT :limit")
    fun observeRecentLogs(limit: Int): Flow<List<TelemetryLogEntity>>

    @Query("SELECT * FROM telemetry_logs ORDER BY timestamp_millis DESC LIMIT 1")
    fun observeLatestLog(): Flow<TelemetryLogEntity?>

    @Query("DELETE FROM telemetry_logs WHERE timestamp_millis < :thresholdMillis")
    suspend fun deleteLogsOlderThan(thresholdMillis: Long): Int
}
