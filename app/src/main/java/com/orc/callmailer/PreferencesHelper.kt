package com.orc.callmailer

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("orc_prefs", Context.MODE_PRIVATE)

    var smtpHost: String get() = prefs.getString("smtp_host", "") ?: ""; set(v) = prefs.edit().putString("smtp_host", v).apply()
    var smtpPort: Int get() = prefs.getInt("smtp_port", 587); set(v) = prefs.edit().putInt("smtp_port", v).apply()
    var smtpUser: String get() = prefs.getString("smtp_user", "") ?: ""; set(v) = prefs.edit().putString("smtp_user", v).apply()
    var smtpPassword: String get() = prefs.getString("smtp_password", "") ?: ""; set(v) = prefs.edit().putString("smtp_password", v).apply()
    var recipientEmail: String get() = prefs.getString("recipient_email", "") ?: ""; set(v) = prefs.edit().putString("recipient_email", v).apply()
    var watchFolder: String get() = prefs.getString("watch_folder", "/storage/emulated/0/Recordings/Call") ?: "/storage/emulated/0/Recordings/Call"; set(v) = prefs.edit().putString("watch_folder", v).apply()
    var emailSubjectPrefix: String get() = prefs.getString("email_subject_prefix", "[ORC통화녹음]") ?: "[ORC통화녹음]"; set(v) = prefs.edit().putString("email_subject_prefix", v).apply()

    var alwaysOnMode: Boolean get() = prefs.getBoolean("always_on_mode", true); set(v) = prefs.edit().putBoolean("always_on_mode", v).apply()
    var scheduledHour: Int get() = prefs.getInt("scheduled_hour", 8); set(v) = prefs.edit().putInt("scheduled_hour", v).apply()
    var scheduledMinute: Int get() = prefs.getInt("scheduled_minute", 0); set(v) = prefs.edit().putInt("scheduled_minute", v).apply()

    var nameFilterEnabled: Boolean get() = prefs.getBoolean("name_filter_enabled", false); set(v) = prefs.edit().putBoolean("name_filter_enabled", v).apply()
    var filterNames: List<String>
        get() = (prefs.getString("filter_names", "") ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        set(v) = prefs.edit().putString("filter_names", v.joinToString(",")).apply()

    var isServiceRunning: Boolean get() = prefs.getBoolean("service_running", false); set(v) = prefs.edit().putBoolean("service_running", v).apply()

    var lastError: String get() = prefs.getString("last_error", "") ?: ""; set(v) = prefs.edit().putString("last_error", v).apply()
    var lastErrorTime: Long get() = prefs.getLong("last_error_time", 0L); set(v) = prefs.edit().putLong("last_error_time", v).apply()

    fun recordError(msg: String) {
        lastError = msg
        lastErrorTime = System.currentTimeMillis()
    }

    fun clearError() {
        lastError = ""
        lastErrorTime = 0L
    }

    private val processedKey = "processed_files"
    fun markFileProcessed(filename: String) {
        val set = prefs.getStringSet(processedKey, mutableSetOf())!!.toMutableSet()
        set.add(filename)
        val trimmed = if (set.size > 1000) set.toList().takeLast(1000).toMutableSet() else set
        prefs.edit().putStringSet(processedKey, trimmed).apply()
    }
    fun isFileProcessed(filename: String): Boolean =
        prefs.getStringSet(processedKey, emptySet())!!.contains(filename)

    fun extractCallerName(filename: String): String? {
        val stem = filename.substringBeforeLast(".")
        val parts = stem.split("_")
        if (parts.size < 3) return null
        val p0 = parts[0].trim()
        val p1 = parts[1].trim()
        val p2 = parts[2].trim()
        return when {
            p0.length == 8 && p0.all { it.isDigit() } -> parts.drop(2).joinToString("_").trim()
            p1.length == 6 && p1.all { it.isDigit() } &&
            p2.length == 6 && p2.all { it.isDigit() } ->
                p0.removePrefix("통화 녹음").trim().ifEmpty { null }
            else -> null
        }
    }

    fun passesNameFilter(filename: String): Boolean {
        if (!nameFilterEnabled) return true
        val names = filterNames
        if (names.isEmpty()) return true
        val callerName = extractCallerName(filename) ?: return false
        return names.any { callerName.contains(it, ignoreCase = true) }
    }
}