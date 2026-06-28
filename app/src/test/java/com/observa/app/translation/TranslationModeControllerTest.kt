package com.observa.app.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationModeControllerTest {

    @Test
    fun noPack_isHonestlyUnavailable_andNeverActivates() {
        val c = TranslationModeController(packs = { false }, speechAvailable = { true }, engineAvailable = { true })
        assertEquals(TranslationModeController.Readiness.NO_PACK, c.readiness())
        assertEquals("Translation language pack missing", c.statusLine())
        val msg = c.start()
        assertTrue(msg.contains("not installed"))
        assertTrue(msg.contains("never translates over the network"))
        assertFalse(c.active) // must NOT fake-activate
    }

    @Test
    fun noSpeech_isHonestlyUnavailable() {
        val c = TranslationModeController(packs = { true }, speechAvailable = { false }, engineAvailable = { true })
        assertEquals(TranslationModeController.Readiness.NO_SPEECH, c.readiness())
        assertEquals("Local speech recognition unavailable", c.statusLine())
        assertFalse(c.active)
    }

    @Test
    fun noEngine_isHonestlyUnavailable_neverFakes() {
        // pack + speech present but no offline engine bundled → honest, never activates.
        val c = TranslationModeController(packs = { true }, speechAvailable = { true }, engineAvailable = { false })
        assertEquals(TranslationModeController.Readiness.NO_ENGINE, c.readiness())
        assertEquals("Translation engine not installed", c.statusLine())
        val msg = c.start()
        assertTrue(msg.contains("never translates over the network"))
        assertFalse(c.active)
    }

    @Test
    fun readyOffline_activatesAndStops() {
        val c = TranslationModeController(packs = { true }, speechAvailable = { true }, engineAvailable = { true })
        assertEquals("Translation ready offline", c.statusLine())
        assertTrue(c.start().contains("offline"))
        assertTrue(c.active)
        assertEquals("Translation active (offline)", c.statusLine())
        c.stop()
        assertFalse(c.active)
    }
}
