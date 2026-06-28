package com.observa.app.translation

/**
 * Speaks the outcome of a translation turn. Only ever voices a **real** translation
 * ([TranslationResult.Translated]); when the engine is unavailable it speaks the honest reason rather
 * than inventing words. Decoupled from the platform TTS via a [speak] sink so it is unit-testable and
 * reuses OBSERVA's existing `output/Speaker` at runtime.
 */
class TranslationSpeechOutput(private val speak: (String) -> Unit) {

    /** Returns the line that was spoken (for status/repeat). */
    fun present(result: TranslationResult): String {
        val line = when (result) {
            is TranslationResult.Translated -> result.text
            is TranslationResult.Unavailable -> result.reason
        }
        speak(line)
        return line
    }
}
