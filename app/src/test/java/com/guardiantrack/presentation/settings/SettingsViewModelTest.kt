package com.guardiantrack.presentation.settings

import app.cash.turbine.test
import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.domain.repository.SyncRepository
import com.guardiantrack.core.model.location.SamplingProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = CoroutineDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
        unconfined = testDispatcher
    )

    private val syncRepository: SyncRepository = mockk(relaxed = true)
    private val locationRepository: LocationRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            syncRepository = syncRepository,
            locationRepository = locationRepository,
            dispatchers = dispatchers
        )
    }

    @Test
    fun initialState_emitsDefaultSettings() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(SamplingProfile.STANDARD, state.selectedProfile)
            assertEquals(50, state.batchUploadSize)
            assertEquals(7, state.dataRetentionDays)
            assertNull(state.userMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profileChanged_updatesSelectedProfile() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(SettingsUiEvent.ProfileChanged(SamplingProfile.ECO))
            val updated = awaitItem()
            assertEquals(SamplingProfile.ECO, updated.selectedProfile)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun batchSizeChanged_updatesBatchUploadSize() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(SettingsUiEvent.BatchSizeChanged(100))
            val updated = awaitItem()
            assertEquals(100, updated.batchUploadSize)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun triggerCompactionClicked_prunesOldDataAndUpdatesUserMessage() = runTest(testDispatcher) {
        coEvery { syncRepository.executePruning(any()) } returns 87

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(SettingsUiEvent.TriggerCompactionClicked)
            advanceUntilIdle()

            val updated = awaitItem()
            assertEquals("Pruned 87 synced breadcrumbs older than 7 days", updated.userMessage)
            coVerify(exactly = 1) { syncRepository.executePruning(match { it < System.currentTimeMillis() }) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dismissMessage_clearsUserMessage() = runTest(testDispatcher) {
        coEvery { syncRepository.executePruning(any()) } returns 87

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(SettingsUiEvent.TriggerCompactionClicked)
            advanceUntilIdle()
            val withMsg = awaitItem()
            assertEquals("Pruned 87 synced breadcrumbs older than 7 days", withMsg.userMessage)

            viewModel.onEvent(SettingsUiEvent.DismissMessage)
            advanceUntilIdle()
            val cleared = awaitItem()
            assertNull(cleared.userMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
