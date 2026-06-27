package com.observa.app.nav

import kotlin.math.abs
import kotlin.math.roundToInt

/** Confidence in the current heading, surfaced to the user so we never give bad directions silently. */
enum class HeadingAccuracy { HIGH, MEDIUM, LOW, UNRELIABLE }

/** Clock-face guidance derived from a relative bearing. */
data class RelativeGuidance(
    val clock: Int,          // 12, 1, 2, 3, 9, 10, 11 ...
    val text: String,        // e.g. "Slight right, 1 o'clock."
    val behind: Boolean,     // target is roughly behind the user
)

/**
 * Translates an absolute route bearing + device heading into user-relative clock-face guidance.
 * Pure math, no Android deps; unit-testable. Used by the (later) navigation module.
 */
object RelativeDirectionTranslator {

    /** Normalize degrees to (-180, 180]. */
    fun normalize(degrees: Double): Double {
        var d = degrees % 360.0
        if (d <= -180.0) d += 360.0
        if (d > 180.0) d -= 360.0
        return d
    }

    fun translate(routeBearing: Double, deviceHeading: Double): RelativeGuidance {
        val rel = normalize(routeBearing - deviceHeading)
        val a = abs(rel)
        if (a > 120.0) {
            return RelativeGuidance(6, "Turn around safely.", behind = true)
        }
        val right = rel >= 0
        val (clock, phrase) = when {
            a <= 15.0 -> 12 to "Straight ahead."
            a <= 45.0 -> (if (right) 1 else 11) to (if (right) "Slight right, 1 o'clock." else "Slight left, 11 o'clock.")
            a <= 75.0 -> (if (right) 2 else 10) to (if (right) "Right, 2 o'clock." else "Left, 10 o'clock.")
            else -> (if (right) 3 else 9) to (if (right) "Turn right, 3 o'clock." else "Turn left, 9 o'clock.")
        }
        return RelativeGuidance(clock, phrase, behind = false)
    }

    /** Convenience: nearest clock hour for an arbitrary relative bearing (for diagnostics/tests). */
    fun clockOf(relativeBearing: Double): Int {
        val rel = (normalize(relativeBearing) + 360.0) % 360.0
        val hour = (rel / 30.0).roundToInt() % 12
        return if (hour == 0) 12 else hour
    }
}
