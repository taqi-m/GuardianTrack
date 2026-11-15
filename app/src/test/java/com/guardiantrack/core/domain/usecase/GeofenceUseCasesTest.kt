package com.guardiantrack.core.domain.usecase

import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.domain.repository.GeofenceRepository
import com.guardiantrack.core.domain.usecase.geofence.AddGeofenceZoneUseCase
import com.guardiantrack.core.domain.usecase.geofence.ObserveGeofencesUseCase
import com.guardiantrack.core.domain.usecase.geofence.RemoveGeofenceZoneUseCase
import com.guardiantrack.core.model.geofence.GeofenceTransitionType
import com.guardiantrack.core.model.geofence.GeofenceZone
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

class GeofenceUseCasesTest {

    private lateinit var repository: GeofenceRepository

    @Before
    fun setUp() {
        repository = mockk()
    }

    @Test
    fun addGeofenceZone_delegatesToRepository() = runTest {
        val zone = GeofenceZone(
            id = "test-zone-1",
            name = "Campus Area",
            latitude = 37.4220,
            longitude = -122.0841,
            radiusMeters = 200f,
            transitionTypes = setOf(GeofenceTransitionType.ENTER, GeofenceTransitionType.EXIT)
        )
        coEvery { repository.addGeofence(zone) } returns Result.Success(Unit)
        val useCase = AddGeofenceZoneUseCase(repository)

        val result = useCase(zone)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.addGeofence(zone) }
    }

    @Test
    fun removeGeofenceZone_delegatesToRepository() = runTest {
        coEvery { repository.removeGeofence("test-zone-1") } returns Result.Success(Unit)
        val useCase = RemoveGeofenceZoneUseCase(repository)

        val result = useCase("test-zone-1")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.removeGeofence("test-zone-1") }
    }

    @Test
    fun observeGeofences_emitsActiveList() = runTest {
        val mockZones = listOf(
            GeofenceZone(
                id = "z1",
                name = "Office",
                latitude = 0.0,
                longitude = 0.0,
                radiusMeters = 100f,
                transitionTypes = setOf(GeofenceTransitionType.ENTER)
            )
        )
        every { repository.observeAllGeofences() } returns flowOf(mockZones)
        val useCase = ObserveGeofencesUseCase(repository)

        val emitted = useCase().first()

        assertEquals(1, emitted.size)
        assertEquals("Office", emitted.first().name)
    }

    @Test
    fun observeGeofenceEvents_emitsRecentEventsFromRepository() = runTest {
        val mockEvents = listOf(
            com.guardiantrack.core.model.geofence.GeofenceEventRecord(
                id = 1L,
                geofenceId = "z1",
                transitionType = GeofenceTransitionType.ENTER,
                timestampMillis = 1700000000L
            )
        )
        every { repository.observeRecentGeofenceEvents(50) } returns flowOf(mockEvents)
        val useCase = com.guardiantrack.core.domain.usecase.geofence.ObserveGeofenceEventsUseCase(repository)

        val events = useCase(50).first()

        assertEquals(1, events.size)
        assertEquals("z1", events.first().geofenceId)
    }
}
