package com.orc.callmailer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "CallRecordingService"
private const val CHANNEL_ID = "orc_callmailer_channel"
private const val NOTIF_ID = 1001

class CallRecordingService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var prefs: PreferencesHelper
    private var fileObserver: FileObserver? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesHelper(this)
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("감시 중..."))
        startWatching()
    }

    private fun startWatching() {
        val folder = File(prefs.watchFolder)
        if (!folder.exists()) {
            folder.mkdirs()
            Log.w(TAG, "Watch folder created: ${folder.absolutePath}")
        }

        // FileObserver.CLOSE_WRITE fires when a file finishes being written
        fileObserver = object : FileObserver(folder, CLOSE_WRITE) {
            override fun onEvent(event: Int, path: String?) {
                if (path == null) return
                if (!path.endsWith(".m4a", ignoreCase = true) &&
                    !path.endsWith(".mp3", ignoreCase = true) &&
                    !path.endsWith(".aac", ignoreCase = true)
                ) return

                val file = File(folder, path)
                Log.i(TAG, "New file detected: ${file.name}")
                updateNotification("새 파일 감지: ${file.name}")
                sendFile(file)
            }
        }.also { it.startWatching() }

        Log.i(TAG, "Watching: ${folder.absolutePath}")
    }

    private fun sendFile(file: File) {
        scope.launch {
            if (!prefs.isConfigured()) {
                Log.e(TAG, "SMTP not configured — skipping ${file.name}")
                return@launch
            }

            // Wait briefly in case the file is still being finalized
            kotlinx.coroutines.delay(2000)

            val config = SmtpConfig(
                host = prefs.smtpHost,
                port = prefs.smtpPort,
                senderEmail = prefs.senderEmail,
                senderPassword = prefs.senderPassword,
                recipientEmail = prefs.recipientEmail,
            )

            try {
                sendCallRecording(config, file)
                updateNotification("전송 완료: ${file.name}")
                Log.i(TAG, "Email sent for ${file.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send ${file.name}: ${e.message}", e)
                updateNotification("전송 실패: ${file.name}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ORC 통화녹음 전송",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "통화녹음 파일 자동 이메일 전송 서비스" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ORC 통화녹음 에이전트")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fileObserver?.stopWatching()
        job.cancel()
        super.onDestroy()
    }
}
