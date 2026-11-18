package com.guardiantrack.presentation.oem

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardiantrack.core.domain.repository.OemSurvivalRepository
import com.guardiantrack.core.model.oem.OemIntentSpec
import com.guardiantrack.core.oem.OemSurvivalEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class OemWizardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val oemSurvivalRepository: OemSurvivalRepository,
    private val oemSurvivalEngine: OemSurvivalEngine
) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<OemWizardUiState> = combine(
        oemSurvivalRepository.observeOemDiagnostics(),
        _userMessage
    ) { report, msg ->
        OemWizardUiState(
            diagnosticReport = report,
            launchableIntents = oemSurvivalRepository.getLaunchableOemIntents(),
            userMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OemWizardUiState()
    )

    fun onEvent(event: OemWizardUiEvent) {
        when (event) {
            is OemWizardUiEvent.RequestSystemExemptionClicked -> requestSystemExemption()
            is OemWizardUiEvent.LaunchOemIntentClicked -> launchOemIntent(event.spec)
            is OemWizardUiEvent.RefreshStatus -> refresh()
            is OemWizardUiEvent.DismissMessage -> _userMessage.value = null
        }
    }

    private fun requestSystemExemption() {
        val intent = oemSurvivalEngine.createStandardExemptionIntent()
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            _userMessage.value = "Failed to launch battery settings: ${e.message}"
        }
    }

    private fun launchOemIntent(spec: OemIntentSpec) {
        val launched = oemSurvivalEngine.launchOemSetting(spec)
        if (!launched) {
            _userMessage.value = "Manufacturer setting activity not available on this device"
        }
    }

    private fun refresh() {
        // Triggers re-check of PowerManager state
        val report = oemSurvivalEngine.getOemDiagnosticReport()
        _userMessage.value = if (report.isBatteryOptimizationIgnored) "Exemption active!" else "Not exempted yet"
    }
}
