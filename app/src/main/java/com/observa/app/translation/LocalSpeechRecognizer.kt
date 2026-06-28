package com.observa.app.translation

/**
 * Translation-side gate for **on-device** speech recognition. Translation Mode must never stream audio
 * to the cloud, so a turn can only begin when local (offline) recognition is available. This thin
 * boundary reports that availability honestly; OBSERVA's existing `voice/OfflineSpeechRecognizer`
 * (which already prefers the on-device recognizer and requests offline results) supplies the probe at
 * runtime. Kept Android-free so [TranslationTurnManager] stays unit-testable.
 */
class LocalSpeechRecognizer(private val availabilityProbe: () -> Boolean) {

    /** True only when offline, on-device speech recognition is usable. */
    fun isAvailable(): Boolean = availabilityProbe()
}
