package com.callmailer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = PreferencesHelper(context)
        if (prefs.alwaysOnMode) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, CallRecordingService::class.java)
            )
        } else {
            ScheduledUploadService.scheduleAlarm(context, prefs.scheduledHour, prefs.scheduledMinute)
        }
    }
}