package com.guardiantrack.core.location.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.guardiantrack.GuardianTrackApp
import com.guardiantrack.core.model.geofence.GeofenceTransitionType
import com.guardiantrack.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showGeofenceAlertNotification(
        geofenceId: String,
        transitionType: GeofenceTransitionType
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            geofenceId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val actionText = when (transitionType) {
            GeofenceTransitionType.ENTER -> "Entered Zone"
            GeofenceTransitionType.EXIT -> "Exited Zone"
            GeofenceTransitionType.DWELL -> "Dwelling in Zone"
            GeofenceTransitionType.UNKNOWN -> "Zone Boundary Crossed"
        }

        val notification = NotificationCompat.Builder(context, GuardianTrackApp.CHANNEL_GEOFENCE_ALERTS)
            .setContentTitle("Geofence Alert: $actionText")
            .setContentText("Triggered by zone: $geofenceId")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(geofenceId.hashCode(), notification)
    }
}
