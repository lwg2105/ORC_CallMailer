package com.callmailer

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.URL

object UpdateChecker {
    private const val API = "https://api.github.com/repos/lwg2105/ORC_CallMailer/releases/latest"

    fun checkAsync(
        currentVersion: Int,
        onUpdate: (latestVersion: Int, apkUrl: String) -> Unit,
        onUpToDate: ((currentVersion: Int) -> Unit)? = null,
        onError: (() -> Unit)? = null
    ) {
        Thread {
            try {
                val conn = URL(API).openConnection()
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val json = conn.getInputStream().bufferedReader().readText()
                val obj = JSONObject(json)
                val tag = obj.getString("tag_name")
                val latest = tag.removePrefix("v").toIntOrNull()
                    ?: run { Handler(Looper.getMainLooper()).post { onError?.invoke() }; return@Thread }
                val assets = obj.getJSONArray("assets")
                val url = if (assets.length() > 0)
                    assets.getJSONObject(0).getString("browser_download_url")
                else
                    obj.getString("html_url")
                Handler(Looper.getMainLooper()).post {
                    if (latest > currentVersion) onUpdate(latest, url)
                    else onUpToDate?.invoke(currentVersion)
                }
            } catch (_: Exception) {
                Handler(Looper.getMainLooper()).post { onError?.invoke() }
            }
        }.start()
    }
}