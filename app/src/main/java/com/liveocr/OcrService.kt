package com.liveocr

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class OcrService : Service() {

    private val TAG = "OcrService"
    private val CHANNEL_ID = "liveocr_service"
    private val CHANNEL_TRIGGER = "liveocr_trigger"
    private val NOTIF_ID_SERVICE = 1
    private val NOTIF_ID_TRIGGER = 2

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var prevText = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(NOTIF_ID_SERVICE, buildServiceNotification("Live OCR aktif..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startOcrLoop()
        return START_STICKY
    }

    private fun startOcrLoop() {
        val interval = TriggerManager.getInterval(this) * 1000L

        scope.launch {
            Log.d(TAG, "OCR loop started, interval=${interval}ms")
            while (isActive) {
                try {
                    // 1. Capture screen via Shizuku
                    val bitmap = ScreenCaptureHelper.captureScreen()
                    if (bitmap == null) {
                        Log.w(TAG, "Screenshot null, skip")
                        delay(interval)
                        continue
                    }

                    // 2. OCR via MLKit
                    val text = OcrEngine.recognizeText(bitmap)
                    bitmap.recycle()

                    // 3. Skip jika sama dengan sebelumnya
                    if (text == prevText) {
                        delay(interval)
                        continue
                    }
                    prevText = text

                    Log.d(TAG, "OCR: ${text.take(80)}...")

                    // Update notification dengan preview teks
                    val preview = text.lines().firstOrNull { it.isNotBlank() } ?: "..."
                    updateServiceNotification(preview)

                    // 4. Cek triggers
                    val matched = TriggerManager.checkTriggers(this@OcrService, text)
                    for ((keyword, action) in matched) {
                        handleTrigger(keyword, action, text)
                    }

                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Loop error: ${e.message}")
                }

                delay(interval)
            }
        }
    }

    private fun handleTrigger(keyword: String, action: String, fullText: String) {
        Log.d(TAG, "Trigger matched: '$keyword' → action='$action'")

        // Tampilkan notifikasi trigger
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_TRIGGER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("OCR Trigger: \"$keyword\"")
            .setContentText(fullText.take(100))
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullText.take(500)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_TRIGGER + keyword.hashCode(), notif)

        // Jalankan shell action jika ada
        if (action.isNotEmpty()) {
            scope.launch {
                try {
                    Log.d(TAG, "Executing action: $action")
                    ShizukuShell.exec(action).waitFor()
                } catch (e: Exception) {
                    Log.e(TAG, "Action exec failed: ${e.message}")
                }
            }
        }
    }

    private fun buildServiceNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Live OCR")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateServiceNotification(preview: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_SERVICE, buildServiceNotification(preview))
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Live OCR Service", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TRIGGER, "OCR Triggers", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    override fun onDestroy() {
        scope.cancel()
        OcrEngine.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
