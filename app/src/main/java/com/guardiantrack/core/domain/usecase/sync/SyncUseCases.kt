package com.guardiantrack.core.domain.usecase.sync

import com.guardiantrack.core.domain.repository.SyncRepository
import com.guardiantrack.core.model.sync.SyncEngineState
import com.guardiantrack.core.model.sync.SyncResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SyncOfflineQueueUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(batchSize: Int = 50): SyncResult {
        return syncRepository.syncPendingData(batchSize)
    }
}

class PruneOldBreadcrumbsUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(retentionDurationMillis: Long = 7L * 24 * 60 * 60 * 1000): Int {
        val threshold = System.currentTimeMillis() - retentionDurationMillis
        return syncRepository.executePruning(threshold)
    }
}

class ObserveSyncStateUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(): Flow<SyncEngineState> {
        return syncRepository.observeSyncEngineState()
    }
}
