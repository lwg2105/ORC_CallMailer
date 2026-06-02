package com.callmailer

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesHelper
    private lateinit var layoutList: LinearLayout
    private lateinit var tvCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        supportActionBar?.title = "발송 이력"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefs = PreferencesHelper(this)
        layoutList = findViewById(R.id.layoutHistoryList)
        tvCount = findViewById(R.id.tvHistoryCount)

        findViewById<Button>(R.id.btnResetHistory).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("이력 초기화")
                .setMessage("발송 이력과 전송 완료 기록을 모두 삭제합니다.\n삭제 후 기존 파일이 다시 전송될 수 있습니다.\n계속하시겠습니까?")
                .setPositiveButton("초기화") { _, _ ->
                    prefs.clearProcessedFiles()
                    prefs.clearHistory()
                    refreshList()
                    Toast.makeText(this, "초기화 완료", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        refreshList()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun refreshList() {
        layoutList.removeAllViews()
        val history = prefs.getHistory()

        if (history.isEmpty()) {
            tvCount.text = "이력 없음"
            val tv = TextView(this).apply {
                text = "아직 기록된 이력이 없습니다."
                gravity = Gravity.CENTER
                setPadding(0, 48, 0, 0)
                setTextColor(Color.GRAY)
            }
            layoutList.addView(tv)
            return
        }

        val sentCount = history.count { it.status == "sent" }
        val skippedCount = history.count { it.status == "skipped" }
        tvCount.text = "전체 ${history.size}건  |  전송 ${sentCount}건  |  건너뜀 ${skippedCount}건"

        val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

        for (entry in history) {
            val isSent = entry.status == "sent"
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 10, 12, 10)
                setBackgroundColor(if (isSent) 0xFFE8F5E9.toInt() else 0xFFF5F5F5.toInt())
            }

            // 상단 행: 시각 + 상태 배지
            val rowTop = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val tvTime = TextView(this).apply {
                text = sdf.format(Date(entry.timestamp))
                setTextColor(Color.GRAY)
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvBadge = TextView(this).apply {
                text = if (isSent) "전송됨" else "건너뜀"
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundColor(if (isSent) 0xFF43A047.toInt() else 0xFF9E9E9E.toInt())
                setPadding(12, 4, 12, 4)
            }
            rowTop.addView(tvTime)
            rowTop.addView(tvBadge)

            // 발신자명
            val displayName = entry.callerName ?: entry.filename.substringBeforeLast(".")
            val tvName = TextView(this).apply {
                text = displayName
                textSize = 15f
                setTextColor(Color.BLACK)
                setPadding(0, 4, 0, 2)
            }

            // 파일명
            val tvFile = TextView(this).apply {
                text = entry.filename
                textSize = 11f
                setTextColor(Color.GRAY)
            }

            card.addView(rowTop)
            card.addView(tvName)
            card.addView(tvFile)

            // 구분선
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                    setMargins(0, 8, 0, 0)
                }
                setBackgroundColor(Color.LTGRAY)
            }

            layoutList.addView(card)
            layoutList.addView(divider)
        }
    }
}