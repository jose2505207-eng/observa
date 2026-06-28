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

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Download a **real** offline map of the area around [lat],[lon]: named places (shops, amenities,
     * transit, etc.) from OpenStreetMap via the free Overpass API (no key). Stored locally as
     * `area.map` waypoints and usable offline afterwards as navigation destinations. Needs INTERNET, so
     * only the provisioning (Setup) build runs the network call; otherwise it says so honestly.
     */
    fun downloadAreaMap(lat: Double, lon: Double, onComplete: (String) -> Unit = {}) {
        if (!setupMode) {
            status = MapPackStatus.FAILED
            detail = "Area map download needs the Setup / Download build (internet). Install the demo map pack to use offline navigation now."
            onComplete(detail); return
        }
        status = MapPackStatus.DOWNLOADING; progressPercent = 15; detail = "Finding places near you…"
        Thread {
            val places = runCatching { fetchOverpassPlaces(lat, lon) }.getOrDefault(emptyList())
            mainHandler.post {
                if (places.isEmpty()) {
                    status = MapPackStatus.FAILED; progressPercent = 0
                    detail = "No places found (or no connection). Try again outdoors with internet."
                } else {
                    val file = File(repo.packsDir, AREA_PACK)
                    runCatching {
                        file.writeText(buildString {
                            append(MapPackVerifier.DEMO_HEADER).append('\n')
                            append("name=Area around $lat,$lon\n")
                            places.forEach { (name, plat, plon) -> append("waypoint=$plat,$plon,$name\n") }
                        })
                    }
                    progressPercent = 100
                    detail = "Installed ${places.size} nearby places. Offline navigation ready for your area."
                    refresh()
                }
                onComplete(detail)
            }
        }.start()
    }

    /** Backward-compatible no-arg variant used by the screen button (no location → honest message). */
    fun downloadAreaMap() {
        status = MapPackStatus.FAILED
        detail = "Say \"download map\" or use the Navigate screen so OBSERVA has your GPS location, then it downloads the area map."
    }

    /** Query Overpass for named places within ~800 m. Returns (name, lat, lon). Network; off the UI thread. */
    private fun fetchOverpassPlaces(lat: Double, lon: Double): List<Triple<String, Double, Double>> {
        val query = "[out:json][timeout:25];(" +
            "node(around:800,$lat,$lon)[name][amenity];" +
            "node(around:800,$lat,$lon)[name][shop];" +
            "node(around:800,$lat,$lon)[name][public_transport];" +
            ");out 80;"
        val url = java.net.URL("https://overpass-api.de/api/interpreter")
        val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 15000; readTimeout = 30000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        conn.outputStream.use { it.write(("data=" + java.net.URLEncoder.encode(query, "UTF-8")).toByteArray()) }
        if (conn.responseCode != 200) { conn.disconnect(); return emptyList() }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val out = ArrayList<Triple<String, Double, Double>>()
        val elements = org.json.JSONObject(body).optJSONArray("elements") ?: return emptyList()
        for (i in 0 until elements.length()) {
            val e = elements.getJSONObject(i)
            val name = e.optJSONObject("tags")?.optString("name").orEmpty()
            if (name.isBlank()) continue
            val plat = e.optDouble("lat", Double.NaN); val plon = e.optDouble("lon", Double.NaN)
            if (plat.isNaN() || plon.isNaN()) continue
            out.add(Triple(name, plat, plon))
        }
        return out.distinctBy { it.first.lowercase() }
    }

    fun deletePack(name: String) { repo.delete(name); detail = "Deleted $name."; refresh() }

    companion object {
        const val DEMO_PACK = "demo${MapPackRepository.MAP_EXT}"
        const val AREA_PACK = "area${MapPackRepository.MAP_EXT}"
        val DEMO_REGION = MapRegion("demo", "OBSERVA demo route", demo = true)
    }
}
