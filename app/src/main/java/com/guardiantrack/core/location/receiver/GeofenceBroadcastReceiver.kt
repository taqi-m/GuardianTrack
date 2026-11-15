package com.guardiantrack.core.location.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.domain.repository.GeofenceRepository
import com.guardiantrack.core.location.service.GeofenceNotificationHelper
import com.guardiantrack.core.model.geofence.GeofenceEventRecord
import com.guardiantrack.core.model.geofence.GeofenceTransitionType
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.LocationProvider
import com.guardiantrack.core.model.sync.SyncStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var geofenceRepository: GeofenceRepository

    @Inject
    lateinit var notificationHelper: GeofenceNotificationHelper

    @Inject
    lateinit var dispatchers: CoroutineDispatchers

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        val transition = when (geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> GeofenceTransitionType.ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> GeofenceTransitionType.EXIT
            Geofence.GEOFENCE_TRANSITION_DWELL -> GeofenceTransitionType.DWELL
            else -> return
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
        val triggerLocation = geofencingEvent.triggeringLocation?.let { loc ->
            LocationPoint(
                latitude = loc.latitude,
                longitude = loc.longitude,
                altitude = loc.altitude,
                accuracyMeters = loc.accuracy,
                speedKmh = loc.speed * 3.6f,
                bearingDegrees = loc.bearing,
                timestampMillis = loc.time,
                provider = LocationProvider.FUSED,
                syncStatus = SyncStatus.PENDING
            )
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

        scope.launch {
            try {
                for (geofence in triggeringGeofences) {
                    val record = GeofenceEventRecord(
                        geofenceId = geofence.requestId,
                        transitionType = transition,
                        triggerLocation = triggerLocation,
                        timestampMillis = System.currentTimeMillis(),
                        syncStatus = SyncStatus.PENDING
                    )
                    // 1. Atomic local persistence in Room
                    geofenceRepository.recordGeofenceEvent(record)

                    // 2. Immediate zero-network local heads-up notification
                    notificationHelper.showGeofenceAlertNotification(
                        geofenceId = geofence.requestId,
                        transitionType = transition
                    )
                }
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}
