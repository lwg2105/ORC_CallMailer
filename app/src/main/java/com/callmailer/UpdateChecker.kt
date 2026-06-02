package com.callmailer

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.URL

object UpdateChecker {
    private const val API = "https://api.github.com/repos/lwg2105/ORC_CallMailer/releases/latest"

    fun checkAsync(currentVersion: Int, onUpdate: (latestVersion: Int, apkUrl: String) -> Unit) {
        Thread {
            try {
                val json = URL(API).readText()
                val obj = JSONObject(json)
                val tag = obj.getString("tag_name")          // "v42"
                val latest = tag.removePrefix("v").toIntOrNull() ?: return@Thread
                if (latest <= currentVersion) return@Thread
                val assets = obj.getJSONArray("assets")
                val url = if (assets.length() > 0)
                    assets.getJSONObject(0).getString("browser_download_url")
                else
                    obj.getString("html_url")
                Handler(Looper.getMainLooper()).post { onUpdate(latest, url) }
            } catch (_: Exception) {}
        }.start()
    }
}