package com.liveocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ScreenCaptureHelper {

    private const val TAG = "ScreenCaptureHelper"

    /**
     * Capture screen via Shizuku shell (screencap -p)
     * Returns Bitmap or null on failure
     */
    fun captureScreen(): Bitmap? {
        return try {
            // Use Shizuku to run screencap and pipe PNG output
            val process = ShizukuShell.exec("screencap -p")
            val inputStream: InputStream = process.inputStream
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                baos.write(buffer, 0, read)
            }
            process.waitFor()
            val bytes = baos.toByteArray()
            if (bytes.isEmpty()) {
                Log.e(TAG, "screencap returned empty data")
                return null
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "captureScreen failed: ${e.message}")
            null
        }
    }
}
