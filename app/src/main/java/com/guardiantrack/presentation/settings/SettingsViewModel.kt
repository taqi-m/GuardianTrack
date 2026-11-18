package com.guardiantrack.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.domain.repository.SyncRepository
import com.guardiantrack.core.model.location.SamplingProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val locationRepository: LocationRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.ProfileChanged -> {
                _uiState.value = _uiState.value.copy(selectedProfile = event.profile)
            }
            is SettingsUiEvent.BatchSizeChanged -> {
                _uiState.value = _uiState.value.copy(batchUploadSize = event.size)
            }
            is SettingsUiEvent.TriggerCompactionClicked -> pruneOldData()
            is SettingsUiEvent.DismissMessage -> {
                _uiState.value = _uiState.value.copy(userMessage = null)
            }
        }
    }

    private fun pruneOldData() {
        viewModelScope.launch(dispatchers.io) {
            val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000
            val threshold = System.currentTimeMillis() - sevenDaysMillis
            val pruned = syncRepository.executePruning(threshold)
            _uiState.value = _uiState.value.copy(userMessage = "Pruned $pruned synced breadcrumbs older than 7 days")
        }
    }
}
