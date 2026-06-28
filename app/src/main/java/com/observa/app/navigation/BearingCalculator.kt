package com.observa.app.navigation

import com.observa.app.nav.Geo
import com.observa.app.nav.GeoPoint
import com.observa.app.nav.RelativeDirectionTranslator
import kotlin.math.abs

/** The six relative directions OBSERVA speaks for GPS Orientation Lite. */
enum class RelativeDirection(val spoken: String, val turnHint: String?) {
    AHEAD("ahead", null),
    SLIGHT_LEFT("ahead-left", "Turn slightly left."),
    LEFT("left", "Turn left."),
    SLIGHT_RIGHT("ahead-right", "Turn slightly right."),
    RIGHT("right", "Turn right."),
    BEHIND("behind", "Turn around safely."),
}

/**
 * Pure geometry for orientation: bearing + distance to a destination and the user-relative direction
 * to face. Delegates the great-circle math to [Geo] (no duplication) and normalization to
 * [RelativeDirectionTranslator]; this class only adds the orientation-specific six-way vocabulary the
 * mission speaks. No Android deps; fully unit-testable.
 */
object BearingCalculator {

    /** Initial bearing from [from] to [to], degrees in [0,360). */
    fun bearingDegrees(from: GeoPoint, to: GeoPoint): Double = Geo.bearingDegrees(from, to)

    /** Great-circle distance in meters. */
    fun distanceMeters(from: GeoPoint, to: GeoPoint): Double = Geo.distanceMeters(from, to)

    /**
     * Which way the user must turn, given the bearing to the destination and the way they currently
     * face. Thresholds: ±15° ahead, ±45° slight, ±120° left/right, beyond → behind.
     */
    fun relativeDirection(bearingToDest: Double, headingDeg: Double): RelativeDirection {
        val rel = RelativeDirectionTranslator.normalize(bearingToDest - headingDeg)
        val a = abs(rel)
        val right = rel >= 0
        return when {
            a <= 15.0 -> RelativeDirection.AHEAD
            a <= 45.0 -> if (right) RelativeDirection.SLIGHT_RIGHT else RelativeDirection.SLIGHT_LEFT
            a <= 120.0 -> if (right) RelativeDirection.RIGHT else RelativeDirection.LEFT
            else -> RelativeDirection.BEHIND
        }
    }
}
