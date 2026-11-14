package com.guardiantrack.core.domain.usecase

import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.domain.usecase.location.GetLocationHistoryUseCase
import com.guardiantrack.core.domain.usecase.location.ObserveTrackingStateUseCase
import com.guardiantrack.core.domain.usecase.location.StartLocationTrackingUseCase
import com.guardiantrack.core.domain.usecase.location.StopLocationTrackingUseCase
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.location.SamplingProfile
import com.guardiantrack.core.model.location.TrackingState
import com.guardiantrack.core.model.sync.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocationUseCasesTest {

    private lateinit var repository: LocationRepository

    @Before
    fun setUp() {
        repository = mockk()
    }

    @Test
    fun startLocationTracking_invokesRepositoryWithProfile() = runTest {
        coEvery { repository.startTracking(SamplingProfile.ECO) } returns Result.Success(Unit)
        val useCase = StartLocationTrackingUseCase(repository)

        val result = useCase(SamplingProfile.ECO)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.startTracking(SamplingProfile.ECO) }
    }

    @Test
    fun stopLocationTracking_invokesRepositoryStop() = runTest {
        coEvery { repository.stopTracking() } returns Result.Success(Unit)
        val useCase = StopLocationTrackingUseCase(repository)

        val result = useCase()

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.stopTracking() }
    }

    @Test
    fun getLocationHistory_emitsBreadcrumbsFromRepository() = runTest {
        val mockPoints = listOf(
            LocationPoint(
                id = 1L,
                latitude = 37.7749,
                longitude = -122.4194,
                accuracyMeters = 5.0f,
                speedKmh = 12.0f,
                timestampMillis = 1000L,
                provider = LocationProvider.GPS,
                syncStatus = SyncStatus.SYNCED
            )
        )
        every { repository.observeBreadcrumbs(50) } returns flowOf(mockPoints)
        val useCase = GetLocationHistoryUseCase(repository)

        val points = useCase(50).first()

        assertEquals(1, points.size)
        assertEquals(37.7749, points.first().latitude, 0.0001)
    }

    @Test
    fun observeTrackingState_emitsStateTransitions() = runTest {
        every { repository.observeTrackingState() } returns flowOf(TrackingState.TRACKING_HIGH_ACCURACY)
        val useCase = ObserveTrackingStateUseCase(repository)

        val state = useCase().first()

        assertEquals(TrackingState.TRACKING_HIGH_ACCURACY, state)
    }
}
