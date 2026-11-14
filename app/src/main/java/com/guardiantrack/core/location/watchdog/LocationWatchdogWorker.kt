package com.guardiantrack.core.location.watchdog

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.location.service.GuardianLocationService
import com.guardiantrack.core.model.location.TrackingState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class LocationWatchdogWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationRepository: LocationRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val currentState = locationRepository.observeTrackingState().first()

        // If tracking was requested or active, verify the service process is alive
        if (currentState != TrackingState.STOPPED) {
            val isServiceRunning = isLocationServiceRunning()
            if (!isServiceRunning) {
                val serviceIntent = Intent(appContext, GuardianLocationService::class.java).apply {
                    action = GuardianLocationService.ACTION_START_TRACKING
                }
                try {
                    ContextCompat.startForegroundService(appContext, serviceIntent)
                } catch (_: Exception) {
                    return Result.retry()
                }
            }
        }

        return Result.success()
    }

    private fun isLocationServiceRunning(): Boolean {
        val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (GuardianLocationService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    companion object {
        const val WATCHDOG_WORK_NAME = "guardian_location_watchdog_worker"

        fun scheduleWatchdog(context: Context) {
            val request = PeriodicWorkRequestBuilder<LocationWatchdogWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WATCHDOG_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancelWatchdog(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WATCHDOG_WORK_NAME)
        }
    }
}
