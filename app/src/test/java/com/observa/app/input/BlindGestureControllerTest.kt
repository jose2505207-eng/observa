package com.observa.app.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlindGestureControllerTest {

    private fun controller(talkBack: Boolean) = BlindGestureController(isTalkBackOn = { talkBack })

    @Test
    fun talkBackOff_mapsGesturesToActions() {
        val c = controller(talkBack = false)
        assertTrue(c.rawGesturesActive())
        assertEquals(GestureAction.OPEN_VOICE, c.resolve(BlindGesture.TRIPLE_TAP))
        assertEquals(GestureAction.REPEAT_LAST, c.resolve(BlindGesture.DOUBLE_TAP))
        assertEquals(GestureAction.START_TRANSLATION, c.resolve(BlindGesture.SWIPE_UP))
        assertEquals(GestureAction.START_ORIENTATION, c.resolve(BlindGesture.SWIPE_DOWN))
        assertEquals(GestureAction.PUSH_TO_TALK, c.resolve(BlindGesture.LONG_PRESS))
        assertEquals(GestureAction.NONE, c.resolve(BlindGesture.SINGLE_TAP))
    }

    @Test
    fun talkBackOn_disablesRawGestures_routesToNativeActions() {
        val c = controller(talkBack = true)
        assertFalse(c.rawGesturesActive())
        // Every raw gesture is ignored so TalkBack's own one-finger handling is never fought.
        BlindGesture.values().forEach { assertEquals(GestureAction.NONE, c.resolve(it)) }
        assertEquals("Gestures available through TalkBack actions.", c.statusLine())
    }

    @Test
    fun statusLine_flipsWithTalkBack() {
        assertTrue(controller(false).statusLine().startsWith("Triple tap for voice commands"))
        assertEquals("Gestures available through TalkBack actions.", controller(true).statusLine())
    }

    @Test
    fun tapGesture_mapsCounts() {
        val c = controller(false)
        assertNull(c.tapGesture(1))
        assertEquals(BlindGesture.DOUBLE_TAP, c.tapGesture(2))
        assertEquals(BlindGesture.TRIPLE_TAP, c.tapGesture(3))
        assertNull(c.tapGesture(4))
    }

    @Test
    fun swipeGesture_respectsThresholdAndDirection() {
        val c = controller(false)
        assertNull(c.swipeGesture(dyPx = -10f, thresholdPx = 48f))   // too small
        assertEquals(BlindGesture.SWIPE_UP, c.swipeGesture(dyPx = -60f, thresholdPx = 48f))
        assertEquals(BlindGesture.SWIPE_DOWN, c.swipeGesture(dyPx = 60f, thresholdPx = 48f))
    }
}
