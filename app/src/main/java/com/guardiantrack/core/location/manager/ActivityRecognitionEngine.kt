package com.guardiantrack.core.location.manager

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.DetectedActivity
import com.guardiantrack.core.location.receiver.ActivityRecognitionReceiver
import com.guardiantrack.core.model.telemetry.MotionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRecognitionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityClient: ActivityRecognitionClient
) {
    private val _currentMotionState = MutableStateFlow(MotionState.UNKNOWN)
    val currentMotionState: Flow<MotionState> = _currentMotionState.asStateFlow()

    private val activityPendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 3003, intent, flags)
    }

    @SuppressLint("MissingPermission")
    fun startActivityUpdates(intervalMillis: Long = 30_000L) {
        try {
            activityClient.requestActivityUpdates(intervalMillis, activityPendingIntent)
        } catch (_: Exception) {
            // Permission or Google Play Services fallback
        }
    }

    fun stopActivityUpdates() {
        try {
            activityClient.removeActivityUpdates(activityPendingIntent)
        } catch (_: Exception) {
        }
    }

    fun updateDetectedActivity(type: Int, confidence: Int) {
        if (confidence < 60) return

        val state = when (type) {
            DetectedActivity.STILL -> MotionState.STATIONARY
            DetectedActivity.WALKING, DetectedActivity.ON_FOOT -> MotionState.WALKING
            DetectedActivity.RUNNING -> MotionState.RUNNING
            DetectedActivity.IN_VEHICLE -> MotionState.IN_VEHICLE
            else -> MotionState.UNKNOWN
        }

        if (state != MotionState.UNKNOWN) {
            _currentMotionState.value = state
        }
    }

    fun setExplicitMotionState(state: MotionState) {
        _currentMotionState.value = state
    }
}
