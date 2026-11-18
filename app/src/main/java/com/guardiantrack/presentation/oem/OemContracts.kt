package com.guardiantrack.presentation.oem

import androidx.compose.runtime.Immutable
import com.guardiantrack.core.model.oem.OemDiagnosticReport
import com.guardiantrack.core.model.oem.OemIntentSpec
import com.guardiantrack.core.model.oem.OemManufacturer

@Immutable
data class OemWizardUiState(
    val diagnosticReport: OemDiagnosticReport = OemDiagnosticReport(
        manufacturer = OemManufacturer.GENERIC,
        deviceModel = "Android Device",
        androidVersion = 34,
        isBatteryOptimizationIgnored = false,
        requiresAutoStartPermission = false,
        requiresBackgroundDataWhitelisting = false,
        manufacturerGuideTitle = "Battery Optimization Setup",
        manufacturerGuideSteps = emptyList()
    ),
    val launchableIntents: List<OemIntentSpec> = emptyList(),
    val userMessage: String? = null
)

sealed interface OemWizardUiEvent {
    data object RequestSystemExemptionClicked : OemWizardUiEvent
    data class LaunchOemIntentClicked(val spec: OemIntentSpec) : OemWizardUiEvent
    data object RefreshStatus : OemWizardUiEvent
    data object DismissMessage : OemWizardUiEvent
}
