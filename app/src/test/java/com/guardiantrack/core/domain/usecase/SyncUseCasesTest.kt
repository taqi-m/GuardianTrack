package com.guardiantrack.core.domain.usecase

import com.guardiantrack.core.domain.repository.SyncRepository
import com.guardiantrack.core.domain.usecase.sync.PruneOldBreadcrumbsUseCase
import com.guardiantrack.core.domain.usecase.sync.SyncOfflineQueueUseCase
import com.guardiantrack.core.model.sync.SyncResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncUseCasesTest {

    private lateinit var syncRepository: SyncRepository

    @Before
    fun setUp() {
        syncRepository = mockk()
    }

    @Test
    fun syncOfflineQueue_invokesRepositoryWithBatchSize() = runTest {
        coEvery { syncRepository.syncPendingData(50) } returns SyncResult.Success(
            pointsSynced = 25,
            eventsSynced = 2,
            timestamp = 1700000000L
        )
        val useCase = SyncOfflineQueueUseCase(syncRepository)

        val result = useCase(50)

        assertTrue(result is SyncResult.Success)
        val success = result as SyncResult.Success
        assertEquals(25, success.pointsSynced)
        assertEquals(2, success.eventsSynced)
        coVerify(exactly = 1) { syncRepository.syncPendingData(50) }
    }

    @Test
    fun pruneOldBreadcrumbs_executesPruningOlderThanThreshold() = runTest {
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        coEvery { syncRepository.executePruning(any()) } returns 142
        val useCase = PruneOldBreadcrumbsUseCase(syncRepository)

        val count = useCase(sevenDaysMs)

        assertEquals(142, count)
        coVerify(exactly = 1) { syncRepository.executePruning(match { it < System.currentTimeMillis() }) }
    }

    @Test
    fun observeSyncState_emitsStateFromRepository() = runTest {
        io.mockk.every { syncRepository.observeSyncEngineState() } returns kotlinx.coroutines.flow.flowOf(
            com.guardiantrack.core.model.sync.SyncEngineState.SYNCING
        )
        val useCase = com.guardiantrack.core.domain.usecase.sync.ObserveSyncStateUseCase(syncRepository)

        val state = useCase().first()

        assertEquals(com.guardiantrack.core.model.sync.SyncEngineState.SYNCING, state)
    }
}
