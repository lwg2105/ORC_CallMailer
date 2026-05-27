package com.orc.callmailer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: PreferencesHelper

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filterValues { !it }.keys
        if (denied.isEmpty()) {
            startService()
        } else {
            Toast.makeText(this, "권한 필요: ${denied.joinToString()}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PreferencesHelper(this)

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btnToggleService).setOnClickListener {
            if (prefs.serviceEnabled) {
                stopService(Intent(this, CallRecordingService::class.java))
                prefs.serviceEnabled = false
                updateStatus()
            } else {
                if (!prefs.isConfigured()) {
                    Toast.makeText(this, "먼저 설정을 완료하세요", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, SettingsActivity::class.java))
                    return@setOnClickListener
                }
                requestPermissionsAndStart()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnToggle = findViewById<Button>(R.id.btnToggleService)
        val configured = prefs.isConfigured()
        val enabled = prefs.serviceEnabled

        tvStatus.text = when {
            !configured -> "설정 미완료"
            enabled -> "서비스 실행 중 ▶\n감시 폴더: ${prefs.watchFolder}\n수신 메일: ${prefs.recipientEmail}"
            else -> "서비스 중지됨 ■"
        }
        btnToggle.text = if (enabled) "서비스 중지" else "서비스 시작"
    }

    private fun requestPermissionsAndStart() {
        val needed = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) needed.add(Manifest.permission.READ_MEDIA_AUDIO)

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (needed.isEmpty()) {
            startService()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun startService() {
        prefs.serviceEnabled = true
        val intent = Intent(this, CallRecordingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateStatus()
    }
}
