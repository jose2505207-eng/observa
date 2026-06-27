package com.observa.app.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the concise Braille line formatting. Pure logic, no Android deps. */
class BrailleStatusPresenterTest {

    @Test fun ambientStatusIsConcise() {
        val line = BrailleStatusPresenter.format(
            BrailleSnapshot(observing = true, muted = false, aiModel = "unavailable — heuristic fallback"),
        )
        assertEquals("OBSERVA: observing on, AI fallback", line)
    }

    @Test fun observingOffAndMutedAreShown() {
        val line = BrailleStatusPresenter.format(
            BrailleSnapshot(observing = false, muted = true, aiModel = "loaded — CPU"),
        )
        assertEquals("OBSERVA: observing off, muted, AI CPU", line)
    }

    @Test fun qnnIsShownWhenActive() {
        val line = BrailleStatusPresenter.format(
            BrailleSnapshot(observing = true, muted = false, aiModel = "loaded — QNN active"),
        )
        assertTrue(line.endsWith("AI QNN"))
    }

    @Test fun activeHazardTakesTheWholeLine() {
        val line = BrailleStatusPresenter.format(
            BrailleSnapshot(observing = true, muted = false, aiModel = "fallback", hazard = "Obstacle center"),
        )
        assertEquals("Obstacle center", line)
    }

    /** A safety-critical hazard must never be buried behind ambient status. */
    @Test fun urgentHazardIsNotLost() {
        val line = BrailleStatusPresenter.format(
            BrailleSnapshot(
                observing = true, muted = true, aiModel = "fallback",
                hazard = "Obstacle ahead", urgent = true,
            ),
        )
        assertEquals("Obstacle ahead", line)
    }
}
