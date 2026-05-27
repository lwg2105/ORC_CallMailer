package com.orc.callmailer

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("orc_callmailer_prefs", Context.MODE_PRIVATE)

    var smtpHost: String
        get() = prefs.getString("smtp_host", "smtp.office365.com") ?: "smtp.office365.com"
        set(value) = prefs.edit().putString("smtp_host", value).apply()

    var smtpPort: Int
        get() = prefs.getInt("smtp_port", 587)
        set(value) = prefs.edit().putInt("smtp_port", value).apply()

    var senderEmail: String
        get() = prefs.getString("sender_email", "") ?: ""
        set(value) = prefs.edit().putString("sender_email", value).apply()

    var senderPassword: String
        get() = prefs.getString("sender_password", "") ?: ""
        set(value) = prefs.edit().putString("sender_password", value).apply()

    var recipientEmail: String
        get() = prefs.getString("recipient_email", "") ?: ""
        set(value) = prefs.edit().putString("recipient_email", value).apply()

    var watchFolder: String
        get() = prefs.getString("watch_folder", "/storage/emulated/0/Recordings/Call") ?: "/storage/emulated/0/Recordings/Call"
        set(value) = prefs.edit().putString("watch_folder", value).apply()

    var serviceEnabled: Boolean
        get() = prefs.getBoolean("service_enabled", false)
        set(value) = prefs.edit().putBoolean("service_enabled", value).apply()

    fun isConfigured(): Boolean =
        senderEmail.isNotBlank() && senderPassword.isNotBlank() && recipientEmail.isNotBlank()
}
