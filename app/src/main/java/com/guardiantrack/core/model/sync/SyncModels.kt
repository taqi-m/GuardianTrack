package com.guardiantrack.core.model.sync

enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

sealed interface SyncResult {
    data class Success(val pointsSynced: Int, val eventsSynced: Int, val timestamp: Long) : SyncResult
    data class Failure(val error: Throwable, val retryScheduledMillis: Long? = null) : SyncResult
    data object NoDataPending : SyncResult
    data object OfflineSkipped : SyncResult
}

enum class SyncEngineState {
    IDLE,
    WAITING_FOR_NETWORK,
    SYNCING,
    BACKOFF_WAIT,
    ERROR
}
