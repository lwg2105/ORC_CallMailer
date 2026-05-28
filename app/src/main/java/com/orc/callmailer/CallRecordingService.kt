package com.orc.callmailer

import android.app.*
import android.content.Intent
import android.os.FileObserver
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File

class CallRecordingService : Service() {
    private var fileObserver: FileObserver? = null
    private lateinit var prefs: PreferencesHelper

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        prefs = PreferencesHelper(this)
        startForeground(NOTIF_ID, buildNotification())
        catchUpMissedFiles()
        startWatching()
        return START_STICKY
    }

    private fun smtpConfig() = SmtpConfig(
        host = prefs.smtpHost,
        port = prefs.smtpPort,
        senderEmail = prefs.smtpUser,
        senderPassword = prefs.smtpPassword,
        recipientEmail = prefs.recipientEmail
    )

    private fun catchUpMissedFiles() {
        val folder = File(prefs.watchFolder)
        if (!folder.exists()) return
        val files = folder.listFiles { f -> f.extension.equals("m4a", ignoreCase = true) } ?: return
        Thread {
            for (file in files) {
                if (prefs.isFileProcessed(file.name)) continue
                if (!prefs.passesNameFilter(file.name)) continue
                try {
                    sendCallRecording(smtpConfig(), file)
                    prefs.markFileProcessed(file.name)
                } catch (_: Exception) {}
            }
        }.start()
    }

    private fun startWatching() {
        val path = prefs.watchFolder
        fileObserver = object : FileObserver(path, CLOSE_WRITE) {
            override fun onEvent(event: Int, filePath: String?) {
                filePath ?: return
                if (!filePath.endsWith(".m4a", ignoreCase = true)) return
                val file = File(path, filePath)
                val name = file.name
                if (prefs.isFileProcessed(name)) return
                if (!prefs.passesNameFilter(name)) return
                Thread {
                    try {
                        sendCallRecording(smtpConfig(), file)
                        prefs.markFileProcessed(name)
                    } catch (_: Exception) {}
                }.start()
            }
        }
        fileObserver?.startWatching()
    }

    override fun onDestroy() {
        fileObserver?.stopWatching()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "recording_watch"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "녹음 감시", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("통화녹음 감시 중")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setOngoing(true)
            .build()
    }

    companion object { private const val NOTIF_ID = 1 }
}