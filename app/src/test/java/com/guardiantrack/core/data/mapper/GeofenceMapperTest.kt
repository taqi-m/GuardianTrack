package com.guardiantrack.core.data.mapper

import com.guardiantrack.core.database.entity.GeofenceEntity
import com.guardiantrack.core.database.entity.GeofenceEventEntity
import com.guardiantrack.core.model.geofence.GeofenceEventRecord
import com.guardiantrack.core.model.geofence.GeofenceTransitionType
import com.guardiantrack.core.model.geofence.GeofenceZone
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.sync.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceMapperTest {

    @Test
    fun geofenceEntityToDomain_parsesTransitionCsvCorrectly() {
        val entity = GeofenceEntity(
            id = "geo-001",
            name = "Headquarters",
            latitude = 37.7749,
            longitude = -122.4194,
            radiusMeters = 150f,
            transitionTypesCsv = "ENTER,EXIT,DWELL",
            loiteringDelayMs = 45000L,
            expirationDurationMs = -1L,
            isActive = true,
            createdAtMillis = 1700000000000L
        )

        val domain = entity.toDomain()

        assertEquals("geo-001", domain.id)
        assertEquals("Headquarters", domain.name)
        assertEquals(3, domain.transitionTypes.size)
        assertTrue(domain.transitionTypes.contains(GeofenceTransitionType.ENTER))
        assertTrue(domain.transitionTypes.contains(GeofenceTransitionType.EXIT))
        assertTrue(domain.transitionTypes.contains(GeofenceTransitionType.DWELL))
        assertEquals(45000L, domain.loiteringDelayMillis)
        assertTrue(domain.isActive)
    }

    @Test
    fun geofenceDomainToEntity_formatsTransitionCsvCorrectly() {
        val domain = GeofenceZone(
            id = "geo-002",
            name = "Depot Zone",
            latitude = 34.0522,
            longitude = -118.2437,
            radiusMeters = 300f,
            transitionTypes = setOf(GeofenceTransitionType.ENTER, GeofenceTransitionType.EXIT),
            loiteringDelayMillis = 30000L,
            expirationDurationMillis = 86400000L,
            isActive = false,
            createdAtMillis = 1700000100000L
        )

        val entity = domain.toEntity()

        assertEquals("geo-002", entity.id)
        assertEquals("Depot Zone", entity.name)
        assertTrue(entity.transitionTypesCsv.contains("ENTER"))
        assertTrue(entity.transitionTypesCsv.contains("EXIT"))
        assertEquals(false, entity.isActive)
    }

    @Test
    fun geofenceEventEntityToDomain_mapsTriggerLocation() {
        val entity = GeofenceEventEntity(
            id = 12L,
            geofenceId = "geo-001",
            transitionType = "ENTER",
            triggerLatitude = 37.7750,
            triggerLongitude = -122.4190,
            triggerAccuracy = 5.0f,
            timestampMillis = 1700000200000L,
            syncStatus = "PENDING"
        )

        val domain = entity.toDomain()

        assertEquals(12L, domain.id)
        assertEquals("geo-001", domain.geofenceId)
        assertEquals(GeofenceTransitionType.ENTER, domain.transitionType)
        assertNotNull(domain.triggerLocation)
        assertEquals(37.7750, domain.triggerLocation?.latitude ?: 0.0, 0.0001)
        assertEquals(5.0f, domain.triggerLocation?.accuracyMeters ?: 0f)
        assertEquals(SyncStatus.PENDING, domain.syncStatus)
    }
}
