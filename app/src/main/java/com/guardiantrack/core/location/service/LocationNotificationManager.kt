package com.guardiantrack.core.location.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.guardiantrack.GuardianTrackApp
import com.guardiantrack.R
import com.guardiantrack.core.model.location.LocationPoint
import com.guardiantrack.core.model.location.SamplingProfile
import com.guardiantrack.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun buildForegroundNotification(
        profile: SamplingProfile = SamplingProfile.STANDARD,
        latestLocation: LocationPoint? = null
    ): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(context, GuardianLocationService::class.java).apply {
            action = GuardianLocationService.ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val profileLabel = when (profile) {
            SamplingProfile.STANDARD -> "Active (10s)"
            SamplingProfile.ECO -> "Eco (60s)"
            SamplingProfile.STATIONARY_ADAPTIVE -> "Stationary Sleep (5m)"
        }

        val contentText = if (latestLocation != null) {
            String.format(
                Locale.US,
                "Accuracy: ±%.1fm • Speed: %.1f km/h • %s",
                latestLocation.accuracyMeters,
                latestLocation.speedKmh,
                latestLocation.provider.name
            )
        } else {
            "Acquiring high-precision GPS lock..."
        }

        return NotificationCompat.Builder(context, GuardianTrackApp.CHANNEL_LOCATION_TRACKING)
            .setContentTitle("GuardianTrack • $profileLabel")
            .setContentText(contentText)
            .setSubText("Low Power Engine (<2%/hr)")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Tracking", stopPendingIntent)
            .build()
    }

    fun updateNotification(profile: SamplingProfile, latestLocation: LocationPoint?) {
        val notification = buildForegroundNotification(profile, latestLocation)
        notificationManager.notify(GuardianLocationService.NOTIFICATION_ID, notification)
    }
}
