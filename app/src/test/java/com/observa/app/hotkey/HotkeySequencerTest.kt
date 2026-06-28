package com.observa.app.hotkey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the pure volume-key shortcut sequencer + command mapping. */
class HotkeySequencerTest {

    @Test fun mappingMatchesGrammar() {
        assertEquals(HotkeyCommand.REPEAT_LAST, HotkeySequencer.map(HotkeyButton.VOLUME_UP, 1))
        assertEquals(HotkeyCommand.STATUS, HotkeySequencer.map(HotkeyButton.VOLUME_UP, 2))
        assertEquals(HotkeyCommand.READ_TEXT, HotkeySequencer.map(HotkeyButton.VOLUME_UP, 3))
        // Mission cap: no more than three clicks per volume button — a 4th press is unmapped.
        assertEquals(HotkeyCommand.NONE, HotkeySequencer.map(HotkeyButton.VOLUME_UP, 4))
        assertEquals(HotkeyCommand.MUTE_TOGGLE, HotkeySequencer.map(HotkeyButton.VOLUME_DOWN, 1))
        assertEquals(HotkeyCommand.STOP_NAVIGATION, HotkeySequencer.map(HotkeyButton.VOLUME_DOWN, 2))
        assertEquals(HotkeyCommand.EMERGENCY_PAUSE, HotkeySequencer.map(HotkeyButton.VOLUME_DOWN, 3))
        assertEquals(HotkeyCommand.NONE, HotkeySequencer.map(HotkeyButton.VOLUME_UP, 9))
        assertEquals(HotkeyCommand.NONE, HotkeySequencer.map(null, 1))
    }

    @Test fun countsPressesWithinWindow() {
        val s = HotkeySequencer(windowMs = 1000, debounceMs = 50)
        assertTrue(s.onPress(HotkeyButton.VOLUME_UP, 0))
        assertTrue(s.onPress(HotkeyButton.VOLUME_UP, 200))
        assertTrue(s.onPress(HotkeyButton.VOLUME_UP, 400))
        assertEquals(HotkeyCommand.READ_TEXT, s.flush()) // 3 presses → read text
    }

    @Test fun debounceDropsTooFastRepeats() {
        val s = HotkeySequencer(windowMs = 1000, debounceMs = 100)
        assertTrue(s.onPress(HotkeyButton.VOLUME_UP, 0))
        assertFalse("within debounce window", s.onPress(HotkeyButton.VOLUME_UP, 30))
        assertEquals(HotkeyCommand.REPEAT_LAST, s.flush()) // counted once
    }

    @Test fun differentButtonResetsRun() {
        val s = HotkeySequencer()
        s.onPress(HotkeyButton.VOLUME_UP, 0)
        s.onPress(HotkeyButton.VOLUME_DOWN, 100)
        assertEquals(HotkeyCommand.MUTE_TOGGLE, s.flush()) // run restarted on the down key
    }

    @Test fun expiryDetectedAfterWindow() {
        val s = HotkeySequencer(windowMs = 1200)
        s.onPress(HotkeyButton.VOLUME_UP, 0)
        assertFalse(s.isExpired(500))
        assertTrue(s.isExpired(1300))
    }

    @Test fun flushClearsState() {
        val s = HotkeySequencer()
        s.onPress(HotkeyButton.VOLUME_UP, 0)
        assertTrue(s.pending)
        s.flush()
        assertFalse(s.pending)
        assertEquals(HotkeyCommand.NONE, s.flush()) // nothing pending
    }
}
