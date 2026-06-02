package com.callmailer

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.util.*

class ScheduledUploadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        Thread { scanAndSendAll(); scheduleNext(applicationContext); stopSelf() }.start()
        return START_NOT_STICKY
    }

    private fun scanAndSendAll() {
        val prefs = PreferencesHelper(this)
        val folder = File(prefs.watchFolder)
        if (!folder.exists()) { prefs.recordError("감시 폴더 없음: ${prefs.watchFolder}"); return }
        val files = folder.listFiles { f -> f.extension.equals("m4a", ignoreCase = true) }
        if (files == null) { prefs.recordError("폴더 읽기 실패 (권한 확인 필요): ${prefs.watchFolder}"); return }
        val config = SmtpConfig(
            host = prefs.smtpHost, port = prefs.smtpPort,
            senderEmail = prefs.smtpUser, senderPassword = prefs.smtpPassword,
            recipientEmail = prefs.recipientEmail
        )
        for (file in files) {
            if (prefs.isFileProcessed(file.name)) {
                prefs.addHistoryEntryIfAbsent(file.name, "sent", file.lastModified())
                continue
            }
            if (!prefs.passesNameFilter(file.name)) {
                prefs.addHistoryEntry(file.name, "skipped"); continue
            }
            try {
                sendCallRecording(config, file)
                prefs.markFileProcessed(file.name)
                prefs.addHistoryEntry(file.name, "sent")
            } catch (e: Exception) {
                prefs.recordError("발송 실패 [${file.name}]: ${e.message}")
            }
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "scheduled_upload"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null)
            nm.createNotificationChannel(NotificationChannel(channelId, "예약 업로드", NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("통화녹음 메일 발송 중")
            .setSmallIcon(android.R.drawable.ic_dialog_email).build()
    }

    companion object {
        private const val NOTIF_ID = 2

        fun scheduleAlarm(context: Context, hour: Int, minute: Int) {
            val am = context.getSystemService(ALARM_SERVICE) as AlarmManager
            val pi = pendingIntent(context)
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }

        fun cancelAlarm(context: Context) {
            (context.getSystemService(ALARM_SERVICE) as AlarmManager).cancel(pendingIntent(context))
        }

        private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
            context, 0, Intent(context, ScheduledAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun scheduleNext(context: Context) {
            val p = PreferencesHelper(context)
            scheduleAlarm(context, p.scheduledHour, p.scheduledMinute)
        }
    }
}