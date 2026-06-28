package com.observa.app.translation

/**
 * Orchestrates one translation turn end to end: recognized speech → identify source language →
 * translate locally → speak the result. Each step is delegated to an honest boundary, so the whole
 * turn fails honestly: if speech, the language-ID model, or the translation engine is unavailable, the
 * turn yields [TurnOutcome.Unavailable] and the spoken output states why — **a translation is never
 * fabricated**. Pure orchestration; unit-testable with fakes.
 */
class TranslationTurnManager(
    private val speech: LocalSpeechRecognizer,
    private val identifier: LanguageIdentifier,
    private val translator: LocalTranslator,
    private val output: TranslationSpeechOutput,
    /** Language the user wants to hear, e.g. the user's own language. */
    private val targetLanguage: String,
) {
    sealed class TurnOutcome {
        data class Spoke(val text: String) : TurnOutcome()
        data class Unavailable(val reason: String) : TurnOutcome()
    }

    /** True only when every stage needed for a real, offline translation turn is present. */
    fun ready(): Boolean =
        speech.isAvailable() && identifier.available() && translator.isReady()

    /**
     * Run a turn on already-recognized [recognizedText]. Returns the outcome and speaks it. Never
     * fabricates: unavailable stages produce an honest spoken explanation.
     */
    fun runTurn(recognizedText: String): TurnOutcome {
        if (!speech.isAvailable()) return fail("On-device speech recognition is unavailable.")
        val id = identifier.identify(recognizedText)
        val from = when (id) {
            is Identification.Detected -> id.language
            Identification.Unavailable ->
                return fail("Cannot identify the language offline, so OBSERVA will not guess a translation.")
        }
        return when (val r = translator.translate(recognizedText, from, targetLanguage)) {
            is TranslationResult.Translated -> TurnOutcome.Spoke(output.present(r))
            is TranslationResult.Unavailable -> fail(r.reason)
        }
    }

    private fun fail(reason: String): TurnOutcome {
        output.present(TranslationResult.Unavailable(reason))
        return TurnOutcome.Unavailable(reason)
    }
}
