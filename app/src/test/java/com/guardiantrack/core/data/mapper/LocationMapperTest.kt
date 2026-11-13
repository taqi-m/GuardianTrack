package com.guardiantrack.core.data.mapper

import com.guardiantrack.core.database.entity.LocationEntity
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.sync.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationMapperTest {

    @Test
    fun entityToDomain_mapsAllFieldsAccurately() {
        val entity = LocationEntity(
            id = 42L,
            latitude = 37.7749,
            longitude = -122.4194,
            altitude = 15.5,
            accuracyMeters = 4.2f,
            speedKmh = 25.8f,
            bearingDegrees = 180.0f,
            timestampMillis = 1700000000000L,
            provider = LocationProvider.GPS.name,
            syncStatus = SyncStatus.PENDING.name,
            isMocked = false
        )

        val domain = entity.toDomain()

        assertEquals(42L, domain.id)
        assertEquals(37.7749, domain.latitude, 0.0001)
        assertEquals(-122.4194, domain.longitude, 0.0001)
        assertEquals(15.5, domain.altitude ?: 0.0, 0.0001)
        assertEquals(4.2f, domain.accuracyMeters)
        assertEquals(25.8f, domain.speedKmh)
        assertEquals(180.0f, domain.bearingDegrees ?: 0f)
        assertEquals(1700000000000L, domain.timestampMillis)
        assertEquals(LocationProvider.GPS, domain.provider)
        assertEquals(SyncStatus.PENDING, domain.syncStatus)
        assertFalse(domain.isMocked)
    }

    @Test
    fun domainToEntity_mapsAllFieldsAccurately() {
        val domain = LocationPoint(
            id = 99L,
            latitude = 40.7128,
            longitude = -74.0060,
            altitude = 10.0,
            accuracyMeters = 6.5f,
            speedKmh = 45.0f,
            bearingDegrees = 90.0f,
            timestampMillis = 1700000050000L,
            provider = LocationProvider.FUSED,
            syncStatus = SyncStatus.SYNCED,
            isMocked = true
        )

        val entity = domain.toEntity()

        assertEquals(99L, entity.id)
        assertEquals(40.7128, entity.latitude, 0.0001)
        assertEquals(-74.0060, entity.longitude, 0.0001)
        assertEquals(10.0, entity.altitude ?: 0.0, 0.0001)
        assertEquals(6.5f, entity.accuracyMeters)
        assertEquals(45.0f, entity.speedKmh)
        assertEquals(90.0f, entity.bearingDegrees ?: 0f)
        assertEquals(1700000050000L, entity.timestampMillis)
        assertEquals("FUSED", entity.provider)
        assertEquals("SYNCED", entity.syncStatus)
        assertTrue(entity.isMocked)
    }
}
