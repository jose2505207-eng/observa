package com.observa.app.translation

/**
 * Honest Offline Translation Mode. OBSERVA must never fake a translation and never use the network at
 * runtime, so this controller is a **readiness gate**: real two-way translation turns on only when
 * (a) an offline language pack is installed AND (b) local speech recognition is available. Until a
 * local translation engine + pack are provisioned (offline-after-install — see
 * `docs/implementation/OFFLINE_TRANSLATION.md`), it reports an honest unavailable state and performs
 * no translation.
 *
 * Pure status logic (no Android deps) so it is unit-testable; the controller injects the two facts.
 */
class TranslationModeController(
    private val packs: () -> Boolean,            // is at least one offline pack installed?
    private val speechAvailable: () -> Boolean,  // is local (offline) speech recognition usable?
) {
    enum class Readiness { READY, NO_PACK, NO_SPEECH }

    var active = false; private set

    fun readiness(): Readiness = when {
        !packs() -> Readiness.NO_PACK
        !speechAvailable() -> Readiness.NO_SPEECH
        else -> Readiness.READY
    }

    /** Short, stable status line for the accessible status node / debug. Always truthful. */
    fun statusLine(): String = when (readiness()) {
        Readiness.READY -> if (active) "Translation active (offline)" else "Translation ready offline"
        Readiness.NO_PACK -> "Translation language pack missing"
        Readiness.NO_SPEECH -> "Local speech recognition unavailable"
    }

    /**
     * Attempt to start. Returns the spoken response. Never fabricates a translation: if not ready it
     * explains exactly what is missing and stays off (and is explicit that the cloud is never used).
     */
    fun start(): String = when (readiness()) {
        Readiness.READY -> {
            active = true
            "Translation mode on, offline. Hold to talk."
        }
        Readiness.NO_PACK ->
            "Translation needs an offline language pack, which is not installed. OBSERVA never translates over the network."
        Readiness.NO_SPEECH ->
            "Translation needs on-device speech recognition, which is unavailable on this device."
    }

    fun stop(): String {
        active = false
        return "Translation mode off."
    }
}
