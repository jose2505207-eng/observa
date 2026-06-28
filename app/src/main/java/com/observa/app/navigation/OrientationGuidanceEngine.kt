package com.observa.app.navigation

import com.observa.app.nav.GeoPoint
import com.observa.app.nav.GpsAccuracy
import com.observa.app.nav.HeadingAccuracy
import com.observa.app.nav.SavedDestination
import kotlin.math.roundToInt

/** Honest confidence the user hears, derived from real GPS + compass quality. */
enum class OrientationConfidence(val label: String) {
    GOOD("good"),
    WEAK_GPS("weak GPS"),
    COMPASS_UNSTABLE("compass unstable"),
}

/** One orientation utterance: spoken line, short braille line, stable status, and confidence. */
data class OrientationGuidance(
    val speech: String,
    val braille: String,
    val status: String,
    val confidence: OrientationConfidence,
    val arrived: Boolean,
)

/**
 * Turns a live GPS fix + compass heading into the mission's directional guidance — relative direction
 * ("Destination ahead-left, 40 meters."), a turn hint when off-axis ("Turn slightly right."), an
 * honest distance, and a confidence state (good / weak GPS / compass unstable). Pure logic over
 * [BearingCalculator]; no Android deps, fully unit-testable. Never invents a location: with no fix it
 * says "GPS signal weak." instead of guessing.
 */
class OrientationGuidanceEngine(private val arriveRadiusM: Double = 15.0) {

    fun guide(
        current: GeoPoint?,
        headingDeg: Double,
        headingAccuracy: HeadingAccuracy,
        gps: GpsAccuracy,
        dest: SavedDestination,
    ): OrientationGuidance {
        if (current == null || gps == GpsAccuracy.NONE) {
            return OrientationGuidance(
                speech = "GPS signal weak. Acquiring location for ${dest.name}.",
                braille = "GPS weak · acquiring",
                status = "Orientation active. GPS signal weak.",
                confidence = OrientationConfidence.WEAK_GPS,
                arrived = false,
            )
        }
        val distance = BearingCalculator.distanceMeters(current, dest.point)
        val meters = roundMeters(distance)
        if (distance <= arriveRadiusM) {
            return OrientationGuidance(
                speech = "You have arrived at ${dest.name}.",
                braille = "${dest.name}: arrived",
                status = "Orientation active. Arrived at ${dest.name}.",
                confidence = confidenceOf(gps, headingAccuracy),
                arrived = true,
            )
        }
        val bearing = BearingCalculator.bearingDegrees(current, dest.point)
        val dir = BearingCalculator.relativeDirection(bearing, headingDeg)
        val confidence = confidenceOf(gps, headingAccuracy)

        val core = "Destination ${dir.spoken}, $meters meters."
        val hint = dir.turnHint?.let { " $it" } ?: ""
        val warning = when (confidence) {
            OrientationConfidence.WEAK_GPS -> " GPS signal weak. Use caution."
            OrientationConfidence.COMPASS_UNSTABLE -> " Compass unstable. Move the phone in a figure eight."
            OrientationConfidence.GOOD -> ""
        }
        val braille = "${dir.spoken} · ${meters}m"
        return OrientationGuidance(
            speech = core + hint + warning,
            braille = braille,
            status = "Orientation active. Destination ${dir.spoken}, $meters meters.",
            confidence = confidence,
            arrived = false,
        )
    }

    private fun confidenceOf(gps: GpsAccuracy, heading: HeadingAccuracy): OrientationConfidence = when {
        heading == HeadingAccuracy.LOW || heading == HeadingAccuracy.UNRELIABLE ->
            OrientationConfidence.COMPASS_UNSTABLE
        gps == GpsAccuracy.LOW -> OrientationConfidence.WEAK_GPS
        else -> OrientationConfidence.GOOD
    }

    /** Round to a calm, non-false-precision step (1m under 30m, else nearest 5m). */
    private fun roundMeters(d: Double): Int =
        if (d < 30) d.roundToInt() else (d / 5).roundToInt() * 5
}
