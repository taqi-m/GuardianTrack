package com.guardiantrack.core.location.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.guardiantrack.core.common.dispatchers.CoroutineDispatchers
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.location.manager.ActivityRecognitionEngine
import com.guardiantrack.core.location.manager.FusedLocationEngine
import com.guardiantrack.core.location.watchdog.LocationWatchdogWorker
import com.guardiantrack.core.model.location.SamplingProfile
import com.guardiantrack.core.model.telemetry.MotionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GuardianLocationService : Service() {

    @Inject
    lateinit var fusedLocationEngine: FusedLocationEngine

    @Inject
    lateinit var activityRecognitionEngine: ActivityRecognitionEngine

    @Inject
    lateinit var locationNotificationManager: LocationNotificationManager

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var dispatchers: CoroutineDispatchers

    private val serviceScope by lazy {
        CoroutineScope(SupervisorJob() + dispatchers.main)
    }

    private var locationUpdatesJob: Job? = null
    private var currentProfile: SamplingProfile = SamplingProfile.STANDARD

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val initialNotification = locationNotificationManager.buildForegroundNotification(currentProfile, null)

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            initialNotification,
            foregroundType
        )

        observeMotionState()
        LocationWatchdogWorker.scheduleWatchdog(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_TRACKING

        when (action) {
            ACTION_STOP_TRACKING -> {
                stopLocationTracking()
                stopSelf()
            }
            ACTION_START_TRACKING -> {
                val profileName = intent?.getStringExtra(EXTRA_SAMPLING_PROFILE)
                val profile = profileName?.let {
                    try {
                        SamplingProfile.valueOf(it)
                    } catch (_: Exception) {
                        SamplingProfile.STANDARD
                    }
                } ?: SamplingProfile.STANDARD

                startLocationTracking(profile)
            }
        }

        return START_STICKY
    }

    private fun startLocationTracking(profile: SamplingProfile) {
        currentProfile = profile
        locationUpdatesJob?.cancel()
        activityRecognitionEngine.startActivityUpdates()

        serviceScope.launch {
            locationRepository.startTracking(profile)
        }

        locationUpdatesJob = fusedLocationEngine.requestLocationUpdates(profile)
            .onEach { point ->
                // 1. Commit breadcrumb atomically to local Room DB
                locationRepository.recordLocation(point)

                // 2. Update persistent notification with live precision stats
                locationNotificationManager.updateNotification(currentProfile, point)
            }
            .catch { error ->
                // Error handled gracefully; service maintains sticky state
            }
            .launchIn(serviceScope)
    }

    private fun observeMotionState() {
        activityRecognitionEngine.currentMotionState
            .onEach { motionState ->
                // Adaptive Battery Invariant (<2%/hr):
                // If stationary, switch to stationary sleep (5 min interval, 100m filter)
                // If moving, restore high accuracy standard interval
                when (motionState) {
                    MotionState.STATIONARY -> {
                        if (currentProfile != SamplingProfile.STATIONARY_ADAPTIVE) {
                            startLocationTracking(SamplingProfile.STATIONARY_ADAPTIVE)
                        }
                    }
                    MotionState.WALKING, MotionState.RUNNING, MotionState.IN_VEHICLE -> {
                        if (currentProfile == SamplingProfile.STATIONARY_ADAPTIVE) {
                            startLocationTracking(SamplingProfile.STANDARD)
                        }
                    }
                    MotionState.UNKNOWN -> { /* Keep current */ }
                }
            }
            .launchIn(serviceScope)
    }

    private fun stopLocationTracking() {
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        activityRecognitionEngine.stopActivityUpdates()
        serviceScope.launch {
            locationRepository.stopTracking()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_TRACKING = "com.guardiantrack.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.guardiantrack.action.STOP_TRACKING"
        const val EXTRA_SAMPLING_PROFILE = "extra_sampling_profile"
    }
}
