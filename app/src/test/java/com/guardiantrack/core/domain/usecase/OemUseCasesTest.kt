package com.guardiantrack.core.domain.usecase

import app.cash.turbine.test
import com.guardiantrack.core.domain.repository.OemSurvivalRepository
import com.guardiantrack.core.domain.usecase.oem.CheckOemOptimizationStatusUseCase
import com.guardiantrack.core.domain.usecase.oem.GetOemIntentsUseCase
import com.guardiantrack.core.model.oem.OemDiagnosticReport
import com.guardiantrack.core.model.oem.OemIntentSpec
import com.guardiantrack.core.model.oem.OemManufacturer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OemUseCasesTest {

    private lateinit var repository: OemSurvivalRepository

    private val sampleReport = OemDiagnosticReport(
        manufacturer = OemManufacturer.XIAOMI,
        deviceModel = "Xiaomi 14",
        androidVersion = 34,
        isBatteryOptimizationIgnored = false,
        requiresAutoStartPermission = true,
        requiresBackgroundDataWhitelisting = false,
        manufacturerGuideTitle = "Xiaomi HyperOS Autostart Setup",
        manufacturerGuideSteps = listOf("Enable Autostart", "Set Battery Saver to No Restrictions")
    )

    private val sampleIntents = listOf(
        OemIntentSpec(
            targetManufacturer = OemManufacturer.XIAOMI,
            packageName = "com.miui.securitycenter",
            className = "com.miui.permcenter.autostart.AutoStartManagementActivity",
            humanReadableName = "Xiaomi Autostart Settings"
        )
    )

    @Before
    fun setUp() {
        repository = mockk()
    }

    @Test
    fun checkOemOptimizationStatus_emitsDiagnosticReport() = runTest {
        every { repository.observeOemDiagnostics() } returns flowOf(sampleReport)
        val useCase = CheckOemOptimizationStatusUseCase(repository)

        useCase().test {
            val report = awaitItem()
            assertEquals(OemManufacturer.XIAOMI, report.manufacturer)
            assertEquals("Xiaomi HyperOS Autostart Setup", report.manufacturerGuideTitle)
            assertTrue(report.requiresAutoStartPermission)
            assertFalse(report.isBatteryOptimizationIgnored)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun checkOemOptimizationStatus_isExempted_delegatesToRepository() {
        every { repository.isBatteryOptimizationExempted() } returns true
        val useCase = CheckOemOptimizationStatusUseCase(repository)

        val exempted = useCase.isExempted()

        assertTrue(exempted)
        verify(exactly = 1) { repository.isBatteryOptimizationExempted() }
    }

    @Test
    fun getOemIntents_returnsLaunchableIntentsFromRepository() {
        every { repository.getLaunchableOemIntents() } returns sampleIntents
        val useCase = GetOemIntentsUseCase(repository)

        val intents = useCase()

        assertEquals(1, intents.size)
        assertEquals("Xiaomi Autostart Settings", intents.first().humanReadableName)
        assertEquals("com.miui.securitycenter", intents.first().packageName)
        verify(exactly = 1) { repository.getLaunchableOemIntents() }
    }
}
