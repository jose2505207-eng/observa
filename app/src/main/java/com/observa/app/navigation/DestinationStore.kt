package com.observa.app.navigation

import com.observa.app.nav.GeoPoint
import com.observa.app.nav.SavedDestination

/**
 * Holds the single active destination for GPS Orientation Lite. The mission requires the demo
 * destination to be configurable in code or a debug UI, so this is a tiny mutable holder seeded with
 * a documented demo coordinate; [setDestination] lets a debug control repoint it at runtime.
 *
 * Distinct from `nav.DestinationStore` (the saved-places list for the older navigate-to feature);
 * orientation only ever guides toward one target at a time. Reuses `nav.SavedDestination`.
 */
class DestinationStore(initial: SavedDestination = DEMO) {

    @Volatile var current: SavedDestination = initial
        private set

    fun setDestination(destination: SavedDestination) { current = destination }

    fun setDestination(name: String, lat: Double, lon: Double) =
        setDestination(SavedDestination(name, GeoPoint(lat, lon)))

    companion object {
        /** Documented offline demo target (Seoul City Hall area) so orientation is demonstrable. */
        val DEMO = SavedDestination("the demo destination", GeoPoint(37.5665, 126.9780))
    }
}
