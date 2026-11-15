package com.guardiantrack.core.location.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.domain.repository.GeofenceRepository
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.location.manager.GeofencingEngine
import com.guardiantrack.core.location.service.GuardianLocationService
import com.guardiantrack.core.model.location.TrackingState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var geofenceRepository: GeofenceRepository

    @Inject
    lateinit var geofencingEngine: GeofencingEngine

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var dispatchers: CoroutineDispatchers

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

            scope.launch {
                try {
                    // 1. Re-register all active geofences with hardware GeofencingClient upon reboot
                    val activeZones = geofenceRepository.observeActiveGeofences().first()
                    if (activeZones.isNotEmpty()) {
                        geofencingEngine.registerGeofences(activeZones)
                    }

                    // 2. Resurrect location service only if tracking was previously active
                    val trackingState = locationRepository.observeTrackingState().first()
                    if (trackingState != TrackingState.STOPPED) {
                        val serviceIntent = Intent(context, GuardianLocationService::class.java).apply {
                            this.action = GuardianLocationService.ACTION_START_TRACKING
                        }
                        try {
                            ContextCompat.startForegroundService(context, serviceIntent)
                        } catch (_: Exception) {
                            // Fallback handled by WorkManager watchdog
                        }
                    }
                } finally {
                    pendingResult.finish()
                    scope.cancel()
                }
            }
        }
    }
}
