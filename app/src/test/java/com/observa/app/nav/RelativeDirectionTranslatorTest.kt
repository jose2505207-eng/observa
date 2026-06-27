package com.observa.app.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the pure clock-face relative-direction math. */
class RelativeDirectionTranslatorTest {

    @Test fun straightAhead() {
        val g = RelativeDirectionTranslator.translate(routeBearing = 0.0, deviceHeading = 0.0)
        assertEquals(12, g.clock)
        assertFalse(g.behind)
    }

    @Test fun slightRight() {
        val g = RelativeDirectionTranslator.translate(30.0, 0.0)
        assertEquals(1, g.clock)
        assertFalse(g.behind)
    }

    @Test fun slightLeft() {
        val g = RelativeDirectionTranslator.translate(-30.0, 0.0)
        assertEquals(11, g.clock)
    }

    @Test fun rightTurn() {
        assertEquals(2, RelativeDirectionTranslator.translate(60.0, 0.0).clock)
        assertEquals(3, RelativeDirectionTranslator.translate(90.0, 0.0).clock)
    }

    @Test fun leftTurn() {
        assertEquals(10, RelativeDirectionTranslator.translate(-60.0, 0.0).clock)
        assertEquals(9, RelativeDirectionTranslator.translate(-90.0, 0.0).clock)
    }

    @Test fun behindTriggersTurnAround() {
        val g = RelativeDirectionTranslator.translate(180.0, 0.0)
        assertEquals(6, g.clock)
        assertTrue(g.behind)
    }

    @Test fun normalizeWrapsTo180Window() {
        assertEquals(0.0, RelativeDirectionTranslator.normalize(360.0), 0.0001)
        assertEquals(0.0, RelativeDirectionTranslator.normalize(720.0), 0.0001)
        assertEquals(-20.0, RelativeDirectionTranslator.normalize(340.0), 0.0001)
        assertEquals(180.0, RelativeDirectionTranslator.normalize(-180.0), 0.0001)
    }

    /** Heading near 360 and route near 0 must resolve correctly across the wrap. */
    @Test fun compassWraparoundAtZero360() {
        // route 350, heading 10 -> rel -20 -> slight left, 11 o'clock
        assertEquals(11, RelativeDirectionTranslator.translate(350.0, 10.0).clock)
        // route 10, heading 350 -> rel +20 -> slight right, 1 o'clock
        assertEquals(1, RelativeDirectionTranslator.translate(10.0, 350.0).clock)
    }

    @Test fun clockOfDestinationBearings() {
        assertEquals(12, RelativeDirectionTranslator.clockOf(0.0))
        assertEquals(3, RelativeDirectionTranslator.clockOf(90.0))
        assertEquals(6, RelativeDirectionTranslator.clockOf(180.0))
        assertEquals(9, RelativeDirectionTranslator.clockOf(270.0))
        assertEquals(12, RelativeDirectionTranslator.clockOf(360.0))
    }
}
