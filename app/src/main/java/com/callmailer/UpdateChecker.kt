package com.callmailer

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val API = "https://api.github.com/repos/lwg2105/ORC_CallMailer/releases/latest"
    const val RELEASES_URL = "https://github.com/lwg2105/ORC_CallMailer/releases/latest"

    fun checkAsync(
        currentVersion: Int,
        onUpdate: (latestVersion: Int, apkUrl: String) -> Unit,
        onUpToDate: ((currentVersion: Int) -> Unit)? = null,
        onError: ((message: String) -> Unit)? = null
    ) {
        Thread {
            try {
                val conn = URL(API).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "CallMailer-Android/$currentVersion")
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val code = conn.responseCode
                if (code != 200) {
                    Handler(Looper.getMainLooper()).post { onError?.invoke("서버 응답 $code") }
                    return@Thread
                }
                val json = conn.inputStream.bufferedReader().readText()
                val obj = JSONObject(json)
                val tag = obj.getString("tag_name")
                val latest = tag.removePrefix("v").toIntOrNull()
                    ?: run { Handler(Looper.getMainLooper()).post { onError?.invoke("버전 파싱 실패") }; return@Thread }
                val assets = obj.getJSONArray("assets")
                val url = if (assets.length() > 0)
                    assets.getJSONObject(0).getString("browser_download_url")
                else obj.getString("html_url")
                Handler(Looper.getMainLooper()).post {
                    if (latest > currentVersion) onUpdate(latest, url)
                    else onUpToDate?.invoke(currentVersion)
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { onError?.invoke(e.message ?: "네트워크 오류") }
            }
        }.start()
    }
}