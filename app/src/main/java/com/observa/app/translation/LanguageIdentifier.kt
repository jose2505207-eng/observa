package com.observa.app.translation

/** Outcome of identifying the language of an utterance. */
sealed class Identification {
    data class Detected(val language: String, val confident: Boolean) : Identification()
    /** No offline language-ID model available — honest, never a guess. */
    object Unavailable : Identification()
}

/**
 * Identifies the source language of spoken text so [LocalTranslator] knows which way to translate.
 * Without a bundled offline language-ID model this returns [Identification.Unavailable] — it never
 * guesses a language, which would risk translating into the wrong one. A real model (provisioned out
 * of band) would supply [model]. Pure boundary; unit-testable.
 */
class LanguageIdentifier(private val model: Model? = null) {

    /** A genuinely-offline language-ID model. Must never touch the network. */
    interface Model {
        /** Returns ISO language code, or null if undetermined. */
        fun identify(text: String): String?
    }

    fun available(): Boolean = model != null

    fun identify(text: String): Identification {
        val m = model ?: return Identification.Unavailable
        val code = m.identify(text) ?: return Identification.Unavailable
        return Identification.Detected(code, confident = true)
    }
}
