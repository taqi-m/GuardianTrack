package com.guardiantrack.core.location.manager

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.location.SamplingProfile
import com.guardiantrack.core.model.sync.SyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FusedLocationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(profile: SamplingProfile): Flow<LocationPoint> = callbackFlow {
        val priority = when (profile) {
            SamplingProfile.STANDARD -> Priority.PRIORITY_HIGH_ACCURACY
            SamplingProfile.ECO -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            SamplingProfile.STATIONARY_ADAPTIVE -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val request = LocationRequest.Builder(priority, profile.intervalMillis)
            .setMinUpdateIntervalMillis(profile.fastestIntervalMillis)
            .setMinUpdateDistanceMeters(profile.displacementMeters)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val point = location.toDomainPoint()
                    trySend(point)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        ).addOnFailureListener { error ->
            close(error)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    private fun Location.toDomainPoint(): LocationPoint {
        val calculatedSpeed = if (hasSpeed()) speed * 3.6f else 0f // Convert m/s to km/h
        val detectedProvider = when {
            provider?.equals(android.location.LocationManager.GPS_PROVIDER, ignoreCase = true) == true -> LocationProvider.GPS
            provider?.equals(android.location.LocationManager.NETWORK_PROVIDER, ignoreCase = true) == true -> LocationProvider.NETWORK_CELL_WIFI
            else -> LocationProvider.FUSED
        }

        val isMock = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            isMock
        } else {
            @Suppress("DEPRECATION")
            isFromMockProvider
        }

        return LocationPoint(
            latitude = latitude,
            longitude = longitude,
            altitude = if (hasAltitude()) altitude else null,
            accuracyMeters = accuracy,
            speedKmh = calculatedSpeed,
            bearingDegrees = if (hasBearing()) bearing else null,
            timestampMillis = time,
            provider = detectedProvider,
            syncStatus = SyncStatus.PENDING,
            isMocked = isMock
        )
    }
}
