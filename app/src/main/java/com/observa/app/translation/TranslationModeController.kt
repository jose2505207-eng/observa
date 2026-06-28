package com.observa.app.translation

/**
 * Honest Offline Translation Mode. OBSERVA must never fake a translation and never use the network at
 * runtime, so this controller is a **readiness gate** over the offline pipeline: real two-way
 * translation turns on only when (a) an offline language pack is installed, (b) local speech
 * recognition is available, AND (c) a local translation engine is present. No offline engine is
 * bundled in the no-INTERNET runtime build (adding ML Kit Translate would pull an INTERNET-declaring
 * dependency), so [engineAvailable] defaults to false and the mode reports an honest unavailable state
 * today — see `docs/implementation/OFFLINE_TRANSLATION.md`.
 *
 * Pure status logic (no Android deps) so it is unit-testable; the controller injects the facts.
 */
class TranslationModeController(
    private val packs: () -> Boolean,            // is at least one offline pack installed?
    private val speechAvailable: () -> Boolean,  // is local (offline) speech recognition usable?
    private val engineAvailable: () -> Boolean = { false }, // is a local translation engine installed?
    /** Runs one offline turn when ready; null until a pipeline is wired (provisioning flavor). */
    private val turns: TranslationTurnManager? = null,
) {
    enum class Readiness { READY, NO_PACK, NO_SPEECH, NO_ENGINE }

    var active = false; private set

    fun readiness(): Readiness = when {
        !packs() -> Readiness.NO_PACK
        !speechAvailable() -> Readiness.NO_SPEECH
        !engineAvailable() -> Readiness.NO_ENGINE
        else -> Readiness.READY
    }

    /** Short, stable status line for the accessible status node / debug. Always truthful. */
    fun statusLine(): String = when (readiness()) {
        Readiness.READY -> if (active) "Translation active (offline)" else "Translation ready offline"
        Readiness.NO_PACK -> "Translation language pack missing"
        Readiness.NO_SPEECH -> "Local speech recognition unavailable"
        Readiness.NO_ENGINE -> "Translation engine not installed"
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
        Readiness.NO_ENGINE ->
            "Translation needs an offline translation engine, which is not installed. OBSERVA never translates over the network."
    }

    /**
     * Process one recognized utterance during an active session. Delegates to the offline
     * [TranslationTurnManager]; with no pipeline wired it stays honest and returns the unavailable
     * status rather than inventing a translation.
     */
    fun onUtterance(recognizedText: String): String {
        if (!active) return "Translation mode is off."
        val mgr = turns ?: return statusLine()
        return when (val outcome = mgr.runTurn(recognizedText)) {
            is TranslationTurnManager.TurnOutcome.Spoke -> outcome.text
            is TranslationTurnManager.TurnOutcome.Unavailable -> outcome.reason
        }
    }

    fun stop(): String {
        active = false
        return "Translation mode off."
    }
}
