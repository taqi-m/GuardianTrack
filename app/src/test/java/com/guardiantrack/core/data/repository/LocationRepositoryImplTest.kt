package com.guardiantrack.core.data.repository

import app.cash.turbine.test
import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.database.dao.LocationDao
import com.guardiantrack.core.database.entity.LocationEntity
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.location.SamplingProfile
import com.guardiantrack.core.model.location.TrackingState
import com.guardiantrack.core.model.sync.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = CoroutineDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
        unconfined = testDispatcher
    )

    private val locationDao: LocationDao = mockk(relaxed = true)
    private lateinit var locationRepository: LocationRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        locationRepository = LocationRepositoryImpl(
            locationDao = locationDao,
            dispatchers = dispatchers
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleEntity(id: Long) = LocationEntity(
        id = id,
        latitude = 37.7749,
        longitude = -122.4194,
        altitude = 12.0,
        accuracyMeters = 3.5f,
        speedKmh = 14.2f,
        bearingDegrees = 90.0f,
        timestampMillis = 1700000000L + id * 1000,
        provider = "GPS",
        isMocked = false,
        syncStatus = "PENDING"
    )

    private fun samplePoint(id: Long = 0L) = LocationPoint(
        id = id,
        latitude = 37.7749,
        longitude = -122.4194,
        altitude = 12.0,
        accuracyMeters = 3.5f,
        speedKmh = 14.2f,
        bearingDegrees = 90.0f,
        timestampMillis = 1700000000L,
        provider = LocationProvider.GPS,
        isMocked = false,
        syncStatus = SyncStatus.PENDING
    )

    @Test
    fun trackingState_transitionsBetweenStartAndStop() = runTest(testDispatcher) {
        locationRepository.observeTrackingState().test {
            assertEquals(TrackingState.STOPPED, awaitItem())

            val startResult = locationRepository.startTracking(SamplingProfile.ECO)
            assertTrue(startResult is Result.Success)
            assertEquals(TrackingState.TRACKING_HIGH_ACCURACY, awaitItem())

            val stopResult = locationRepository.stopTracking()
            assertTrue(stopResult is Result.Success)
            assertEquals(TrackingState.STOPPED, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recordLocation_insertsEntityIntoDaoAndReturnsId() = runTest(testDispatcher) {
        val point = samplePoint()
        coEvery { locationDao.insertLocation(any()) } returns 42L

        val result = locationRepository.recordLocation(point)

        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data)
        coVerify(exactly = 1) {
            locationDao.insertLocation(match {
                it.latitude == 37.7749 && it.longitude == -122.4194 && it.provider == "GPS"
            })
        }
    }

    @Test
    fun recordLocation_whenDaoThrows_returnsErrorResult() = runTest(testDispatcher) {
        val point = samplePoint()
        coEvery { locationDao.insertLocation(any()) } throws RuntimeException("SQLite disk full")

        val result = locationRepository.recordLocation(point)

        assertTrue(result is Result.Error)
        assertEquals("SQLite disk full", (result as Result.Error).exception.message)
    }

    @Test
    fun markLocationsAsSynced_updatesDaoWithSyncedStatus() = runTest(testDispatcher) {
        val ids = listOf(1L, 2L, 3L)

        val result = locationRepository.markLocationsAsSynced(ids)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { locationDao.updateSyncStatus(ids, SyncStatus.SYNCED.name) }
    }

    @Test
    fun markLocationsAsFailed_updatesDaoWithFailedStatus() = runTest(testDispatcher) {
        val ids = listOf(4L, 5L)

        val result = locationRepository.markLocationsAsFailed(ids)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { locationDao.updateSyncStatus(ids, SyncStatus.FAILED.name) }
    }

    @Test
    fun pruneSyncedLocations_deletesOlderThanThreshold() = runTest(testDispatcher) {
        val threshold = 1700000000L
        coEvery { locationDao.deleteSyncedLocationsOlderThan(threshold) } returns 64

        val result = locationRepository.pruneSyncedLocations(threshold)

        assertTrue(result is Result.Success)
        assertEquals(64, (result as Result.Success).data)
        coVerify(exactly = 1) { locationDao.deleteSyncedLocationsOlderThan(threshold) }
    }

    @Test
    fun observeLatestLocation_mapsDaoEntityToDomainModel() = runTest(testDispatcher) {
        every { locationDao.observeLatestLocation() } returns flowOf(sampleEntity(10L), null)

        locationRepository.observeLatestLocation().test {
            val first = awaitItem()
            assertNotNull(first)
            assertEquals(10L, first!!.id)
            assertEquals(LocationProvider.GPS, first.provider)

            val second = awaitItem()
            assertNull(second)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeBreadcrumbs_mapsEntityListToDomain() = runTest(testDispatcher) {
        every { locationDao.observeRecentBreadcrumbs(30) } returns flowOf(
            listOf(sampleEntity(1L), sampleEntity(2L))
        )

        locationRepository.observeBreadcrumbs(30).test {
            val list = awaitItem()
            assertEquals(2, list.size)
            assertEquals(1L, list[0].id)
            assertEquals(2L, list[1].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getUnsyncedLocations_queriesPendingFromDao() = runTest(testDispatcher) {
        every { locationDao.getPendingLocations(50) } returns flowOf(listOf(sampleEntity(1L)))

        locationRepository.getUnsyncedLocations(50).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(1L, list.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun countPendingAndTotal_delegatesToDao() = runTest(testDispatcher) {
        every { locationDao.countPendingLocations() } returns flowOf(15)
        every { locationDao.countTotalLocations() } returns flowOf(250)

        locationRepository.countPendingLocations().test {
            assertEquals(15, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        locationRepository.countTotalLocations().test {
            assertEquals(250, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
