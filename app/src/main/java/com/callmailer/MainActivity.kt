package com.callmailer

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: PreferencesHelper
    private lateinit var switchAlwaysOn: Switch
    private lateinit var tvModeDesc: TextView
    private lateinit var layoutSchedule: LinearLayout
    private lateinit var btnPickTime: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvHistorySummary: TextView
    private lateinit var btnToggleService: Button
    private lateinit var tvError: TextView
    private lateinit var btnUpdate: Button

    private var updateUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PreferencesHelper(this)

        switchAlwaysOn   = findViewById(R.id.switchAlwaysOn)
        tvModeDesc       = findViewById(R.id.tvModeDesc)
        layoutSchedule   = findViewById(R.id.layoutSchedule)
        btnPickTime      = findViewById(R.id.btnPickTime)
        tvStatus         = findViewById(R.id.tvStatus)
        tvHistorySummary = findViewById(R.id.tvHistorySummary)
        btnToggleService = findViewById(R.id.btnToggleService)
        tvError          = findViewById(R.id.tvError)
        btnUpdate        = findViewById(R.id.btnUpdate)

        switchAlwaysOn.isChecked = prefs.alwaysOnMode
        updateModeUI(prefs.alwaysOnMode)
        updateTimeButton()

        switchAlwaysOn.setOnCheckedChangeListener { _, checked ->
            prefs.alwaysOnMode = checked
            updateModeUI(checked)
        }

        btnPickTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                prefs.scheduledHour = h; prefs.scheduledMinute = m
                updateTimeButton()
                if (prefs.isServiceRunning && !prefs.alwaysOnMode)
                    ScheduledUploadService.scheduleAlarm(this, h, m)
            }, prefs.scheduledHour, prefs.scheduledMinute, true).show()
        }

        btnToggleService.setOnClickListener { if (prefs.isServiceRunning) doStop() else doStart() }
        tvError.setOnClickListener { prefs.clearError(); tvError.visibility = View.GONE }

        btnUpdate.setOnClickListener {
            if (updateUrl != null) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
            } else {
                checkForUpdate()
            }
        }

        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        checkStoragePermission()
    }

    override fun onResume() {
        super.onResume()
        updateServiceUI()
        refreshErrorDisplay()
        refreshHistorySummary()
        // 업데이트가 아직 발견되지 않은 경우에만 자동 체크
        if (updateUrl == null) checkForUpdate()
    }

    private fun checkForUpdate() {
        updateUrl = null
        btnUpdate.isEnabled = false
        btnUpdate.text = "버전 확인 중..."
        btnUpdate.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF9E9E9E.toInt())

        UpdateChecker.checkAsync(BuildConfig.VERSION_CODE,
            onUpdate = { latest, url ->
                updateUrl = url
                btnUpdate.isEnabled = true
                btnUpdate.text = "업데이트 v$latest 다운로드"
                btnUpdate.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())
            },
            onUpToDate = { current ->
                btnUpdate.isEnabled = true
                btnUpdate.text = "최신 버전 (v$current)  ↻"
                btnUpdate.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF9E9E9E.toInt())
            },
            onError = {
                btnUpdate.isEnabled = true
                btnUpdate.text = "버전 확인 실패 — 탭하여 재시도"
                btnUpdate.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF9E9E9E.toInt())
            }
        )
    }

    private fun refreshHistorySummary() {
        val history = prefs.getHistory()
        if (history.isEmpty()) { tvHistorySummary.text = ""; return }
        val sentCount = history.count { it.status == "sent" }
        val lastSent = history.firstOrNull { it.status == "sent" }
        val lastTime = if (lastSent != null)
            SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(lastSent.timestamp))
        else "-"
        tvHistorySummary.text = "누적 전송 ${sentCount}건  |  최근: $lastTime"
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "파일 접근 권한이 필요합니다. 설정에서 '모든 파일 접근 허용' 후 돌아오세요.", Toast.LENGTH_LONG).show()
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }
    }

    private fun updateModeUI(alwaysOn: Boolean) {
        tvModeDesc.text = if (alwaysOn) "새 녹음 파일을 즉시 감지하여 발송합니다." else "지정한 시각에 미발송 파일을 일괄 발송합니다."
        layoutSchedule.visibility = if (alwaysOn) View.GONE else View.VISIBLE
    }

    private fun updateTimeButton() { btnPickTime.text = "%02d:%02d".format(prefs.scheduledHour, prefs.scheduledMinute) }

    private fun updateServiceUI() {
        if (prefs.isServiceRunning) {
            tvStatus.text = if (prefs.alwaysOnMode) "감시 중" else "%02d:%02d 예약 등록됨".format(prefs.scheduledHour, prefs.scheduledMinute)
            btnToggleService.text = "서비스 중지"
        } else {
            tvStatus.text = "서비스 중지됨"
            btnToggleService.text = "서비스 시작"
        }
    }

    private fun refreshErrorDisplay() {
        val err = prefs.lastError
        if (err.isEmpty()) { tvError.visibility = View.GONE; return }
        val time = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(prefs.lastErrorTime))
        tvError.text = "⚠ 오류 ($time): $err\n[탭하여 닫기]"
        tvError.visibility = View.VISIBLE
    }

    private fun doStart() {
        if (prefs.smtpHost.isEmpty() || prefs.smtpUser.isEmpty() || prefs.recipientEmail.isEmpty()) {
            Toast.makeText(this, "설정에서 SMTP 정보를 먼저 입력하세요.", Toast.LENGTH_LONG).show(); return
        }
        prefs.isServiceRunning = true
        if (prefs.alwaysOnMode) startForegroundService(Intent(this, CallRecordingService::class.java))
        else ScheduledUploadService.scheduleAlarm(this, prefs.scheduledHour, prefs.scheduledMinute)
        updateServiceUI()
    }

    private fun doStop() {
        prefs.isServiceRunning = false
        stopService(Intent(this, CallRecordingService::class.java))
        ScheduledUploadService.cancelAlarm(this)
        updateServiceUI()
    }
}