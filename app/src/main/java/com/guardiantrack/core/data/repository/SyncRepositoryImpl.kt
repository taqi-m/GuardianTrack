package com.guardiantrack.core.data.repository

import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.database.dao.GeofenceEventDao
import com.guardiantrack.core.database.dao.LocationDao
import com.guardiantrack.core.database.security.DatabaseKeyManager
import com.guardiantrack.core.domain.repository.SyncRepository
import com.guardiantrack.core.model.sync.SyncEngineState
import com.guardiantrack.core.model.sync.SyncResult
import com.guardiantrack.core.model.sync.SyncStatus
import com.guardiantrack.core.network.api.GuardianSyncApi
import com.guardiantrack.core.network.dto.GeofenceEventPayloadDto
import com.guardiantrack.core.network.dto.LocationPayloadDto
import com.guardiantrack.core.network.dto.SyncBatchRequest
import com.guardiantrack.core.network.watcher.NetworkConnectivityWatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
    private val geofenceEventDao: GeofenceEventDao,
    private val syncApi: GuardianSyncApi,
    private val connectivityWatcher: NetworkConnectivityWatcher,
    private val keyManager: DatabaseKeyManager,
    private val dispatchers: CoroutineDispatchers
) : SyncRepository {

    private val _syncState = MutableStateFlow(SyncEngineState.IDLE)
    private var consecutiveFailures = 0

    override fun observeSyncEngineState(): Flow<SyncEngineState> = _syncState.asStateFlow()

    override suspend fun syncPendingData(batchSize: Int): SyncResult = withContext(dispatchers.io) {
        val pendingLocations = locationDao.getPendingLocationsSnapshot(batchSize)
        val pendingEvents = geofenceEventDao.getPendingEventsSnapshot(batchSize)

        if (pendingLocations.isEmpty() && pendingEvents.isEmpty()) {
            return@withContext SyncResult.NoDataPending
        }

        val locationIds = pendingLocations.map { it.id }
        val eventIds = pendingEvents.map { it.id }

        try {
            _syncState.value = SyncEngineState.SYNCING
            locationDao.updateSyncStatus(locationIds, SyncStatus.SYNCING.name)
            geofenceEventDao.updateSyncStatus(eventIds, SyncStatus.SYNCING.name)

            val request = SyncBatchRequest(
                deviceId = keyManager.getOrCreateDeviceId(),
                batchTimestamp = System.currentTimeMillis(),
                locations = pendingLocations.map { loc ->
                    LocationPayloadDto(
                        clientId = loc.id,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitude = loc.altitude,
                        accuracyMeters = loc.accuracyMeters,
                        speedKmh = loc.speedKmh,
                        bearingDegrees = loc.bearingDegrees,
                        timestampMillis = loc.timestampMillis,
                        provider = loc.provider,
                        isMocked = loc.isMocked
                    )
                },
                geofenceEvents = pendingEvents.map { ev ->
                    GeofenceEventPayloadDto(
                        clientId = ev.id,
                        geofenceId = ev.geofenceId,
                        transitionType = ev.transitionType,
                        triggerLatitude = ev.triggerLatitude,
                        triggerLongitude = ev.triggerLongitude,
                        triggerAccuracy = ev.triggerAccuracy,
                        timestampMillis = ev.timestampMillis
                    )
                }
            )

            val response = syncApi.uploadBatch(request)
            if (response.isSuccessful && response.body() != null) {
                locationDao.updateSyncStatus(locationIds, SyncStatus.SYNCED.name)
                geofenceEventDao.updateSyncStatus(eventIds, SyncStatus.SYNCED.name)
                consecutiveFailures = 0
                _syncState.value = SyncEngineState.IDLE
                SyncResult.Success(
                    pointsSynced = locationIds.size,
                    eventsSynced = eventIds.size,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                handleSyncFailure(locationIds, eventIds, Exception("Server HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            handleSyncFailure(locationIds, eventIds, e)
        }
    }

    private suspend fun handleSyncFailure(
        locationIds: List<Long>,
        eventIds: List<Long>,
        error: Throwable
    ): SyncResult {
        consecutiveFailures++
        locationDao.updateSyncStatus(locationIds, SyncStatus.FAILED.name)
        geofenceEventDao.updateSyncStatus(eventIds, SyncStatus.FAILED.name)
        _syncState.value = SyncEngineState.BACKOFF_WAIT

        // Exponential backoff formula with ±20% jitter:
        // delay = min(60s, 2s * 2^failures) * (1 ± 0.20)
        val baseDelay = min(60_000L, 2_000L * (1 shl min(consecutiveFailures, 5)))
        val jitterMultiplier = 1.0 + (Random.nextDouble(-0.20, 0.20))
        val retryScheduledMillis = (baseDelay * jitterMultiplier).toLong()

        return SyncResult.Failure(error, retryScheduledMillis)
    }

    override suspend fun executePruning(olderThanMillis: Long): Int = withContext(dispatchers.io) {
        val prunedLocations = locationDao.deleteSyncedLocationsOlderThan(olderThanMillis)
        val prunedEvents = geofenceEventDao.deleteSyncedEventsOlderThan(olderThanMillis)
        prunedLocations + prunedEvents
    }

    override fun isNetworkAvailable(): Flow<Boolean> = connectivityWatcher.isOnline
}
