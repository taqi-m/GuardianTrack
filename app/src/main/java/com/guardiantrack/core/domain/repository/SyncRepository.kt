package com.guardiantrack.core.domain.repository

import com.guardiantrack.core.model.sync.SyncEngineState
import com.guardiantrack.core.model.sync.SyncResult
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    fun observeSyncEngineState(): Flow<SyncEngineState>
    suspend fun syncPendingData(batchSize: Int = 50): SyncResult
    suspend fun executePruning(olderThanMillis: Long): Int
    fun isNetworkAvailable(): Flow<Boolean>
}
