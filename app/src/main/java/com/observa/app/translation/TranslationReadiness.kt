package com.observa.app.translation

/** Honest offline-translation readiness for the UI/accessibility status. */
enum class TranslationReadiness(val label: String) {
    READY_OFFLINE("Translation ready offline"),
    DOWNLOADING("Downloading language pack"),
    LANGUAGE_PACK_MISSING("Language pack missing"),
    FAILED("Language download failed"),
}

/** Pure helper: derive readiness from facts, so it stays unit-testable. */
object TranslationReadinessRules {
    fun of(downloading: Boolean, failed: Boolean, bothInstalled: Boolean): TranslationReadiness = when {
        downloading -> TranslationReadiness.DOWNLOADING
        bothInstalled -> TranslationReadiness.READY_OFFLINE
        failed -> TranslationReadiness.FAILED
        else -> TranslationReadiness.LANGUAGE_PACK_MISSING
    }
}
