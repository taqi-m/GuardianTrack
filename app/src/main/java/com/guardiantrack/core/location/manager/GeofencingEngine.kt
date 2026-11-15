package com.guardiantrack.core.location.manager

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.guardiantrack.core.common.result.Result
import com.guardiantrack.core.location.receiver.GeofenceBroadcastReceiver
import com.guardiantrack.core.model.geofence.GeofenceTransitionType
import com.guardiantrack.core.model.geofence.GeofenceZone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofencingEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geofencingClient: GeofencingClient
) {

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 2002, intent, flags)
    }

    @SuppressLint("MissingPermission")
    suspend fun registerGeofences(zones: List<GeofenceZone>): Result<Unit> {
        if (zones.isEmpty()) return Result.Success(Unit)

        val geofenceList = zones.map { zone ->
            var transitionMask = 0
            if (zone.transitionTypes.contains(GeofenceTransitionType.ENTER)) {
                transitionMask = transitionMask or Geofence.GEOFENCE_TRANSITION_ENTER
            }
            if (zone.transitionTypes.contains(GeofenceTransitionType.EXIT)) {
                transitionMask = transitionMask or Geofence.GEOFENCE_TRANSITION_EXIT
            }
            if (zone.transitionTypes.contains(GeofenceTransitionType.DWELL)) {
                transitionMask = transitionMask or Geofence.GEOFENCE_TRANSITION_DWELL
            }

            Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(zone.latitude, zone.longitude, zone.radiusMeters)
                .setExpirationDuration(if (zone.expirationDurationMillis <= 0) Geofence.NEVER_EXPIRE else zone.expirationDurationMillis)
                .setTransitionTypes(transitionMask)
                .setLoiteringDelay(zone.loiteringDelayMillis.toInt())
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofences(geofenceList)
            .build()

        return try {
            geofencingClient.addGeofences(request, geofencePendingIntent).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun removeGeofences(geofenceIds: List<String>): Result<Unit> {
        if (geofenceIds.isEmpty()) return Result.Success(Unit)
        return try {
            geofencingClient.removeGeofences(geofenceIds).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
