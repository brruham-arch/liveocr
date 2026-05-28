package com.liveocr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.liveocr.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isServiceRunning = false

    private val shizukuRequestCode = 1001
    private val notifPermRequestCode = 1002

    private val shizukuBinderListener = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            runOnUiThread { updateShizukuStatus() }
        }
    }

    private val shizukuDeadListener = object : Shizuku.OnBinderDeadListener {
        override fun onBinderDead() {
            runOnUiThread { updateShizukuStatus() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Shizuku.addBinderReceivedListenerSticky(shizukuBinderListener)
        Shizuku.addBinderDeadListener(shizukuDeadListener)

        setupUI()
        requestNotificationPermission()
        updateShizukuStatus()
    }

    private fun setupUI() {
        // Start/Stop button
        binding.btnToggleService.setOnClickListener {
            if (isServiceRunning) stopOcrService() else startOcrService()
        }

        // Grant Shizuku permission
        binding.btnGrantShizuku.setOnClickListener {
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                Toast.makeText(this, "Shizuku version tidak support", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(shizukuRequestCode)
            } else {
                Toast.makeText(this, "Shizuku sudah granted", Toast.LENGTH_SHORT).show()
            }
        }

        // Add trigger keyword
        binding.btnAddTrigger.setOnClickListener {
            val keyword = binding.etKeyword.text.toString().trim()
            val action = binding.etAction.text.toString().trim()
            if (keyword.isEmpty()) {
                Toast.makeText(this, "Keyword tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            TriggerManager.addTrigger(this, keyword, action)
            binding.etKeyword.text?.clear()
            binding.etAction.text?.clear()
            refreshTriggerList()
            Toast.makeText(this, "Trigger '$keyword' ditambahkan", Toast.LENGTH_SHORT).show()
        }

        // Interval setting
        binding.btnSaveInterval.setOnClickListener {
            val interval = binding.etInterval.text.toString().toLongOrNull() ?: 2L
            TriggerManager.setInterval(this, interval.coerceIn(1L, 60L))
            Toast.makeText(this, "Interval: ${interval}s", Toast.LENGTH_SHORT).show()
        }

        binding.etInterval.setText(TriggerManager.getInterval(this).toString())
        refreshTriggerList()
    }

    private fun startOcrService() {
        if (!isShizukuReady()) {
            Toast.makeText(this, "Shizuku belum ready atau belum di-grant", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(this, OcrService::class.java)
        ContextCompat.startForegroundService(this, intent)
        isServiceRunning = true
        binding.btnToggleService.text = "Stop OCR"
        binding.tvStatus.text = "Status: Running"
    }

    private fun stopOcrService() {
        stopService(Intent(this, OcrService::class.java))
        isServiceRunning = false
        binding.btnToggleService.text = "Start OCR"
        binding.tvStatus.text = "Status: Stopped"
    }

    private fun isShizukuReady(): Boolean {
        return try {
            Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) { false }
    }

    private fun updateShizukuStatus() {
        val ready = isShizukuReady()
        binding.tvShizukuStatus.text = if (ready) "Shizuku: Ready ✓" else "Shizuku: Not Ready ✗"
        binding.btnGrantShizuku.isEnabled = !ready
    }

    private fun refreshTriggerList() {
        val triggers = TriggerManager.getTriggers(this)
        if (triggers.isEmpty()) {
            binding.tvTriggerList.text = "(belum ada trigger)"
            return
        }
        binding.tvTriggerList.text = triggers.entries.joinToString("\n") { (k, v) ->
            "• \"$k\" → ${v.ifEmpty { "notify" }}"
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    notifPermRequestCode
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == shizukuRequestCode) updateShizukuStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(shizukuBinderListener)
        Shizuku.removeBinderDeadListener(shizukuDeadListener)
    }
}
