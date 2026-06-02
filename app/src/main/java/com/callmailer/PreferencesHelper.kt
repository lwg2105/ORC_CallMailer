package com.callmailer

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val filename: String,
    val callerName: String?,
    val timestamp: Long,
    val status: String  // "sent" | "skipped"
)

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

    fun recordError(msg: String) { lastError = msg; lastErrorTime = System.currentTimeMillis() }
    fun clearError() { lastError = ""; lastErrorTime = 0L }

    // --- 발송 완료 파일 추적 ---
    private val processedKey = "processed_files"
    fun markFileProcessed(filename: String) {
        val set = prefs.getStringSet(processedKey, mutableSetOf())!!.toMutableSet()
        set.add(filename)
        val trimmed = if (set.size > 1000) set.toList().takeLast(1000).toMutableSet() else set
        prefs.edit().putStringSet(processedKey, trimmed).apply()
    }
    fun isFileProcessed(filename: String): Boolean =
        prefs.getStringSet(processedKey, emptySet())!!.contains(filename)
    fun clearProcessedFiles() =
        prefs.edit().putStringSet(processedKey, mutableSetOf()).apply()

    // --- 이력 로그 ---
    private val historyKey = "history_log"
    private val historyMigratedKey = "history_migrated"
    private val maxHistory = 200

    // processedFiles Set → 이력으로 1회 마이그레이션 (파일이 삭제돼도 복원 가능)
    fun migrateProcessedFilesToHistory() {
        if (prefs.getBoolean(historyMigratedKey, false)) return
        val processed = prefs.getStringSet(processedKey, emptySet())!!
        if (processed.isEmpty()) { prefs.edit().putBoolean(historyMigratedKey, true).apply(); return }
        val historyFilenames = getHistory().map { it.filename }.toSet()
        for (filename in processed) {
            if (!historyFilenames.contains(filename)) {
                addHistoryEntry(filename, "sent", -1L)  // -1 = 날짜 불명 (이전 기록)
            }
        }
        prefs.edit().putBoolean(historyMigratedKey, true).apply()
    }

    fun addHistoryEntry(filename: String, status: String, timestamp: Long = System.currentTimeMillis()) {
        val callerName = extractCallerName(filename)
        val arr = try { JSONArray(prefs.getString(historyKey, "[]")) } catch (_: Exception) { JSONArray() }
        val entry = JSONObject().apply {
            put("fn", filename)
            if (callerName != null) put("cn", callerName) else put("cn", JSONObject.NULL)
            put("ts", timestamp)
            put("st", status)
        }
        arr.put(entry)
        val trimmed = if (arr.length() > maxHistory) {
            val newArr = JSONArray()
            for (i in (arr.length() - maxHistory) until arr.length()) newArr.put(arr.get(i))
            newArr
        } else arr
        prefs.edit().putString(historyKey, trimmed.toString()).apply()
    }

    fun addHistoryEntryIfAbsent(filename: String, status: String, timestamp: Long) {
        val arr = try { JSONArray(prefs.getString(historyKey, "[]")) } catch (_: Exception) { JSONArray() }
        for (i in 0 until arr.length()) {
            if (arr.getJSONObject(i).getString("fn") == filename) return
        }
        addHistoryEntry(filename, status, timestamp)
    }

    fun getHistory(): List<HistoryEntry> {
        val arr = try { JSONArray(prefs.getString(historyKey, "[]")) } catch (_: Exception) { JSONArray() }
        val list = mutableListOf<HistoryEntry>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(HistoryEntry(
                filename = obj.getString("fn"),
                callerName = if (obj.isNull("cn")) null else obj.getString("cn"),
                timestamp = obj.getLong("ts"),
                status = obj.getString("st")
            ))
        }
        return list.reversed()
    }

    fun clearHistory() {
        prefs.edit().putString(historyKey, "[]")
            .putBoolean(historyMigratedKey, false)  // 초기화 시 마이그레이션 플래그도 리셋
            .apply()
    }

    // --- 파일명 파싱 ---
    fun extractCallerName(filename: String): String? {
        val stem = filename.substringBeforeLast(".")
        val parts = stem.split("_")
        if (parts.size < 3) return null
        val p0 = parts[0].trim(); val p1 = parts[1].trim(); val p2 = parts[2].trim()
        return when {
            p0.length == 8 && p0.all { it.isDigit() } -> parts.drop(2).joinToString("_").trim()
            p1.length == 6 && p1.all { it.isDigit() } && p2.length == 6 && p2.all { it.isDigit() } ->
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