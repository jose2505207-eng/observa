package com.observa.app.nav

import kotlin.math.roundToInt

/** GPS fix quality, surfaced honestly to the user. */
enum class GpsAccuracy { GOOD, LOW, NONE }

/** A snapshot of where the user is and which way they face. */
data class NavFix(
    val point: GeoPoint?,
    val headingDeg: Double,
    val headingAccuracy: HeadingAccuracy,
    val gps: GpsAccuracy,
)

/** One guidance utterance for the output router. */
data class NavGuidance(val speech: String, val braille: String, val arrived: Boolean)

/**
 * Guidance-first navigation: translates (bearing-to-destination − device heading) into clock-face,
 * heading-relative instructions with honest distance and uncertainty. This is **bearing-to-
 * destination** guidance, not street turn-by-turn (documented in [[offline-navigation]]). Pure
 * logic; unit-testable. Hazard alerts always override navigation via the router's priority.
 */
class GuidanceEngine(private val arriveRadiusM: Double = 15.0) {

    fun guide(fix: NavFix, dest: SavedDestination): NavGuidance {
        if (fix.point == null || fix.gps == GpsAccuracy.NONE) {
            return NavGuidance(
                "GPS unavailable. Cannot guide to ${dest.name} right now.",
                "GPS unavailable",
                arrived = false,
            )
        }
        val distance = Geo.distanceMeters(fix.point, dest.point)
        if (distance <= arriveRadiusM) {
            return NavGuidance("Destination ahead. ${dest.name}.", "${dest.name}: arrived", arrived = true)
        }
        val bearing = Geo.bearingDegrees(fix.point, dest.point)
        val rel = RelativeDirectionTranslator.translate(bearing, fix.headingDeg)
        val meters = roundMeters(distance)
        val core = when {
            rel.behind -> "Turn around safely. $meters meters to ${dest.name}."
            rel.clock == 12 -> "Continue straight, $meters meters."
            else -> "${rel.text} $meters meters."
        }
        val warning = buildString {
            if (fix.gps == GpsAccuracy.LOW) append(" GPS accuracy low. Use caution.")
            if (fix.headingAccuracy == HeadingAccuracy.LOW || fix.headingAccuracy == HeadingAccuracy.UNRELIABLE) {
                append(" Compass accuracy low. Calibrate phone.")
            }
        }
        val braille = if (rel.behind) "Turn around · ${meters}m" else "${rel.clock} o'clock · ${meters}m"
        return NavGuidance(core + warning, braille, arrived = false)
    }

    /** Round to a calm, non-false-precision step (1m under 30m, else nearest 5m). */
    private fun roundMeters(d: Double): Int =
        if (d < 30) d.roundToInt() else (d / 5).roundToInt() * 5
}
