package com.observa.app.navigation

import android.content.Context
import java.io.File

/** Honest lifecycle of an offline map pack. Runtime is offline; a pack may need a one-time download. */
enum class MapPackStatus(val label: String) {
    NOT_INSTALLED("Map pack not installed"),
    DOWNLOAD_REQUIRED("Map pack download required"),
    CORRUPT("Map pack corrupt"),
    READY_OFFLINE("Map ready offline"),
}

/**
 * Tracks whether an **offline** map pack is present on the device. OBSERVA never routes over the
 * network, so navigation map rendering/route data must be a local file (Mapsforge `.map` or a MapLibre
 * offline region) provisioned out of band — see `docs/implementation/OFFLINE_MAPS.md`. No pack is
 * bundled in the no-INTERNET runtime build, so this reports honestly until one is sideloaded into
 * `filesDir/map_packs/`. Navigation compass guidance works **without** a pack; the pack only adds the
 * rendered map/route layer.
 */
class OfflineMapPackManager(private val context: Context) {

    private val packsDir: File get() = File(context.filesDir, PACK_DIR)

    /** Installed pack files (e.g. "seoul.map"), or empty if none provisioned. */
    fun installedPacks(): List<String> =
        packsDir.takeIf { it.isDirectory }?.listFiles()
            ?.filter { it.isFile && it.name.endsWith(MAP_EXT) }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()

    val hasAnyPack: Boolean get() = installedPacks().isNotEmpty()

    fun status(): MapPackStatus {
        val dir = packsDir
        if (!dir.isDirectory) return MapPackStatus.NOT_INSTALLED
        val maps = dir.listFiles()?.filter { it.name.endsWith(MAP_EXT) } ?: emptyList()
        if (maps.isEmpty()) return MapPackStatus.DOWNLOAD_REQUIRED
        // A non-empty, readable file is "ready"; a zero-byte/unreadable file is corrupt.
        if (maps.any { !it.canRead() || it.length() == 0L }) return MapPackStatus.CORRUPT
        return MapPackStatus.READY_OFFLINE
    }

    fun statusLine(): String = status().label

    companion object {
        const val PACK_DIR = "map_packs"
        const val MAP_EXT = ".map"
    }
}
