package com.observa.app.navigation

import com.observa.app.nav.GeoPoint
import com.observa.app.nav.GpsAccuracy
import com.observa.app.nav.HeadingAccuracy
import com.observa.app.nav.SavedDestination

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
 * turn-by-turn maps: it reports which way to face and how far, composing real device GPS
 * ([LocationProvider]), the real compass ([CompassProvider]), pure geometry ([BearingCalculator]),
 * the [OrientationGuidanceEngine], and a [DestinationStore]. Fully offline (satellite GPS + sensors).
 *
 * Hazard safety: guidance is gated by [NavigationSafetyArbiter] and emitted at NAVIGATION priority, so
 * a vision hazard always interrupts and is never talked over. Updates are rate-limited ([minUpdateMs])
 * so TalkBack / braille are not flooded.
 */
class OrientationController(
    private val location: LocationSource,
    private val compass: HeadingSource,
    private val destinations: DestinationStore = DestinationStore(),
    private val guidance: OrientationGuidanceEngine = OrientationGuidanceEngine(),
    private val arbiter: NavigationSafetyArbiter = NavigationSafetyArbiter(),
    private val minUpdateMs: Long = 4_000L,
) {
    var active = false; private set
    var lastGuidance: OrientationGuidance? = null; private set
    private var lastEmitMs = 0L

    val destinationName: String get() = destinations.current.name

    /** Repoint the demo destination (debug control). */
    fun setDestination(destination: SavedDestination) = destinations.setDestination(destination)

    /** Begin orientation. Returns the opening guidance/status to speak. */
    fun start(): OrientationGuidance {
        active = true
        lastEmitMs = 0L
        location.start()
        compass.start()
        val opening = if (!location.hasPermission)
            OrientationGuidance(
                speech = "Orientation needs location permission to guide you.",
                braille = "no location permission",
                status = "Orientation: no location permission",
                confidence = OrientationConfidence.WEAK_GPS,
                arrived = false,
            )
        else OrientationGuidance(
            speech = "Orientation on. Acquiring GPS toward ${destinationName}.",
            braille = "acquiring GPS",
            status = "Orientation active. Acquiring GPS.",
            confidence = OrientationConfidence.WEAK_GPS,
            arrived = false,
        )
        lastGuidance = opening
        return opening
    }

    fun stop() {
        active = false
        location.stop()
        compass.stop()
        lastGuidance = null
    }

    /**
     * Called periodically by the processing loop. Returns guidance to emit, or null when inactive,
     * within the rate-limit window, or while a hazard owns the channel (so we never spam or talk over
     * a safety alert). [lastHazardMs] is the timestamp of the most recent hazard (0 = none).
     */
    fun tick(nowMs: Long, lastHazardMs: Long = 0L): OrientationGuidance? {
        if (!active) return null
        if (!arbiter.guidanceAllowed(nowMs, lastHazardMs)) return null
        if (nowMs - lastEmitMs < minUpdateMs) return null
        lastEmitMs = nowMs
        val g = compute()
        lastGuidance = g
        return g
    }

    /** "Repeat orientation": the last guidance, or a fresh computation if none yet. */
    fun repeat(): OrientationGuidance =
        if (active) (lastGuidance ?: compute().also { lastGuidance = it })
        else OrientationGuidance(
            "Orientation is off.", "Orientation off", "Orientation off",
            OrientationConfidence.WEAK_GPS, arrived = false,
        )

    /** Short, stable status line for the accessible status node. */
    fun statusLine(): String =
        if (!active) "Orientation off"
        else lastGuidance?.status ?: "Orientation active. Acquiring GPS."

    private fun compute(): OrientationGuidance = guidance.guide(
        current = location.current(),
        headingDeg = compass.headingDegrees,
        headingAccuracy = compass.accuracy,
        gps = if (location.current() == null) GpsAccuracy.NONE else location.accuracy,
        dest = destinations.current,
    )
}
