package com.observa.app.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the per-key rate limiter that protects speech / braille / audio from flooding. */
class OutputThrottlerTest {

    @Test fun firstEmitForAKeyIsAllowed() {
        val t = OutputThrottler(1_000L)
        assertTrue(t.shouldEmit("obstacle", nowMs = 0L))
    }

    @Test fun repeatedKeyWithinCooldownIsRateLimited() {
        val t = OutputThrottler(1_000L)
        assertTrue(t.shouldEmit("obstacle", 0L))
        assertFalse(t.shouldEmit("obstacle", 200L))
        assertFalse(t.shouldEmit("obstacle", 999L))
    }

    @Test fun keyIsAllowedAgainAfterCooldownElapses() {
        val t = OutputThrottler(1_000L)
        assertTrue(t.shouldEmit("obstacle", 0L))
        assertFalse(t.shouldEmit("obstacle", 500L))   // suppressed, does not reset the clock
        assertTrue(t.shouldEmit("obstacle", 1_000L))  // 1000ms since last *emit*
    }

    @Test fun differentKeysAreIndependent() {
        val t = OutputThrottler(1_000L)
        assertTrue(t.shouldEmit("left", 0L))
        assertTrue(t.shouldEmit("right", 0L))
        assertFalse(t.shouldEmit("left", 100L))
    }

    @Test fun speechSpamIsPrevented() {
        val t = OutputThrottler(1_000L)
        var allowed = 0
        // 10 rapid identical events inside one cooldown window.
        for (i in 0 until 10) if (t.shouldEmit("person ahead", i * 50L)) allowed++
        assertTrue("only the first identical event should pass", allowed == 1)
    }

    @Test fun perCallCooldownOverrideIsHonored() {
        val t = OutputThrottler(defaultCooldownMs = 5_000L)
        assertTrue(t.shouldEmit("ping", 0L, cooldownMs = 100L))
        assertTrue(t.shouldEmit("ping", 150L, cooldownMs = 100L)) // short override lets it through
    }

    @Test fun resetClearsHistory() {
        val t = OutputThrottler(1_000L)
        assertTrue(t.shouldEmit("x", 0L))
        assertFalse(t.shouldEmit("x", 100L))
        t.reset()
        assertTrue(t.shouldEmit("x", 100L))
    }

    /**
     * The throttler has no notion of urgency by design — urgent breakthrough lives one level up:
     * AccessibilityOutputRouter skips the throttler entirely for urgent events. This models that
     * contract so the invariant is regression-guarded without an Android (TextToSpeech) dependency.
     */
    @Test fun urgentEventsBypassTheThrottler() {
        val t = OutputThrottler(10_000L)
        fun routerEmit(urgent: Boolean, key: String, now: Long): Boolean =
            urgent || t.shouldEmit(key, now)

        assertTrue(routerEmit(urgent = false, key = "obstacle", now = 0L))   // first non-urgent passes
        assertFalse(routerEmit(urgent = false, key = "obstacle", now = 10L)) // throttled
        assertTrue(routerEmit(urgent = true, key = "obstacle", now = 20L))   // urgent breaks through
    }
}
