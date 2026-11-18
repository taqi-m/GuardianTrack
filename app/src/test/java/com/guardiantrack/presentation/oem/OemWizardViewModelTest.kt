package com.guardiantrack.presentation.oem

import android.content.Context
import android.content.Intent
import app.cash.turbine.test
import com.guardiantrack.core.domain.repository.OemSurvivalRepository
import com.guardiantrack.core.model.oem.OemDiagnosticReport
import com.guardiantrack.core.model.oem.OemIntentSpec
import com.guardiantrack.core.model.oem.OemManufacturer
import com.guardiantrack.core.oem.OemSurvivalEngine
import io.mockk.every
import io.mockk.mockk
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
class OemWizardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk(relaxed = true)
    private val oemSurvivalRepository: OemSurvivalRepository = mockk(relaxed = true)
    private val oemSurvivalEngine: OemSurvivalEngine = mockk(relaxed = true)

    private val defaultReport = OemDiagnosticReport(
        manufacturer = OemManufacturer.SAMSUNG,
        deviceModel = "Samsung Galaxy S24",
        androidVersion = 34,
        isBatteryOptimizationIgnored = false,
        requiresAutoStartPermission = false,
        requiresBackgroundDataWhitelisting = true,
        manufacturerGuideTitle = "Samsung One UI Survival Setup",
        manufacturerGuideSteps = listOf("Set battery to Unrestricted")
    )
    private val oemDiagnosticsFlow = MutableStateFlow(defaultReport)

    private val sampleIntentSpec = OemIntentSpec(
        targetManufacturer = OemManufacturer.SAMSUNG,
        packageName = "com.samsung.android.lool",
        className = "com.samsung.android.sm.ui.battery.BatteryActivity",
        humanReadableName = "Samsung Battery Management"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { oemSurvivalRepository.observeOemDiagnostics() } returns oemDiagnosticsFlow
        every { oemSurvivalRepository.getLaunchableOemIntents() } returns listOf(sampleIntentSpec)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): OemWizardViewModel {
        return OemWizardViewModel(
            context = context,
            oemSurvivalRepository = oemSurvivalRepository,
            oemSurvivalEngine = oemSurvivalEngine
        )
    }

    @Test
    fun initialState_emitsDiagnosticReportAndLaunchableIntents() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            if (state.launchableIntents.isEmpty()) {
                state = awaitItem()
            }

            assertEquals(OemManufacturer.SAMSUNG, state.diagnosticReport.manufacturer)
            assertEquals("Samsung One UI Survival Setup", state.diagnosticReport.manufacturerGuideTitle)
            assertFalse(state.diagnosticReport.isBatteryOptimizationIgnored)
            assertEquals(1, state.launchableIntents.size)
            assertEquals("Samsung Battery Management", state.launchableIntents.first().humanReadableName)
            assertNull(state.userMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun requestSystemExemptionClicked_launchesExemptionIntent() = runTest(testDispatcher) {
        val dummyIntent = mockk<Intent>(relaxed = true)
        every { oemSurvivalEngine.createStandardExemptionIntent() } returns dummyIntent

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onEvent(OemWizardUiEvent.RequestSystemExemptionClicked)
            advanceUntilIdle()

            verify(exactly = 1) { oemSurvivalEngine.createStandardExemptionIntent() }
            verify(exactly = 1) { context.startActivity(dummyIntent) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun requestSystemExemptionClicked_whenException_updatesUserMessage() = runTest(testDispatcher) {
        val dummyIntent = mockk<Intent>(relaxed = true)
        every { oemSurvivalEngine.createStandardExemptionIntent() } returns dummyIntent
        every { context.startActivity(dummyIntent) } throws SecurityException("Permission denial")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(OemWizardUiEvent.RequestSystemExemptionClicked)
            advanceUntilIdle()

            val errorState = awaitItem()
            assertTrue(errorState.userMessage?.contains("Failed to launch battery settings: Permission denial") == true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun launchOemIntentClicked_whenLaunchSuccessful_doesNotSetErrorMessage() = runTest(testDispatcher) {
        every { oemSurvivalEngine.launchOemSetting(sampleIntentSpec) } returns true

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(OemWizardUiEvent.LaunchOemIntentClicked(sampleIntentSpec))
            advanceUntilIdle()

            verify(exactly = 1) { oemSurvivalEngine.launchOemSetting(sampleIntentSpec) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun launchOemIntentClicked_whenLaunchFails_setsErrorMessage() = runTest(testDispatcher) {
        every { oemSurvivalEngine.launchOemSetting(sampleIntentSpec) } returns false

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(OemWizardUiEvent.LaunchOemIntentClicked(sampleIntentSpec))
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Manufacturer setting activity not available on this device", state.userMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshStatus_whenExemptionActive_updatesUserMessage() = runTest(testDispatcher) {
        val activeReport = defaultReport.copy(isBatteryOptimizationIgnored = true)
        every { oemSurvivalEngine.getOemDiagnosticReport() } returns activeReport

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(OemWizardUiEvent.RefreshStatus)
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Exemption active!", state.userMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshStatus_whenNotExempted_updatesUserMessage() = runTest(testDispatcher) {
        val notActiveReport = defaultReport.copy(isBatteryOptimizationIgnored = false)
        every { oemSurvivalEngine.getOemDiagnosticReport() } returns notActiveReport

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(OemWizardUiEvent.RefreshStatus)
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Not exempted yet", state.userMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dismissMessage_clearsUserMessage() = runTest(testDispatcher) {
        val activeReport = defaultReport.copy(isBatteryOptimizationIgnored = true)
        every { oemSurvivalEngine.getOemDiagnosticReport() } returns activeReport

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(OemWizardUiEvent.RefreshStatus)
            advanceUntilIdle()
            val withMsg = awaitItem()
            assertEquals("Exemption active!", withMsg.userMessage)

            viewModel.onEvent(OemWizardUiEvent.DismissMessage)
            advanceUntilIdle()
            val cleared = awaitItem()
            assertNull(cleared.userMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
