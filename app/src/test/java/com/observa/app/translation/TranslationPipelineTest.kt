package com.observa.app.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationPipelineTest {

    private val spoken = ArrayList<String>()
    private fun output() = TranslationSpeechOutput { spoken.add(it) }

    @Test
    fun translator_withoutEngine_isUnavailable_neverFakes() {
        val t = LocalTranslator()
        assertFalse(t.isReady())
        val r = t.translate("hola", "es", "en")
        assertTrue(r is TranslationResult.Unavailable)
        assertTrue((r as TranslationResult.Unavailable).reason.contains("never translates over the network"))
    }

    @Test
    fun identifier_withoutModel_isUnavailable_neverGuesses() {
        val id = LanguageIdentifier()
        assertFalse(id.available())
        assertEquals(Identification.Unavailable, id.identify("hola"))
    }

    @Test
    fun turn_withNoEngine_failsHonestly_andSpeaksReason() {
        val mgr = TranslationTurnManager(
            speech = LocalSpeechRecognizer { true },
            identifier = LanguageIdentifier(),       // no model
            translator = LocalTranslator(),          // no engine
            output = output(),
            targetLanguage = "en",
        )
        assertFalse(mgr.ready())
        val outcome = mgr.runTurn("hola")
        assertTrue(outcome is TranslationTurnManager.TurnOutcome.Unavailable)
        // The honest reason was spoken — no fabricated translation.
        assertTrue(spoken.single().contains("identify the language offline"))
    }

    @Test
    fun turn_withRealEngine_translatesAndSpeaks() {
        val mgr = TranslationTurnManager(
            speech = LocalSpeechRecognizer { true },
            identifier = LanguageIdentifier(object : LanguageIdentifier.Model {
                override fun identify(text: String) = "es"
            }),
            translator = LocalTranslator(object : LocalTranslator.Engine {
                override fun translate(text: String, from: String, to: String) = "hello"
            }),
            output = output(),
            targetLanguage = "en",
        )
        assertTrue(mgr.ready())
        val outcome = mgr.runTurn("hola")
        assertEquals(TranslationTurnManager.TurnOutcome.Spoke("hello"), outcome)
        assertEquals("hello", spoken.single())
    }
}
