package com.observa.app.ocr

import android.graphics.Bitmap

/** Result of an on-demand OCR pass. */
data class OcrResult(val found: Boolean, val text: String) {
    /** The line to speak / show. Honest when nothing was read. */
    val message: String get() = if (found) text else "No readable text found."
}

/** Cleans raw OCR output into a concise, speakable result. Pure logic; unit-testable. */
object OcrFormatter {
    fun fromRaw(raw: String): OcrResult {
        val cleaned = raw.replace('\n', ' ').trim().replace(Regex("\\s+"), " ")
        return if (cleaned.isEmpty()) OcrResult(found = false, text = "")
        else OcrResult(found = true, text = cleaned)
    }
}

/**
 * On-demand text recognition. Implementations must be offline. [ready] reports whether the engine
 * (and its model) is actually available, so the app can decline honestly rather than pretend.
 */
interface OcrEngine {
    val ready: Boolean
    suspend fun recognize(bitmap: Bitmap): OcrResult
    fun close()
}
