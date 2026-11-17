package com.guardiantrack.core.data.repository

import com.guardiantrack.core.domain.repository.OemSurvivalRepository
import com.guardiantrack.core.model.oem.OemDiagnosticReport
import com.guardiantrack.core.model.oem.OemIntentSpec
import com.guardiantrack.core.oem.OemSurvivalEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OemSurvivalRepositoryImpl @Inject constructor(
    private val oemSurvivalEngine: OemSurvivalEngine
) : OemSurvivalRepository {

    private val _diagnostics = MutableStateFlow(oemSurvivalEngine.getOemDiagnosticReport())

    override fun observeOemDiagnostics(): Flow<OemDiagnosticReport> {
        _diagnostics.value = oemSurvivalEngine.getOemDiagnosticReport()
        return _diagnostics.asStateFlow()
    }

    override fun isBatteryOptimizationExempted(): Boolean {
        val exempted = oemSurvivalEngine.isBatteryOptimizationExempted()
        _diagnostics.value = oemSurvivalEngine.getOemDiagnosticReport()
        return exempted
    }

    override fun getLaunchableOemIntents(): List<OemIntentSpec> {
        return oemSurvivalEngine.getOemIntentSpecs()
    }
}
