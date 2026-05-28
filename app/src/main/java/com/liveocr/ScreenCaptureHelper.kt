package com.liveocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File

object ScreenCaptureHelper {

    private const val TAG = "ScreenCaptureHelper"
    private const val SCREENSHOT_PATH = "/sdcard/liveocr_tmp.png"

    fun captureScreen(): Bitmap? {
        return try {
            // Jalankan screencap via Shizuku, simpan ke file
            val process = ShizukuShell.exec("screencap -p $SCREENSHOT_PATH")
            process.waitFor()

            val file = File(SCREENSHOT_PATH)
            if (!file.exists() || file.length() == 0L) {
                Log.e(TAG, "screencap file missing or empty")
                return null
            }

            val bmp = BitmapFactory.decodeFile(SCREENSHOT_PATH)
            file.delete()
            bmp
        } catch (e: Exception) {
            Log.e(TAG, "captureScreen failed: ${e.message}")
            null
        }
    }
}