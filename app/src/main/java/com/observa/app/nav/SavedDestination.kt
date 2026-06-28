package com.observa.app.nav

/** A user-saved place for offline, guidance-first navigation. */
data class SavedDestination(val name: String, val point: GeoPoint)

/**
 * In-memory store of saved destinations plus name lookup. Offline by construction. A small demo
 * set is preloaded so the guidance path is usable without external map data ([[offline-navigation]]).
 * Pure logic; unit-testable.
 */
class DestinationStore(seed: List<SavedDestination> = DEMO) {

    private val items = ArrayList(seed)

    fun all(): List<SavedDestination> = items.toList()

    fun add(destination: SavedDestination) {
        items.removeAll { it.name.equals(destination.name, ignoreCase = true) }
        items.add(destination)
    }

    /** Case-insensitive, whitespace-tolerant lookup; exact match preferred, else contains. */
    fun find(query: String): SavedDestination? {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return null
        return items.firstOrNull { it.name.lowercase() == q }
            ?: items.firstOrNull { it.name.lowercase().contains(q) }
    }

    companion object {
        /** Demo region (documented, offline) so navigation guidance is demonstrable. */
        val DEMO = listOf(
            SavedDestination("home", GeoPoint(37.5665, 126.9780)),
            SavedDestination("the park", GeoPoint(37.5700, 126.9769)),
            SavedDestination("the station", GeoPoint(37.5547, 126.9707)),
        )
    }
}
