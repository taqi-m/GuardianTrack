package com.guardiantrack.core.data.repository

import app.cash.turbine.test
import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.database.dao.GeofenceEventDao
import com.guardiantrack.core.database.dao.LocationDao
import com.guardiantrack.core.database.entity.GeofenceEventEntity
import com.guardiantrack.core.database.entity.LocationEntity
import com.guardiantrack.core.model.sync.SyncEngineState
import com.guardiantrack.core.model.sync.SyncResult
import com.guardiantrack.core.model.sync.SyncStatus
import com.guardiantrack.core.network.api.GuardianSyncApi
import com.guardiantrack.core.network.dto.SyncBatchResponse
import com.guardiantrack.core.network.watcher.NetworkConnectivityWatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class SyncRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = CoroutineDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
        unconfined = testDispatcher
    )

    private val locationDao: LocationDao = mockk(relaxed = true)
    private val geofenceEventDao: GeofenceEventDao = mockk(relaxed = true)
    private val syncApi: GuardianSyncApi = mockk()
    private val connectivityWatcher: NetworkConnectivityWatcher = mockk(relaxed = true)
    private val keyManager: com.guardiantrack.core.database.security.DatabaseKeyManager = mockk {
        io.mockk.every { getOrCreateDeviceId() } returns "mock-device-id"
    }

    private lateinit var syncRepository: SyncRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        syncRepository = SyncRepositoryImpl(
            locationDao = locationDao,
            geofenceEventDao = geofenceEventDao,
            syncApi = syncApi,
            connectivityWatcher = connectivityWatcher,
            keyManager = keyManager,
            dispatchers = dispatchers
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleLocationEntity(id: Long) = LocationEntity(
        id = id,
        latitude = 37.7749,
        longitude = -122.4194,
        altitude = 15.0,
        accuracyMeters = 4.5f,
        speedKmh = 25.0f,
        bearingDegrees = 180.0f,
        timestampMillis = 1700000000L + id * 1000,
        provider = "GPS",
        isMocked = false,
        syncStatus = SyncStatus.PENDING.name
    )

    private fun sampleEventEntity(id: Long) = GeofenceEventEntity(
        id = id,
        geofenceId = "geo-$id",
        transitionType = "ENTER",
        triggerLatitude = 37.7749,
        triggerLongitude = -122.4194,
        triggerAccuracy = 5.0f,
        timestampMillis = 1700000000L,
        syncStatus = SyncStatus.PENDING.name
    )

    @Test
    fun syncPendingData_whenNoDataPending_returnsNoDataPendingAndDoesNotCallApi() = runTest(testDispatcher) {
        coEvery { locationDao.getPendingLocationsSnapshot(50) } returns emptyList()
        coEvery { geofenceEventDao.getPendingEventsSnapshot(50) } returns emptyList()

        val result = syncRepository.syncPendingData(50)

        assertEquals(SyncResult.NoDataPending, result)
        coVerify(exactly = 0) { syncApi.uploadBatch(any()) }
    }

    @Test
    fun syncPendingData_batchTransmission_successTransitionsToSynced() = runTest(testDispatcher) {
        val locations = listOf(sampleLocationEntity(1L), sampleLocationEntity(2L))
        val events = listOf(sampleEventEntity(10L))

        coEvery { locationDao.getPendingLocationsSnapshot(50) } returns locations
        coEvery { geofenceEventDao.getPendingEventsSnapshot(50) } returns events

        val fakeResponse = SyncBatchResponse(
            status = "SUCCESS",
            syncedLocationsCount = 2,
            syncedEventsCount = 1,
            serverTimestamp = 1700000050L
        )
        coEvery { syncApi.uploadBatch(any()) } returns Response.success(fakeResponse)

        syncRepository.observeSyncEngineState().test {
            assertEquals(SyncEngineState.IDLE, awaitItem())

            val result = syncRepository.syncPendingData(50)

            assertTrue(result is SyncResult.Success)
            val success = result as SyncResult.Success
            assertEquals(2, success.pointsSynced)
            assertEquals(1, success.eventsSynced)

            // Verify DAO transition order: PENDING -> SYNCING -> SYNCED
            coVerifyOrder {
                locationDao.updateSyncStatus(listOf(1L, 2L), SyncStatus.SYNCING.name)
                geofenceEventDao.updateSyncStatus(listOf(10L), SyncStatus.SYNCING.name)
                syncApi.uploadBatch(match { it.locations.size == 2 && it.geofenceEvents.size == 1 })
                locationDao.updateSyncStatus(listOf(1L, 2L), SyncStatus.SYNCED.name)
                geofenceEventDao.updateSyncStatus(listOf(10L), SyncStatus.SYNCED.name)
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun syncPendingData_whenHttpError_triggersExponentialBackoffWithJitter() = runTest(testDispatcher) {
        val locations = listOf(sampleLocationEntity(1L))
        coEvery { locationDao.getPendingLocationsSnapshot(50) } returns locations
        coEvery { geofenceEventDao.getPendingEventsSnapshot(50) } returns emptyList()

        coEvery { syncApi.uploadBatch(any()) } returns Response.error(
            500,
            "Internal Server Error".toResponseBody()
        )

        val result = syncRepository.syncPendingData(50)

        assertTrue(result is SyncResult.Failure)
        val failure = result as SyncResult.Failure
        assertTrue(failure.error.message?.contains("Server HTTP 500") == true)

        // For first failure: baseDelay = 2000L * 2^1 = 4000L.
        // With +-20% jitter: range is [3200L, 4800L].
        assertTrue(
            "Backoff delay ${failure.retryScheduledMillis} must be within jitter range [3200, 4800]",
            failure.retryScheduledMillis in 3200L..4800L
        )

        // Verify status marked as FAILED in local DB
        coVerify(exactly = 1) { locationDao.updateSyncStatus(listOf(1L), SyncStatus.FAILED.name) }
    }

    @Test
    fun syncPendingData_consecutiveFailures_increasesBackoffUpToCap() = runTest(testDispatcher) {
        val locations = listOf(sampleLocationEntity(1L))
        coEvery { locationDao.getPendingLocationsSnapshot(any()) } returns locations
        coEvery { geofenceEventDao.getPendingEventsSnapshot(any()) } returns emptyList()
        coEvery { syncApi.uploadBatch(any()) } returns Response.error(503, "Service Unavailable".toResponseBody())

        // Execute 5 consecutive failures to saturate the exponential backoff
        var lastFailure: SyncResult.Failure? = null
        repeat(5) {
            val result = syncRepository.syncPendingData(50)
            assertTrue(result is SyncResult.Failure)
            lastFailure = result as SyncResult.Failure
        }

        // 5th failure: baseDelay = min(60_000, 2_000 * 2^5 = 64_000) = 60_000L
        // With +-20% jitter: range is [48_000L, 72_000L]
        val delay = lastFailure!!.retryScheduledMillis
        assertTrue(
            "Cap backoff delay $delay must be within [48000, 72000]",
            delay in 48000L..72000L
        )
    }

    @Test
    fun syncPendingData_whenExceptionThrown_marksFailedAndSchedulesRetry() = runTest(testDispatcher) {
        val locations = listOf(sampleLocationEntity(5L))
        coEvery { locationDao.getPendingLocationsSnapshot(50) } returns locations
        coEvery { geofenceEventDao.getPendingEventsSnapshot(50) } returns emptyList()
        coEvery { syncApi.uploadBatch(any()) } throws java.io.IOException("Socket timeout during transmission")

        val result = syncRepository.syncPendingData(50)

        assertTrue(result is SyncResult.Failure)
        val failure = result as SyncResult.Failure
        assertEquals("Socket timeout during transmission", failure.error.message)

        coVerify(exactly = 1) { locationDao.updateSyncStatus(listOf(5L), SyncStatus.FAILED.name) }
    }

    @Test
    fun executePruning_purgesSyncedDataOlderThanThreshold() = runTest(testDispatcher) {
        val threshold = 1700000000L
        coEvery { locationDao.deleteSyncedLocationsOlderThan(threshold) } returns 45
        coEvery { geofenceEventDao.deleteSyncedEventsOlderThan(threshold) } returns 12

        val totalPruned = syncRepository.executePruning(threshold)

        assertEquals(57, totalPruned)
        coVerify(exactly = 1) { locationDao.deleteSyncedLocationsOlderThan(threshold) }
        coVerify(exactly = 1) { geofenceEventDao.deleteSyncedEventsOlderThan(threshold) }
    }

    @Test
    fun isNetworkAvailable_delegatesToConnectivityWatcher() = runTest(testDispatcher) {
        every { connectivityWatcher.isOnline } returns flowOf(true, false, true)

        syncRepository.isNetworkAvailable().test {
            assertTrue(awaitItem())
            assertEquals(false, awaitItem())
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
