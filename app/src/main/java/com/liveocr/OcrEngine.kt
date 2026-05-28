package com.liveocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OcrEngine {

    private const val TAG = "OcrEngine"
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Run MLKit OCR on a Bitmap, returns full text string.
     * Suspend function — call from coroutine.
     */
    suspend fun recognizeText(bitmap: Bitmap): String {
        return suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    cont.resume(result.text)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "OCR failed: ${e.message}")
                    cont.resumeWithException(e)
                }
        }
    }

    fun close() {
        recognizer.close()
    }
}
