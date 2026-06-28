package com.observa.app.translation

import android.content.Context
import java.io.File

/**
 * Tracks whether an **offline** translation language pack is installed on the device. OBSERVA never
 * translates over the network, so a pack must be present locally before Translation Mode can do real
 * work. No pack is bundled in the no-INTERNET runtime build (see `docs/implementation/OFFLINE_TRANSLATION.md`),
 * so this reports `false` honestly until a pack is provisioned (a one-time, offline-after-install step).
 *
 * A pack is considered present if a non-empty model directory exists under the app's files dir. This
 * lets a future provisioning/debug flavor drop a pack in without any runtime code or permission change.
 */
class OfflineLanguagePackManager(private val context: Context) {

    private val packsDir: File get() = File(context.filesDir, PACK_DIR)

    /** Installed pack identifiers (e.g. "es-en"), or empty if none provisioned. */
    fun installedPacks(): List<String> =
        packsDir.takeIf { it.isDirectory }?.listFiles()
            ?.filter { it.isDirectory && (it.listFiles()?.isNotEmpty() == true) }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()

    val hasAnyPack: Boolean get() = installedPacks().isNotEmpty()

    companion object {
        const val PACK_DIR = "translation_packs"
    }
}
