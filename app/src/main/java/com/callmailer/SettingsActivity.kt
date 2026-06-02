package com.callmailer

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: PreferencesHelper
    private lateinit var layoutNameList: LinearLayout
    private lateinit var switchNameFilter: Switch
    private lateinit var layoutNameFilter: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = PreferencesHelper(this)

        val etSmtpHost = findViewById<EditText>(R.id.etSmtpHost)
        val etSmtpPort = findViewById<EditText>(R.id.etSmtpPort)
        val etSmtpUser = findViewById<EditText>(R.id.etSmtpUser)
        val etSmtpPassword = findViewById<EditText>(R.id.etSmtpPassword)
        val etRecipient = findViewById<EditText>(R.id.etRecipient)
        val etWatchFolder = findViewById<EditText>(R.id.etWatchFolder)
        switchNameFilter = findViewById(R.id.switchNameFilter)
        layoutNameFilter = findViewById(R.id.layoutNameFilter)
        layoutNameList = findViewById(R.id.layoutNameList)

        etSmtpHost.setText(prefs.smtpHost)
        etSmtpPort.setText(prefs.smtpPort.toString())
        etSmtpUser.setText(prefs.smtpUser)
        etSmtpPassword.setText(prefs.smtpPassword)
        etRecipient.setText(prefs.recipientEmail)
        etWatchFolder.setText(prefs.watchFolder)
        switchNameFilter.isChecked = prefs.nameFilterEnabled
        layoutNameFilter.visibility = if (prefs.nameFilterEnabled) View.VISIBLE else View.GONE
        refreshNameList()

        switchNameFilter.setOnCheckedChangeListener { _, checked ->
            prefs.nameFilterEnabled = checked
            layoutNameFilter.visibility = if (checked) View.VISIBLE else View.GONE
        }

        findViewById<Button>(R.id.btnPickFolder).setOnClickListener {
            browseTo(File("/storage/emulated/0")) { path ->
                etWatchFolder.setText(path)
            }
        }

        val etNewName = findViewById<EditText>(R.id.etNewName)
        findViewById<Button>(R.id.btnAddName).setOnClickListener {
            val name = etNewName.text.toString().trim()
            if (name.isNotEmpty()) {
                prefs.filterNames = prefs.filterNames + name
                etNewName.text.clear()
                refreshNameList()
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.smtpHost = etSmtpHost.text.toString().trim()
            prefs.smtpPort = etSmtpPort.text.toString().toIntOrNull() ?: 587
            prefs.smtpUser = etSmtpUser.text.toString().trim()
            prefs.smtpPassword = etSmtpPassword.text.toString()
            prefs.recipientEmail = etRecipient.text.toString().trim()
            prefs.watchFolder = etWatchFolder.text.toString().trim()
            Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun refreshNameList() {
        layoutNameList.removeAllViews()
        prefs.filterNames.forEachIndexed { idx, name ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val tv = TextView(this).apply {
                text = name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(0, 8, 0, 8)
            }
            val btn = Button(this).apply {
                text = "삭제"
                setOnClickListener {
                    prefs.filterNames = prefs.filterNames.toMutableList().also { it.removeAt(idx) }
                    refreshNameList()
                }
            }
            row.addView(tv)
            row.addView(btn)
            layoutNameList.addView(row)
        }
    }

    private fun browseTo(dir: File, onSelected: (String) -> Unit) {
        val entries = mutableListOf("⬆ 상위 폴더", "✔ 이 폴더 선택")
        val subDirs = try {
            dir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } ?: emptyList()
        } catch (_: Exception) { emptyList() }
        entries.addAll(subDirs.map { "📁 ${it.name}" })
        AlertDialog.Builder(this)
            .setTitle(dir.absolutePath)
            .setItems(entries.toTypedArray()) { _, which ->
                when (which) {
                    0 -> if (dir.parentFile != null) browseTo(dir.parentFile!!, onSelected)
                    1 -> onSelected(dir.absolutePath)
                    else -> browseTo(subDirs[which - 2], onSelected)
                }
            }.show()
    }
}