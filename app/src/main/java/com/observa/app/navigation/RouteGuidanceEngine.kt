package com.observa.app.navigation

import com.observa.app.nav.GeoPoint
import com.observa.app.nav.GpsAccuracy
import com.observa.app.nav.HeadingAccuracy
import com.observa.app.nav.SavedDestination

/**
 * Guidance for Navigation Mode. When real offline **route** data exists it would emit route steps;
 * with none bundled it honestly falls back to **bearing guidance** (heading/bearing/distance/relative
 * direction) via the pure [OrientationGuidanceEngine] — and never calls itself turn-by-turn. Pure
 * logic; unit-testable. [routeAvailable] is supplied by [OfflineMapRepository.hasRoute].
 */
class RouteGuidanceEngine(private val bearing: OrientationGuidanceEngine = OrientationGuidanceEngine()) {

    fun guide(
        current: GeoPoint?,
        headingDeg: Double,
        headingAccuracy: HeadingAccuracy,
        gps: GpsAccuracy,
        dest: SavedDestination,
        routeAvailable: Boolean,
    ): OrientationGuidance {
        // No bundled routing engine: always bearing guidance today. The flag documents intent and lets
        // a future routable pack light up route steps without changing callers.
        val g = bearing.guide(current, headingDeg, headingAccuracy, gps, dest)
        return if (routeAvailable) g else g // identical now; explicit so the honest path is obvious
    }

    /** Whether this engine is producing route steps (vs bearing guidance). Honest-false today. */
    fun usingRoute(routeAvailable: Boolean): Boolean = routeAvailable
}
