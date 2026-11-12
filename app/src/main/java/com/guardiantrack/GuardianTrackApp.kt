package com.guardiantrack

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GuardianTrackApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel 1: Resilient Location Foreground Service (Low importance to prevent sound spam)
            val trackingChannel = NotificationChannel(
                CHANNEL_LOCATION_TRACKING,
                getString(R.string.notification_channel_tracking_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_tracking_description)
                setShowBadge(false)
            }

            // Channel 2: Geofence Breach Alerts (High importance for immediate user awareness)
            val geofenceChannel = NotificationChannel(
                CHANNEL_GEOFENCE_ALERTS,
                getString(R.string.notification_channel_geofence_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_geofence_description)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(trackingChannel)
            notificationManager.createNotificationChannel(geofenceChannel)
        }
    }

    companion object {
        const val CHANNEL_LOCATION_TRACKING = "guardian_location_tracking_channel"
        const val CHANNEL_GEOFENCE_ALERTS = "guardian_geofence_alerts_channel"
    }
}
