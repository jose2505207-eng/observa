package com.observa.app.navigation

/**
 * Navigation Mode = real compass + GPS bearing guidance ([OrientationController]) plus an honest
 * offline **map-pack** status ([OfflineMapPackManager]). Compass/bearing guidance works **without** a
 * map pack; the pack only adds the rendered-map/route layer. This controller is the thin seam the UI
 * and accessibility layer drive, so the live guidance, the map status, and a short braille line all
 * come from one place. Hazard safety is enforced upstream (router priority + [NavigationSafetyArbiter]).
 *
 * Pure status composition (the location/compass Android bits live in the injected providers), so the
 * status logic is unit-testable.
 */
class NavigationModeController(
    private val orientation: OrientationController,
    private val mapStatus: () -> MapPackStatus,
    private val hasRenderableMap: () -> Boolean = { mapStatus() == MapPackStatus.READY_OFFLINE },
) {
    val active: Boolean get() = orientation.active

    fun start(): OrientationGuidance = orientation.start()
    fun stop() = orientation.stop()
    fun repeat(): OrientationGuidance = orientation.repeat()

    /** "Map ready offline" / "Map pack missing" — honest about the optional rendered-map layer. */
    fun mapLine(): String = when (mapStatus()) {
        MapPackStatus.READY_OFFLINE -> "Map ready offline"
        MapPackStatus.CORRUPT -> "Map pack corrupt"
        MapPackStatus.DOWNLOAD_REQUIRED, MapPackStatus.NOT_INSTALLED -> "Map pack missing"
    }

    /**
     * One short braille/TalkBack status line. While active it leads with live guidance (so a braille
     * user reads "Destination ahead-left, 40 meters"); the map layer is reported as a suffix.
     */
    fun statusLine(): String {
        val map = mapLine()
        return if (!active) "Navigation ready. $map (compass guidance works without a map)."
        else "${orientation.statusLine()}. $map."
    }
}
