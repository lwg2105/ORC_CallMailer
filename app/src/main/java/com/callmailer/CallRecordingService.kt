package com.callmailer

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
        Thread {
            catchUpMissedFiles()                    // 1. 폴더 스캔 먼저 (실제 파일 타임스탬프 사용)
            prefs.migrateProcessedFilesToHistory()  // 2. 폴더에 없는 나머지 processedFiles 소급 추가
        }.start()
        startWatching()
        return START_STICKY
    }

    private fun smtpConfig() = SmtpConfig(
        host = prefs.smtpHost, port = prefs.smtpPort,
        senderEmail = prefs.smtpUser, senderPassword = prefs.smtpPassword,
        recipientEmail = prefs.recipientEmail
    )

    private fun catchUpMissedFiles() {
        val folder = File(prefs.watchFolder)
        if (!folder.exists()) { prefs.recordError("감시 폴더 없음: ${prefs.watchFolder}"); return }
        val files = folder.listFiles { f -> f.extension.equals("m4a", ignoreCase = true) }
        if (files == null) { prefs.recordError("폴더 읽기 실패 (권한 확인 필요): ${prefs.watchFolder}"); return }
        for (file in files) {
            if (prefs.isFileProcessed(file.name)) {
                prefs.addHistoryEntryIfAbsent(file.name, "sent", file.lastModified())
                continue
            }
            if (!prefs.passesNameFilter(file.name)) {
                prefs.addHistoryEntry(file.name, "skipped"); continue
            }
            try {
                sendCallRecording(smtpConfig(), file)
                prefs.markFileProcessed(file.name)
                prefs.addHistoryEntry(file.name, "sent")
            } catch (e: Exception) {
                prefs.recordError("발송 실패 [${file.name}]: ${e.message}")
            }
        }
    }

    private fun startWatching() {
        val path = prefs.watchFolder
        val dir = File(path)
        if (!dir.exists()) return
        fileObserver = object : FileObserver(dir, CLOSE_WRITE) {
            override fun onEvent(event: Int, filePath: String?) {
                filePath ?: return
                if (!filePath.endsWith(".m4a", ignoreCase = true)) return
                val file = File(path, filePath)
                val name = file.name
                if (prefs.isFileProcessed(name)) return
                if (!prefs.passesNameFilter(name)) {
                    prefs.addHistoryEntry(name, "skipped"); return
                }
                Thread {
                    try {
                        sendCallRecording(smtpConfig(), file)
                        prefs.markFileProcessed(name)
                        prefs.addHistoryEntry(name, "sent")
                    } catch (e: Exception) {
                        prefs.recordError("발송 실패 [${name}]: ${e.message}")
                    }
                }.start()
            }
        }
        fileObserver?.startWatching()
    }

    override fun onDestroy() {
        fileObserver?.stopWatching()
        PreferencesHelper(this).isServiceRunning = false
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "recording_watch"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null)
            nm.createNotificationChannel(NotificationChannel(channelId, "녹음 감시", NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("통화녹음 감시 중")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setOngoing(true).build()
    }

    companion object { private const val NOTIF_ID = 1 }
}