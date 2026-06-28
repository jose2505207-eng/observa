package com.observa.app.maps

import android.content.Context
import java.io.File

/** An installed map pack on disk. */
data class InstalledMapPack(val name: String, val file: File, val sizeBytes: Long, val valid: Boolean)

/**
 * Read/verify access to offline map packs under `filesDir/map_packs/`. This is the shared on-disk
 * contract: [MapDownloadController] writes here, the navigation layer reads "is a map present" from the
 * same directory. No network. "Ready offline" requires [MapPackVerifier] to pass.
 */
class MapPackRepository(private val context: Context) {

    val packsDir: File get() = File(context.filesDir, PACK_DIR).apply { if (!exists()) mkdirs() }

    fun installed(): List<InstalledMapPack> =
        packsDir.listFiles()?.filter { it.isFile && it.name.endsWith(MAP_EXT) }
            ?.map { InstalledMapPack(it.name, it, it.length(), MapPackVerifier.isValid(it)) }
            ?.sortedBy { it.name }
            ?: emptyList()

    fun status(): MapPackStatus {
        val packs = installed()
        return when {
            packs.isEmpty() -> MapPackStatus.NOT_INSTALLED
            packs.any { !it.valid } && packs.none { it.valid } -> MapPackStatus.CORRUPT
            packs.any { it.valid } -> MapPackStatus.READY_OFFLINE
            else -> MapPackStatus.CORRUPT
        }
    }

    fun delete(name: String): Boolean = File(packsDir, name).takeIf { it.isFile }?.delete() ?: false

    companion object {
        const val PACK_DIR = "map_packs"
        const val MAP_EXT = ".map"
    }
}
