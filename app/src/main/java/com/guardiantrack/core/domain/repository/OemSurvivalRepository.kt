package com.guardiantrack.core.domain.repository

import com.guardiantrack.core.model.oem.OemDiagnosticReport
import com.guardiantrack.core.model.oem.OemIntentSpec
import kotlinx.coroutines.flow.Flow

interface OemSurvivalRepository {
    fun observeOemDiagnostics(): Flow<OemDiagnosticReport>
    fun isBatteryOptimizationExempted(): Boolean
    fun getLaunchableOemIntents(): List<OemIntentSpec>
}
