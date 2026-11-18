package com.guardiantrack.presentation.settings

import androidx.compose.runtime.Immutable
import com.guardiantrack.core.model.location.SamplingProfile

@Immutable
data class SettingsUiState(
    val selectedProfile: SamplingProfile = SamplingProfile.STANDARD,
    val batchUploadSize: Int = 50,
    val dataRetentionDays: Int = 7,
    val userMessage: String? = null
)

sealed interface SettingsUiEvent {
    data class ProfileChanged(val profile: SamplingProfile) : SettingsUiEvent
    data class BatchSizeChanged(val size: Int) : SettingsUiEvent
    data object TriggerCompactionClicked : SettingsUiEvent
    data object DismissMessage : SettingsUiEvent
}
