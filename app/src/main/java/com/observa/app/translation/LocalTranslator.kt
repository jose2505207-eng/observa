package com.observa.app.translation

/** Result of a translation attempt. [Unavailable] is honest — text is never fabricated. */
sealed class TranslationResult {
    data class Translated(val text: String, val from: String, val to: String) : TranslationResult()
    data class Unavailable(val reason: String) : TranslationResult()
}

/**
 * On-device translation engine boundary. **This is the honesty anchor of Translation Mode**: OBSERVA
 * must never fabricate a translation and never use the network, so when no offline engine is bundled
 * [translate] returns [TranslationResult.Unavailable] rather than a made-up string. A real offline
 * engine (provisioned out of band — see `docs/implementation/OFFLINE_TRANSLATION.md`) would supply an
 * [engine] that performs genuine local translation; until then [isReady] is false.
 *
 * Pure boundary (no Android deps); unit-testable. An engine is injected, never assumed.
 */
class LocalTranslator(private val engine: Engine? = null) {

    /** A genuinely-offline translation engine. Implementations must never touch the network. */
    interface Engine {
        fun translate(text: String, from: String, to: String): String
    }

    fun isReady(): Boolean = engine != null

    fun translate(text: String, from: String, to: String): TranslationResult {
        val e = engine ?: return TranslationResult.Unavailable(
            "No offline translation engine is installed. OBSERVA never translates over the network.",
        )
        return TranslationResult.Translated(e.translate(text, from, to), from, to)
    }
}
