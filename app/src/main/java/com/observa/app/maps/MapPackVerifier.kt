package com.observa.app.maps

import java.io.File

/**
 * Verifies a map pack is real before it is reported "Ready offline" — so status is never faked.
 * A demo pack is an OBSERVA waypoint bundle (header [DEMO_HEADER]); a real Mapsforge `.map` starts with
 * its own "mapsforge binary OSM" magic. Either non-empty, readable, recognized file counts as valid.
 */
object MapPackVerifier {

    const val DEMO_HEADER = "OBSERVA_MAP_V1"
    private const val MAPSFORGE_MAGIC = "mapsforge binary OSM"

    fun isValid(file: File): Boolean {
        if (!file.isFile || !file.canRead() || file.length() == 0L) return false
        return runCatching {
            file.inputStream().use { input ->
                val head = ByteArray(32)
                val n = input.read(head)
                if (n <= 0) return false
                val text = String(head, 0, n, Charsets.ISO_8859_1)
                text.startsWith(DEMO_HEADER) || text.startsWith(MAPSFORGE_MAGIC)
            }
        }.getOrDefault(false)
    }
}
