package com.guardiantrack.core.location.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionResult
import com.guardiantrack.core.location.manager.ActivityRecognitionEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActivityRecognitionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var activityRecognitionEngine: ActivityRecognitionEngine

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent) ?: return
            val mostProbable = result.mostProbableActivity ?: return
            activityRecognitionEngine.updateDetectedActivity(
                type = mostProbable.type,
                confidence = mostProbable.confidence
            )
        }
    }
}
