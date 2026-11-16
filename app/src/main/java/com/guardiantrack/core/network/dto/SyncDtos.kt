package com.guardiantrack.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationPayloadDto(
    @SerialName("client_id") val clientId: Long,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("altitude") val altitude: Double? = null,
    @SerialName("accuracy_meters") val accuracyMeters: Float,
    @SerialName("speed_kmh") val speedKmh: Float,
    @SerialName("bearing_degrees") val bearingDegrees: Float? = null,
    @SerialName("timestamp_millis") val timestampMillis: Long,
    @SerialName("provider") val provider: String,
    @SerialName("is_mocked") val isMocked: Boolean = false
)

@Serializable
data class GeofenceEventPayloadDto(
    @SerialName("client_id") val clientId: Long,
    @SerialName("geofence_id") val geofenceId: String,
    @SerialName("transition_type") val transitionType: String,
    @SerialName("trigger_latitude") val triggerLatitude: Double? = null,
    @SerialName("trigger_longitude") val triggerLongitude: Double? = null,
    @SerialName("trigger_accuracy") val triggerAccuracy: Float? = null,
    @SerialName("timestamp_millis") val timestampMillis: Long
)

@Serializable
data class SyncBatchRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("batch_timestamp") val batchTimestamp: Long,
    @SerialName("locations") val locations: List<LocationPayloadDto>,
    @SerialName("geofence_events") val geofenceEvents: List<GeofenceEventPayloadDto>
)

@Serializable
data class SyncBatchResponse(
    @SerialName("status") val status: String,
    @SerialName("synced_locations_count") val syncedLocationsCount: Int,
    @SerialName("synced_events_count") val syncedEventsCount: Int,
    @SerialName("server_timestamp") val serverTimestamp: Long
)
