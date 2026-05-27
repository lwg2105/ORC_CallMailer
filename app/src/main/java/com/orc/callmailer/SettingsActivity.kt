package com.orc.callmailer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: PreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = PreferencesHelper(this)

        val etSmtpHost = findViewById<EditText>(R.id.etSmtpHost)
        val etSmtpPort = findViewById<EditText>(R.id.etSmtpPort)
        val etSenderEmail = findViewById<EditText>(R.id.etSenderEmail)
        val etSenderPassword = findViewById<EditText>(R.id.etSenderPassword)
        val etRecipientEmail = findViewById<EditText>(R.id.etRecipientEmail)
        val etWatchFolder = findViewById<EditText>(R.id.etWatchFolder)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Load current values
        etSmtpHost.setText(prefs.smtpHost)
        etSmtpPort.setText(prefs.smtpPort.toString())
        etSenderEmail.setText(prefs.senderEmail)
        etSenderPassword.setText(prefs.senderPassword)
        etRecipientEmail.setText(prefs.recipientEmail)
        etWatchFolder.setText(prefs.watchFolder)

        btnSave.setOnClickListener {
            prefs.smtpHost = etSmtpHost.text.toString().trim()
            prefs.smtpPort = etSmtpPort.text.toString().toIntOrNull() ?: 587
            prefs.senderEmail = etSenderEmail.text.toString().trim()
            prefs.senderPassword = etSenderPassword.text.toString()
            prefs.recipientEmail = etRecipientEmail.text.toString().trim()
            prefs.watchFolder = etWatchFolder.text.toString().trim()

            Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
