package com.observa.app.maps

/** Honest lifecycle of an offline map pack. "Ready offline" only when a verified file exists. */
enum class MapPackStatus(val label: String) {
    NOT_INSTALLED("Map pack not installed"),
    DOWNLOADING("Downloading map pack"),
    READY_OFFLINE("Map ready offline"),
    FAILED("Map download failed"),
    CORRUPT("Map pack corrupt"),
}

/** A downloadable/installable region. The demo pack is generated locally (offline) and labeled so. */
data class MapRegion(val id: String, val name: String, val demo: Boolean)
