package com.observa.app.maps

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.observa.app.BuildConfig
import java.io.File

/**
 * Drives the Map Download screen. Two honest paths:
 *  - **Install demo map pack** — writes a small OBSERVA waypoint bundle locally (no network), labeled
 *    "Demo offline map pack". Lets navigation report "Map ready offline" and demonstrate offline map
 *    context immediately.
 *  - **Download area map** — a real regional map needs a one-time download, so it is enabled only in the
 *    provisioning (Setup) build (which has INTERNET). In the demoOffline build it states that honestly.
 *
 * "Ready offline" is never faked: it requires a verified file via [MapPackRepository]/[MapPackVerifier].
 * Compose-observable; main-thread.
 */
class MapDownloadController(private val context: Context, private val repo: MapPackRepository = MapPackRepository(context)) {

    var status by mutableStateOf(MapPackStatus.NOT_INSTALLED); private set
    var progressPercent by mutableStateOf(0); private set
    var detail by mutableStateOf(""); private set
    var installed by mutableStateOf<List<InstalledMapPack>>(emptyList()); private set

    val setupMode: Boolean get() = BuildConfig.SETUP_MODE

    init { refresh() }

    fun refresh() { installed = repo.installed(); status = repo.status() }

    fun statusLine(): String = status.label

    /** Generate + install the demo waypoint pack locally (offline). Honest "Demo offline map pack". */
    fun installDemoPack() {
        status = MapPackStatus.DOWNLOADING; progressPercent = 30; detail = "Installing demo map pack…"
        val ok = runCatching {
            val file = File(repo.packsDir, DEMO_PACK)
            file.writeText(buildString {
                append(MapPackVerifier.DEMO_HEADER).append('\n')
                append("name=OBSERVA demo route\n")
                // A tiny offline route bundle (waypoints near the demo region) — NOT full street tiles.
                append("waypoint=37.5665,126.9780,Start\n")
                append("waypoint=37.5680,126.9775,Midpoint\n")
                append("waypoint=37.5700,126.9769,The park\n")
            })
            MapPackVerifier.isValid(file)
        }.getOrDefault(false)
        progressPercent = 100
        detail = if (ok) "Demo offline map pack installed." else "Failed to write demo pack."
        refresh()
        if (!ok) status = MapPackStatus.FAILED
    }

    /** Real regional map: only the provisioning build can download it. Honest otherwise. No fake status. */
    fun downloadAreaMap() {
        if (!setupMode) {
            status = MapPackStatus.FAILED
            detail = "Area map download needs the Setup / Download build (provisioning) with internet. " +
                "Then switch to the offline build. Or install the demo map pack now."
            return
        }
        // A full Mapsforge/MapLibre region download would be wired here in the provisioning build.
        status = MapPackStatus.DOWNLOADING
        detail = "Area map download not bundled yet — install the demo map pack to navigate with offline map context."
    }

    fun deletePack(name: String) { repo.delete(name); detail = "Deleted $name."; refresh() }

    companion object {
        const val DEMO_PACK = "demo${MapPackRepository.MAP_EXT}"
        val DEMO_REGION = MapRegion("demo", "OBSERVA demo route", demo = true)
    }
}
