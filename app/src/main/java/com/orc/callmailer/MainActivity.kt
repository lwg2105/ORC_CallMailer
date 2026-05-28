package com.orc.callmailer

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: PreferencesHelper
    private lateinit var switchAlwaysOn: Switch
    private lateinit var tvModeDesc: TextView
    private lateinit var layoutSchedule: LinearLayout
    private lateinit var btnPickTime: Button
    private lateinit var tvStatus: TextView
    private lateinit var btnToggleService: Button
    private var serviceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PreferencesHelper(this)

        switchAlwaysOn = findViewById(R.id.switchAlwaysOn)
        tvModeDesc = findViewById(R.id.tvModeDesc)
        layoutSchedule = findViewById(R.id.layoutSchedule)
        btnPickTime = findViewById(R.id.btnPickTime)
        tvStatus = findViewById(R.id.tvStatus)
        btnToggleService = findViewById(R.id.btnToggleService)

        switchAlwaysOn.isChecked = prefs.alwaysOnMode
        updateModeUI(prefs.alwaysOnMode)
        updateTimeButton()

        switchAlwaysOn.setOnCheckedChangeListener { _, checked ->
            prefs.alwaysOnMode = checked
            updateModeUI(checked)
        }

        btnPickTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                prefs.scheduledHour = h
                prefs.scheduledMinute = m
                updateTimeButton()
                if (serviceRunning && !prefs.alwaysOnMode)
                    ScheduledUploadService.scheduleAlarm(this, h, m)
            }, prefs.scheduledHour, prefs.scheduledMinute, true).show()
        }

        btnToggleService.setOnClickListener {
            if (serviceRunning) doStop() else doStart()
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun updateModeUI(alwaysOn: Boolean) {
        if (alwaysOn) {
            tvModeDesc.text = "새 녹음 파일을 즉시 감지하여 발송합니다."
            layoutSchedule.visibility = android.view.View.GONE
        } else {
            tvModeDesc.text = "지정한 시각에 미발송 파일을 일괄 발송합니다."
            layoutSchedule.visibility = android.view.View.VISIBLE
        }
    }

    private fun updateTimeButton() {
        btnPickTime.text = "%02d:%02d".format(prefs.scheduledHour, prefs.scheduledMinute)
    }

    private fun doStart() {
        if (prefs.alwaysOnMode) {
            startForegroundService(Intent(this, CallRecordingService::class.java))
        } else {
            ScheduledUploadService.scheduleAlarm(this, prefs.scheduledHour, prefs.scheduledMinute)
        }
        serviceRunning = true
        tvStatus.text = if (prefs.alwaysOnMode) "감시 중" else "%02d:%02d 예약 등록됨".format(prefs.scheduledHour, prefs.scheduledMinute)
        btnToggleService.text = "서비스 중지"
    }

    private fun doStop() {
        stopService(Intent(this, CallRecordingService::class.java))
        ScheduledUploadService.cancelAlarm(this)
        serviceRunning = false
        tvStatus.text = "서비스 중지됨"
        btnToggleService.text = "서비스 시작"
    }
}