package com.observa.app.nav

import kotlin.math.abs

/** Lock-on states in OBSERVA's directional haptic language. */
enum class LockState { SEARCHING, OFF_COURSE, APPROACHING, NEAR, ALIGNED, ARRIVED, HAZARD }

/** User setting for how much haptic guidance to emit. */
enum class HapticGuidanceMode { OFF, SAFETY_ONLY, NAVIGATION_LOCK_ON, FULL }

/**
 * One computed haptic guidance step: which [state], how often to pulse ([tickIntervalMs], 0 = no
 * tick this cadence), relative [amplitude] in [0,1] (callers fall back to on/off if hardware
 * amplitude control is unavailable), and a short [pattern] tag.
 */
data class LockCue(
    val state: LockState,
    val tickIntervalMs: Long,
    val amplitude: Float,
    val pattern: String,
)

/**
 * Pure mapping from bearing error (route bearing − user heading, degrees) to a progressive
 * "lock-on" haptic cue. No Android deps; fully unit-testable. As the user rotates toward the target
 * bearing, ticks get faster and stronger; at alignment a distinct lock-on double click fires.
 * Hazards and arrival override everything. Compass uncertainty downgrades the cadence and is
 * surfaced separately (the controller speaks "Compass accuracy poor").
 */
object DirectionalLockHaptics {

    /** Normalize a bearing error to (-180, 180]. */
    fun normalizeError(deg: Double): Double {
        var d = deg % 360.0
        if (d <= -180.0) d += 360.0
        if (d > 180.0) d -= 360.0
        return d
    }

    /**
     * @param bearingErrorDeg route bearing − user heading (any range; normalized internally)
     * @param accuracy compass accuracy; POOR/UNAVAILABLE suppresses fine lock ticks
     * @param hazard true if a safety hazard is active → overrides navigation haptics
     * @param arrived true if at the destination
     */
    fun cue(
        bearingErrorDeg: Double,
        accuracy: HeadingAccuracy,
        hazard: Boolean = false,
        arrived: Boolean = false,
    ): LockCue {
        if (hazard) return LockCue(LockState.HAZARD, 0L, 1f, "stop")
        if (arrived) return LockCue(LockState.ARRIVED, 0L, 1f, "arrived")

        val err = abs(normalizeError(bearingErrorDeg))

        // With poor/unavailable compass, do not imply precision: only a coarse "searching" cue.
        if (accuracy == HeadingAccuracy.UNRELIABLE) {
            return LockCue(LockState.SEARCHING, 0L, 0f, "searching")
        }
        val degraded = accuracy == HeadingAccuracy.LOW

        return when {
            err > 60.0 -> LockCue(LockState.SEARCHING, 0L, 0f, "searching")
            err > 30.0 -> tick(LockState.OFF_COURSE, if (degraded) 1200L else 900L, 0.4f, "off-course", degraded)
            err > 15.0 -> tick(LockState.APPROACHING, if (degraded) 700L else 500L, 0.6f, "approaching", degraded)
            err > 5.0 -> tick(LockState.NEAR, if (degraded) 350L else 250L, 0.8f, "near", degraded)
            else -> LockCue(LockState.ALIGNED, 700L, 1f, "lock-on") // distinct double-click cadence
        }
    }

    /** Degraded compass widens cadence and caps amplitude so we never feel falsely precise. */
    private fun tick(state: LockState, interval: Long, amp: Float, pattern: String, degraded: Boolean) =
        LockCue(state, interval, if (degraded) minOf(amp, 0.6f) else amp, pattern)

    /** Does this mode permit navigation lock-on haptics? */
    fun navigationEnabled(mode: HapticGuidanceMode): Boolean =
        mode == HapticGuidanceMode.NAVIGATION_LOCK_ON || mode == HapticGuidanceMode.FULL

    /** Does this mode permit any haptics at all? Safety (hazard) is allowed unless OFF. */
    fun safetyEnabled(mode: HapticGuidanceMode): Boolean = mode != HapticGuidanceMode.OFF
}
