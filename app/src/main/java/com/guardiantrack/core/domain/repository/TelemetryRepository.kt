package com.guardiantrack.core.domain.repository

import com.guardiantrack.core.model.telemetry.TelemetryStats
import kotlinx.coroutines.flow.Flow

interface TelemetryRepository {
    fun observeTelemetryStats(): Flow<TelemetryStats>
    suspend fun recordBatterySnapshot(levelPercent: Float, isCharging: Boolean)
    suspend fun recordSystemLog(tag: String, message: String, isWarningOrError: Boolean)
}
