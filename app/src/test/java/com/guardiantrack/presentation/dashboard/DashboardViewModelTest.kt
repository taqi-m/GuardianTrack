package com.guardiantrack.presentation.dashboard

import android.content.Context
import androidx.core.content.ContextCompat
import app.cash.turbine.test
import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.domain.repository.OemSurvivalRepository
import com.guardiantrack.core.domain.repository.SyncRepository
import com.guardiantrack.core.domain.repository.TelemetryRepository
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.location.SamplingProfile
import com.guardiantrack.core.model.location.TrackingState
import com.guardiantrack.core.model.sync.SyncResult
import com.guardiantrack.core.model.telemetry.MotionState
import com.guardiantrack.core.model.telemetry.TelemetryStats
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = CoroutineDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
        unconfined = testDispatcher
    )

    private val context: Context = mockk(relaxed = true)
    private val locationRepository: LocationRepository = mockk(relaxed = true)
    private val syncRepository: SyncRepository = mockk(relaxed = true)
    private val oemSurvivalRepository: OemSurvivalRepository = mockk(relaxed = true)
    private val telemetryRepository: TelemetryRepository = mockk(relaxed = true)

    private val trackingStateFlow = MutableStateFlow(TrackingState.STOPPED)
    private val latestLocationFlow = MutableStateFlow<LocationPoint?>(null)
    private val breadcrumbsFlow = MutableStateFlow<List<LocationPoint>>(emptyList())
    private val pendingCountFlow = MutableStateFlow(0)
    private val totalCountFlow = MutableStateFlow(0)
    private val telemetryFlow = MutableStateFlow(
        TelemetryStats(
            batteryLevelPercent = 95f,
            batteryDrainPerHourPercent = 1.2f,
            isCharging = false,
            activeProvider = LocationProvider.FUSED,
            currentHorizontalAccuracyMeters = 6.5f,
            currentMotionState = MotionState.STATIONARY
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(ContextCompat::class)
        every { ContextCompat.startForegroundService(any(), any()) } returns mockk()

        every { locationRepository.observeTrackingState() } returns trackingStateFlow
        every { locationRepository.observeLatestLocation() } returns latestLocationFlow
        every { locationRepository.observeBreadcrumbs(any()) } returns breadcrumbsFlow
        every { locationRepository.countPendingLocations() } returns pendingCountFlow
        every { locationRepository.countTotalLocations() } returns totalCountFlow
        every { telemetryRepository.observeTelemetryStats() } returns telemetryFlow
        every { oemSurvivalRepository.isBatteryOptimizationExempted() } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel(): DashboardViewModel {
        return DashboardViewModel(
            context = context,
            locationRepository = locationRepository,
            syncRepository = syncRepository,
            oemSurvivalRepository = oemSurvivalRepository,
            telemetryRepository = telemetryRepository,
            dispatchers = dispatchers
        )
    }

    @Test
    fun initialState_emitsDefaultUiStateAndObservesRepositories() = runTest(testDispatcher) {
        pendingCountFlow.value = 14
        totalCountFlow.value = 120

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            // If the first item was initial default, advance scheduler to receive combined emission
            val resolvedState = if (state.pendingSyncCount == 0 && state.totalRecordedCount == 0) {
                awaitItem()
            } else {
                state
            }

            assertEquals(TrackingState.STOPPED, resolvedState.trackingState)
            assertEquals(SamplingProfile.STANDARD, resolvedState.selectedProfile)
            assertEquals(14, resolvedState.pendingSyncCount)
            assertEquals(120, resolvedState.totalRecordedCount)
            assertEquals(1.2f, resolvedState.batteryDrainPerHour, 0.01f)
            assertEquals(95f, resolvedState.batteryLevelPercent, 0.01f)
            assertTrue(resolvedState.isOemExempted)
            assertNull(resolvedState.userMessage)
        }
    }

    @Test
    fun toggleTracking_whenStopped_startsForegroundService() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Consume initial or combined emission

            viewModel.onEvent(DashboardUiEvent.ToggleTrackingClicked)
            advanceUntilIdle()

            verify(exactly = 1) { ContextCompat.startForegroundService(context, any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleTracking_whenAlreadyTracking_stopsForegroundServiceAndRepository() = runTest(testDispatcher) {
        trackingStateFlow.value = TrackingState.TRACKING_HIGH_ACCURACY
        coEvery { locationRepository.stopTracking() } returns Result.Success(Unit)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            // Wait for combined state to reflect TRACKING_HIGH_ACCURACY
            var state = awaitItem()
            if (state.trackingState != TrackingState.TRACKING_HIGH_ACCURACY) {
                state = awaitItem()
            }
            assertEquals(TrackingState.TRACKING_HIGH_ACCURACY, state.trackingState)

            viewModel.onEvent(DashboardUiEvent.ToggleTrackingClicked)
            advanceUntilIdle()

            verify(exactly = 1) { context.stopService(any()) }
            coVerify(exactly = 1) { locationRepository.stopTracking() }
        }
    }

    @Test
    fun changeSamplingProfile_updatesProfileInState() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(DashboardUiEvent.ChangeSamplingProfile(SamplingProfile.ECO))
            val updated = awaitItem()

            assertEquals(SamplingProfile.ECO, updated.selectedProfile)
        }
    }

    @Test
    fun changeSamplingProfile_whenTracking_restartsServiceWithNewProfile() = runTest(testDispatcher) {
        trackingStateFlow.value = TrackingState.TRACKING_HIGH_ACCURACY
        val viewModel = createViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            if (state.trackingState != TrackingState.TRACKING_HIGH_ACCURACY) {
                state = awaitItem()
            }

            viewModel.onEvent(DashboardUiEvent.ChangeSamplingProfile(SamplingProfile.STATIONARY_ADAPTIVE))
            val updated = awaitItem()
            assertEquals(SamplingProfile.STATIONARY_ADAPTIVE, updated.selectedProfile)
            advanceUntilIdle()

            verify(exactly = 1) { ContextCompat.startForegroundService(context, any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun forceSyncClicked_whenSuccess_updatesUserMessage() = runTest(testDispatcher) {
        coEvery { syncRepository.syncPendingData(batchSize = 50) } returns SyncResult.Success(
            pointsSynced = 35,
            eventsSynced = 2,
            timestamp = 1700000000L
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(DashboardUiEvent.ForceSyncClicked)
            val updated = awaitItem()

            assertEquals("Synced 35 breadcrumbs successfully", updated.userMessage)
        }
    }

    @Test
    fun forceSyncClicked_whenFailure_updatesUserMessageWithError() = runTest(testDispatcher) {
        coEvery { syncRepository.syncPendingData(batchSize = 50) } returns SyncResult.Failure(
            error = RuntimeException("Connection reset by peer"),
            retryScheduledMillis = 4000L
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(DashboardUiEvent.ForceSyncClicked)
            val updated = awaitItem()

            assertEquals("Sync failed: Connection reset by peer", updated.userMessage)
        }
    }

    @Test
    fun forceSyncClicked_whenNoDataPending_updatesUserMessage() = runTest(testDispatcher) {
        coEvery { syncRepository.syncPendingData(batchSize = 50) } returns SyncResult.NoDataPending

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(DashboardUiEvent.ForceSyncClicked)
            val updated = awaitItem()

            assertEquals("All data is already synchronized", updated.userMessage)
        }
    }

    @Test
    fun forceSyncClicked_whenOfflineSkipped_updatesUserMessage() = runTest(testDispatcher) {
        coEvery { syncRepository.syncPendingData(batchSize = 50) } returns SyncResult.OfflineSkipped

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(DashboardUiEvent.ForceSyncClicked)
            val updated = awaitItem()

            assertEquals("Device is offline. Queued for next connection", updated.userMessage)
        }
    }

    @Test
    fun dismissMessage_clearsUserMessage() = runTest(testDispatcher) {
        coEvery { syncRepository.syncPendingData(batchSize = 50) } returns SyncResult.NoDataPending

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(DashboardUiEvent.ForceSyncClicked)
            val withMessage = awaitItem()
            assertEquals("All data is already synchronized", withMessage.userMessage)

            viewModel.onEvent(DashboardUiEvent.DismissMessage)
            val cleared = awaitItem()
            assertNull(cleared.userMessage)
        }
    }
}
