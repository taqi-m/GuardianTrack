package com.guardiantrack.core.data.repository

import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.database.dao.LocationDao
import com.guardiantrack.core.database.dao.TelemetryDao
import com.guardiantrack.core.database.entity.TelemetryLogEntity
import com.guardiantrack.core.domain.repository.TelemetryRepository
import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.location.TrackingState
import com.guardiantrack.core.model.telemetry.MotionState
import com.guardiantrack.core.model.telemetry.TelemetryStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryRepositoryImpl @Inject constructor(
    private val telemetryDao: TelemetryDao,
    private val locationDao: LocationDao,
    private val dispatchers: CoroutineDispatchers
) : TelemetryRepository {

    private val _telemetryStats = MutableStateFlow(
        TelemetryStats(
            batteryLevelPercent = 100f,
            batteryDrainPerHourPercent = 0.5f,
            isCharging = false,
            activeProvider = LocationProvider.FUSED,
            currentHorizontalAccuracyMeters = 8.5f,
            currentTrackingState = TrackingState.STOPPED,
            currentMotionState = MotionState.STATIONARY
        )
    )

    override fun observeTelemetryStats(): Flow<TelemetryStats> = _telemetryStats.asStateFlow()

    override suspend fun recordBatterySnapshot(levelPercent: Float, isCharging: Boolean): Unit = withContext(dispatchers.io) {
        _telemetryStats.value = _telemetryStats.value.copy(
            batteryLevelPercent = levelPercent,
            isCharging = isCharging
        )
    }

    override suspend fun recordSystemLog(tag: String, message: String, isWarningOrError: Boolean): Unit = withContext(dispatchers.io) {
        telemetryDao.insertLog(
            TelemetryLogEntity(
                batteryLevel = _telemetryStats.value.batteryLevelPercent,
                isCharging = _telemetryStats.value.isCharging,
                hourlyDrainRate = _telemetryStats.value.batteryDrainPerHourPercent,
                activeProvider = _telemetryStats.value.activeProvider.name,
                motionState = _telemetryStats.value.currentMotionState.name,
                tag = tag,
                message = message,
                isError = isWarningOrError,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }
}
