package com.observa.app.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Offline OCR via ML Kit's bundled Latin text-recognition model. The model ships inside the APK
 * (`com.google.mlkit:text-recognition`), so recognition runs fully on device — no Play Services
 * download, no network (the INTERNET permission is stripped in the manifest). Recognition runs only
 * on demand; it is never called per-frame.
 */
class MlKitOcrEngine : OcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** The Latin model is bundled, so the engine is always ready offline. */
    override val ready: Boolean = true

    override suspend fun recognize(bitmap: Bitmap): OcrResult =
        suspendCancellableCoroutine { cont ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val result = OcrFormatter.fromRaw(visionText.text)
                        Log.i("OBSERVA_OCR", "recognize: found=${result.found}, chars=${result.text.length}")
                        if (cont.isActive) cont.resume(result)
                    }
                    .addOnFailureListener { e ->
                        Log.e("OBSERVA_OCR", "recognition failed", e)
                        if (cont.isActive) cont.resume(OcrResult(found = false, text = ""))
                    }
            } catch (e: Exception) {
                Log.e("OBSERVA_OCR", "recognize threw", e)
                if (cont.isActive) cont.resume(OcrResult(found = false, text = ""))
            }
        }

    override fun close() = recognizer.close()
}
