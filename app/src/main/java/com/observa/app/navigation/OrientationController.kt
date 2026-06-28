package com.observa.app.navigation

import com.observa.app.nav.GeoPoint
import com.observa.app.nav.GuidanceEngine
import com.observa.app.nav.HeadingAccuracy
import com.observa.app.nav.GpsAccuracy
import com.observa.app.nav.NavFix
import com.observa.app.nav.NavGuidance
import com.observa.app.nav.SavedDestination
import com.observa.app.nav.DestinationStore

/** Minimal location surface so [OrientationController] stays unit-testable (see [LocationProvider]). */
interface LocationSource {
    fun start()
    fun stop()
    fun current(): GeoPoint?
    val accuracy: GpsAccuracy
    val hasPermission: Boolean
}

/**
 * GPS Orientation Lite — heading/bearing/distance guidance toward a destination. This is **not**
 * turn-by-turn maps: it reports which way to face and how far, using real device GPS ([LocationProvider])
 * + the compass heading, run through the existing pure [GuidanceEngine] / clock-face translator. Fully
 * offline (satellite GPS + sensors). Hazard alerts always take priority — orientation is emitted at
 * NAVIGATION priority through the same router, so a hazard interrupts it.
 *
 * Updates are rate-limited ([minUpdateMs]) so TalkBack / braille are not flooded.
 */
class OrientationController(
    private val location: LocationSource,
    /** Supplies (headingDegrees, headingAccuracy) from the device compass. */
    private val heading: () -> Pair<Double, HeadingAccuracy>,
    private val destination: SavedDestination = DestinationStore.DEMO.first(),
    private val guidance: GuidanceEngine = GuidanceEngine(),
    private val minUpdateMs: Long = 4_000L,
) {
    var active = false; private set
    var lastGuidance: NavGuidance? = null; private set
    private var lastEmitMs = 0L

    val destinationName: String get() = destination.name

    /** Begin orientation. Returns the opening guidance/status to speak. */
    fun start(): NavGuidance {
        active = true
        lastEmitMs = 0L
        location.start()
        val opening = if (!location.hasPermission)
            NavGuidance(
                "Orientation needs location permission to guide you.",
                "Orientation: no location permission",
                arrived = false,
            )
        else NavGuidance(
            "Orientation on. Acquiring GPS toward ${destination.name}.",
            "Orientation: acquiring GPS",
            arrived = false,
        )
        lastGuidance = opening
        return opening
    }

    fun stop() {
        active = false
        location.stop()
        lastGuidance = null
    }

    /**
     * Called periodically by the processing loop. Returns guidance to emit, or null when inactive or
     * still within the rate-limit window (so we never spam).
     */
    fun tick(nowMs: Long): NavGuidance? {
        if (!active) return null
        if (nowMs - lastEmitMs < minUpdateMs) return null
        lastEmitMs = nowMs
        val g = compute()
        lastGuidance = g
        return g
    }

    /** "Repeat orientation": the last guidance, or a fresh computation if none yet. */
    fun repeat(): NavGuidance = if (active) (lastGuidance ?: compute().also { lastGuidance = it })
    else NavGuidance("Orientation is off.", "Orientation off", arrived = false)

    /** Short, stable status line for the accessible status node. */
    fun statusLine(): String =
        if (!active) "Orientation off"
        else "Orientation active. ${lastGuidance?.braille ?: "acquiring GPS"}"

    private fun compute(): NavGuidance {
        val point = location.current()
        val (hdg, hacc) = heading()
        val fix = NavFix(
            point = point,
            headingDeg = hdg,
            headingAccuracy = hacc,
            gps = if (point == null) GpsAccuracy.NONE else location.accuracy,
        )
        return guidance.guide(fix, destination)
    }
}
