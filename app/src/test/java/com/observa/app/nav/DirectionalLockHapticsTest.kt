package com.observa.app.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the progressive lock-on haptic mapping. Pure logic. */
class DirectionalLockHapticsTest {

    private fun cue(err: Double, acc: HeadingAccuracy = HeadingAccuracy.HIGH, hazard: Boolean = false, arrived: Boolean = false) =
        DirectionalLockHaptics.cue(err, acc, hazard, arrived)

    @Test fun bearingErrorNormalizes() {
        assertEquals(0.0, DirectionalLockHaptics.normalizeError(360.0), 0.001)
        assertEquals(-170.0, DirectionalLockHaptics.normalizeError(190.0), 0.001)
        assertEquals(180.0, DirectionalLockHaptics.normalizeError(-180.0), 0.001)
    }

    @Test fun farFromBearingIsSearchingNoTicks() {
        val c = cue(90.0)
        assertEquals(LockState.SEARCHING, c.state)
        assertEquals(0L, c.tickIntervalMs)
    }

    @Test fun tickIntervalShortensAsErrorShrinks() {
        val off = cue(45.0).tickIntervalMs    // 30-60
        val approaching = cue(20.0).tickIntervalMs // 15-30
        val near = cue(10.0).tickIntervalMs   // 5-15
        assertTrue(off > approaching)
        assertTrue(approaching > near)
        assertEquals(900L, off)
        assertEquals(500L, approaching)
        assertEquals(250L, near)
    }

    @Test fun alignedIsLockOn() {
        val c = cue(2.0)
        assertEquals(LockState.ALIGNED, c.state)
        assertEquals("lock-on", c.pattern)
        assertEquals(1f, c.amplitude, 0.001f)
    }

    @Test fun amplitudeIncreasesTowardAlignment() {
        assertTrue(cue(10.0).amplitude > cue(45.0).amplitude)
    }

    @Test fun hazardOverridesEverything() {
        val c = cue(2.0, hazard = true)
        assertEquals(LockState.HAZARD, c.state)
        assertEquals("stop", c.pattern)
    }

    @Test fun arrivedOverridesGuidance() {
        assertEquals(LockState.ARRIVED, cue(2.0, arrived = true).state)
    }

    @Test fun unreliableCompassSuppressesFineTicks() {
        val c = cue(2.0, acc = HeadingAccuracy.UNRELIABLE)
        assertEquals(LockState.SEARCHING, c.state)
        assertEquals(0L, c.tickIntervalMs)
    }

    @Test fun lowAccuracyWidensCadenceAndCapsAmplitude() {
        val good = cue(20.0, acc = HeadingAccuracy.HIGH)
        val low = cue(20.0, acc = HeadingAccuracy.LOW)
        assertTrue("low accuracy slower ticks", low.tickIntervalMs > good.tickIntervalMs)
        assertTrue("low accuracy capped amplitude", low.amplitude <= 0.6f)
    }

    @Test fun modeGating() {
        assertTrue(DirectionalLockHaptics.navigationEnabled(HapticGuidanceMode.NAVIGATION_LOCK_ON))
        assertTrue(DirectionalLockHaptics.navigationEnabled(HapticGuidanceMode.FULL))
        assertFalse(DirectionalLockHaptics.navigationEnabled(HapticGuidanceMode.SAFETY_ONLY))
        assertFalse(DirectionalLockHaptics.navigationEnabled(HapticGuidanceMode.OFF))
        assertTrue(DirectionalLockHaptics.safetyEnabled(HapticGuidanceMode.SAFETY_ONLY))
        assertFalse(DirectionalLockHaptics.safetyEnabled(HapticGuidanceMode.OFF))
    }
}
