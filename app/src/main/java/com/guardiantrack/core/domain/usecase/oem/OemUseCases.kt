package com.guardiantrack.core.domain.usecase.oem

import com.guardiantrack.core.domain.repository.OemSurvivalRepository
import com.guardiantrack.core.model.oem.OemDiagnosticReport
import com.guardiantrack.core.model.oem.OemIntentSpec
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckOemOptimizationStatusUseCase @Inject constructor(
    private val oemSurvivalRepository: OemSurvivalRepository
) {
    operator fun invoke(): Flow<OemDiagnosticReport> {
        return oemSurvivalRepository.observeOemDiagnostics()
    }

    fun isExempted(): Boolean {
        return oemSurvivalRepository.isBatteryOptimizationExempted()
    }
}

class GetOemIntentsUseCase @Inject constructor(
    private val oemSurvivalRepository: OemSurvivalRepository
) {
    operator fun invoke(): List<OemIntentSpec> {
        return oemSurvivalRepository.getLaunchableOemIntents()
    }
}
