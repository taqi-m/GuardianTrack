package com.guardiantrack.presentation.geofence

import app.cash.turbine.test
import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.domain.repository.GeofenceRepository
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.location.manager.GeofencingEngine
import com.guardiantrack.core.model.geofence.GeofenceEventRecord
import com.guardiantrack.core.model.geofence.GeofenceTransitionType
import com.guardiantrack.core.model.geofence.GeofenceZone
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.sync.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeofenceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = CoroutineDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
        unconfined = testDispatcher
    )

    private val geofenceRepository: GeofenceRepository = mockk(relaxed = true)
    private val geofencingEngine: GeofencingEngine = mockk(relaxed = true)
    private val locationRepository: LocationRepository = mockk(relaxed = true)

    private val allGeofencesFlow = MutableStateFlow<List<GeofenceZone>>(emptyList())
    private val recentEventsFlow = MutableStateFlow<List<GeofenceEventRecord>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { geofenceRepository.observeAllGeofences() } returns allGeofencesFlow
        every { geofenceRepository.observeRecentGeofenceEvents(any()) } returns recentEventsFlow
        every { locationRepository.observeLatestLocation() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): GeofenceViewModel {
        return GeofenceViewModel(
            geofenceRepository = geofenceRepository,
            geofencingEngine = geofencingEngine,
            locationRepository = locationRepository,
            dispatchers = dispatchers
        )
    }

    @Test
    fun initialState_emitsDefaultStateWithLoadedZonesAndEvents() = runTest(testDispatcher) {
        val testZone = GeofenceZone(
            id = "geo-1",
            name = "Headquarters",
            latitude = 37.7749,
            longitude = -122.4194,
            radiusMeters = 200f,
            transitionTypes = setOf(GeofenceTransitionType.ENTER, GeofenceTransitionType.EXIT)
        )
        val testEvent = GeofenceEventRecord(
            id = 10L,
            geofenceId = "geo-1",
            transitionType = GeofenceTransitionType.ENTER,
            timestampMillis = 1700000000L,
            syncStatus = SyncStatus.SYNCED
        )
        allGeofencesFlow.value = listOf(testZone)
        recentEventsFlow.value = listOf(testEvent)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            if (state.geofences.isEmpty()) {
                state = awaitItem()
            }

            assertEquals(1, state.geofences.size)
            assertEquals("Headquarters", state.geofences.first().name)
            assertEquals(1, state.recentBreachEvents.size)
            assertEquals(GeofenceTransitionType.ENTER, state.recentBreachEvents.first().transitionType)
            assertFalse(state.isAddDialogOpen)
            assertNull(state.userMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun openAndCloseAddDialog_updatesDialogVisibility() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(GeofenceUiEvent.OpenAddDialog)
            val openState = awaitItem()
            assertTrue(openState.isAddDialogOpen)

            viewModel.onEvent(GeofenceUiEvent.CloseAddDialog)
            val closedState = awaitItem()
            assertFalse(closedState.isAddDialogOpen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun draftUpdates_updatePendingZoneFields() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(GeofenceUiEvent.NameChanged("Warehouse Sector 7"))
            val s1 = awaitItem()
            assertEquals("Warehouse Sector 7", s1.pendingZoneName)

            viewModel.onEvent(GeofenceUiEvent.LatitudeChanged("40.7128"))
            val s2 = awaitItem()
            assertEquals("40.7128", s2.pendingLatitude)

            viewModel.onEvent(GeofenceUiEvent.LongitudeChanged("-74.0060"))
            val s3 = awaitItem()
            assertEquals("-74.0060", s3.pendingLongitude)

            viewModel.onEvent(GeofenceUiEvent.RadiusChanged(350f))
            val s4 = awaitItem()
            assertEquals(350f, s4.pendingRadiusMeters, 0.01f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmAddGeofence_success_addsToRepoRegistersWithEngineSetsMessageAndClosesDialog() = runTest(testDispatcher) {
        coEvery { geofenceRepository.addGeofence(any()) } returns Result.Success(Unit)
        coEvery { geofencingEngine.registerGeofences(any()) } returns Result.Success(Unit)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(GeofenceUiEvent.OpenAddDialog)
            awaitItem()

            viewModel.onEvent(GeofenceUiEvent.NameChanged("Safe Harbor"))
            awaitItem()
            viewModel.onEvent(GeofenceUiEvent.LatitudeChanged("34.0522"))
            awaitItem()
            viewModel.onEvent(GeofenceUiEvent.LongitudeChanged("-118.2437"))
            awaitItem()
            viewModel.onEvent(GeofenceUiEvent.RadiusChanged(250f))
            awaitItem()

            viewModel.onEvent(GeofenceUiEvent.ConfirmAddGeofence)
            advanceUntilIdle()

            val successState = awaitItem()
            assertEquals("Geofence registered with hardware subsystem", successState.userMessage)
            assertFalse(successState.isAddDialogOpen)

            coVerify(exactly = 1) {
                geofenceRepository.addGeofence(match {
                    it.name == "Safe Harbor" &&
                    it.latitude == 34.0522 &&
                    it.longitude == -118.2437 &&
                    it.radiusMeters == 250f
                })
            }
            coVerify(exactly = 1) { geofencingEngine.registerGeofences(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmAddGeofence_invalidInput_setsValidationErrorMessage() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial default
            awaitItem() // combined initial

            viewModel.onEvent(GeofenceUiEvent.NameChanged(""))
            val blankNameState = awaitItem()
            assertEquals("", blankNameState.pendingZoneName)

            viewModel.onEvent(GeofenceUiEvent.ConfirmAddGeofence)
            advanceUntilIdle()
            val errorState = awaitItem()

            assertEquals("Please provide valid coordinates and a name", errorState.userMessage)
            coVerify(exactly = 0) { geofenceRepository.addGeofence(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmAddGeofence_hardwareWarning_setsHardwareWarningMessage() = runTest(testDispatcher) {
        coEvery { geofenceRepository.addGeofence(any()) } returns Result.Success(Unit)
        coEvery { geofencingEngine.registerGeofences(any()) } returns Result.Error(Exception("Max geofence limit reached"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(GeofenceUiEvent.NameChanged("Perimeter B"))
            awaitItem()

            viewModel.onEvent(GeofenceUiEvent.ConfirmAddGeofence)
            advanceUntilIdle()

            val warningState = awaitItem()
            assertTrue(warningState.userMessage?.contains("Hardware geofence registration warning: Max geofence limit reached") == true)
            assertFalse(warningState.isAddDialogOpen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteGeofence_removesFromEngineAndRepoAndSetsMessage() = runTest(testDispatcher) {
        coEvery { geofencingEngine.removeGeofences(listOf("geo-99")) } returns Result.Success(Unit)
        coEvery { geofenceRepository.removeGeofence("geo-99") } returns Result.Success(Unit)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(GeofenceUiEvent.DeleteGeofence("geo-99"))
            advanceUntilIdle()

            val deletedState = awaitItem()
            assertEquals("Geofence removed", deletedState.userMessage)

            coVerify(exactly = 1) { geofencingEngine.removeGeofences(listOf("geo-99")) }
            coVerify(exactly = 1) { geofenceRepository.removeGeofence("geo-99") }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleActive_whenDeactivating_updatesRepoAndRemovesFromHardwareEngine() = runTest(testDispatcher) {
        coEvery { geofenceRepository.updateGeofenceStatus("geo-10", false) } returns Result.Success(Unit)
        coEvery { geofencingEngine.removeGeofences(listOf("geo-10")) } returns Result.Success(Unit)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(GeofenceUiEvent.ToggleActive("geo-10", false))
            advanceUntilIdle()

            coVerify(exactly = 1) { geofenceRepository.updateGeofenceStatus("geo-10", false) }
            coVerify(exactly = 1) { geofencingEngine.removeGeofences(listOf("geo-10")) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleActive_whenActivating_updatesRepoAndRegistersWithHardwareEngine() = runTest(testDispatcher) {
        val mockZone = GeofenceZone(
            id = "geo-10",
            name = "Depot",
            latitude = 37.77,
            longitude = -122.41,
            radiusMeters = 100f,
            transitionTypes = setOf(GeofenceTransitionType.ENTER)
        )
        coEvery { geofenceRepository.updateGeofenceStatus("geo-10", true) } returns Result.Success(Unit)
        every { geofenceRepository.getGeofenceById("geo-10") } returns flowOf(mockZone)
        coEvery { geofencingEngine.registerGeofences(listOf(mockZone)) } returns Result.Success(Unit)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onEvent(GeofenceUiEvent.ToggleActive("geo-10", true))
            advanceUntilIdle()

            coVerify(exactly = 1) { geofenceRepository.updateGeofenceStatus("geo-10", true) }
            coVerify(exactly = 1) { geofencingEngine.registerGeofences(listOf(mockZone)) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dismissMessage_clearsUserMessage() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial default
            awaitItem() // combined initial

            viewModel.onEvent(GeofenceUiEvent.NameChanged(""))
            val blankNameState = awaitItem()
            assertEquals("", blankNameState.pendingZoneName)

            viewModel.onEvent(GeofenceUiEvent.ConfirmAddGeofence)
            advanceUntilIdle()
            val errorState = awaitItem()
            assertEquals("Please provide valid coordinates and a name", errorState.userMessage)

            viewModel.onEvent(GeofenceUiEvent.DismissMessage)
            advanceUntilIdle()
            val clearedState = awaitItem()
            assertNull(clearedState.userMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun useCurrentLocationClicked_whenLocationAvailable_populatesDraftCoordinates() = runTest(testDispatcher) {
        val testLocation = LocationPoint(
            id = 1L,
            latitude = 37.7749295,
            longitude = -122.419416,
            accuracyMeters = 5f,
            speedKmh = 0f,
            timestampMillis = 1700000000L,
            provider = LocationProvider.GPS
        )
        every { locationRepository.observeLatestLocation() } returns flowOf(testLocation)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial default
            awaitItem() // combined initial

            viewModel.onEvent(GeofenceUiEvent.UseCurrentLocationClicked)
            advanceUntilIdle()

            val updatedState = awaitItem()
            assertEquals("37.77493", updatedState.pendingLatitude)
            assertEquals("-122.41942", updatedState.pendingLongitude)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun useCurrentLocationClicked_whenLocationNull_setsUserMessage() = runTest(testDispatcher) {
        every { locationRepository.observeLatestLocation() } returns flowOf(null)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial default
            awaitItem() // combined initial

            viewModel.onEvent(GeofenceUiEvent.UseCurrentLocationClicked)
            advanceUntilIdle()

            val updatedState = awaitItem()
            assertEquals("Acquiring GPS fix... please try again shortly", updatedState.userMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
