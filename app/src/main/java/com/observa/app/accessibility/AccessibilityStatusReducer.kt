package com.observa.app.accessibility

/** Which real inference path is active, in short braille-friendly terms. */
enum class DetectorBackend(val label: String) {
    XNNPACK("XNNPACK"),
    QNN("QNN/NPU"),
    HEURISTIC("heuristic fallback"),
}

/**
 * Immutable snapshot of the app state the accessibility operating layer needs. Kept free of Android
 * types so the reducer stays pure and unit-testable.
 */
data class A11yState(
    val awarenessActive: Boolean,
    val muted: Boolean,
    val detector: DetectorBackend,
    val ocrReady: Boolean,
    val translationInstalled: Boolean,
    val navigating: Boolean,
    /** Raw last hazard line (already short), or null/blank if nothing meaningful yet. */
    val lastAlert: String?,
    /** Short orientation line when GPS Orientation Lite is active, else null. */
    val orientation: String? = null,
)

/**
 * Pure derivation of the stable accessible node texts and semantic state descriptions that the
 * native TalkBack / braille operating layer presents. No Android deps; fully unit-tested.
 *
 * Design rules:
 *  - Output is short and stable so a refreshable braille display is not flooded.
 *  - Debug detail (confidence, bounding boxes, latency) never appears here — see [stripDebug].
 *  - Nothing is invented: object labels only ever come from [A11yState.lastAlert], which the
 *    detector/hazard pipeline supplies.
 */
object AccessibilityStatusReducer {

    private const val NO_ALERT = "No alerts yet"

    /** First-class "Current status" node: one concise line covering the safety-relevant state. */
    fun currentStatus(s: A11yState): String {
        val parts = ArrayList<String>(4)
        parts += awarenessState(s)
        parts += detectorState(s)
        parts += ocrState(s)
        if (s.muted) parts += "Speech muted"
        if (s.navigating) parts += "Navigating"
        s.orientation?.takeIf { it.isNotBlank() }?.let { parts += it.trimEnd('.') }
        return parts.joinToString(". ") + "."
    }

    /** "Last alert" node. Honest placeholder when nothing has fired; sanitized of debug detail. */
    fun lastAlert(s: A11yState): String {
        val raw = s.lastAlert?.let { stripDebug(it).trim() }.orEmpty()
        if (raw.isEmpty() || raw == NO_ALERT) return "Last alert: none"
        return "Last alert: ${raw.replaceFirstChar { it.lowercase() }}"
    }

    /** "Available actions" node text. The actionable verbs also exist as custom semantic actions. */
    fun availableActions(s: A11yState): String =
        "Available actions: " +
            (if (s.awarenessActive) "pause awareness" else "start awareness") +
            ", " + (if (s.orientation != null) "stop navigation" else "navigate") +
            ", download map, translate, download languages, voice commands, read signs, " +
            "repeat last alert, repeat translation, " +
            (if (s.translationInstalled) "translation ready" else "translation not installed") +
            ", silence alerts, debug status. Open the actions menu to choose."

    // --- Semantic state descriptions (stateDescription) ---

    fun awarenessState(s: A11yState): String =
        if (s.awarenessActive) "Awareness active" else "Awareness paused"

    fun detectorState(s: A11yState): String = "Detector backend: ${s.detector.label}"

    fun ocrState(s: A11yState): String = if (s.ocrReady) "OCR ready" else "OCR unavailable"

    fun translationState(s: A11yState): String =
        if (s.translationInstalled) "Translation ready" else "Translation not installed"

    /**
     * Defensive guard so engineering/debug fragments can never reach normal user output. Drops
     * anything from a debug marker onward (e.g. "Obstacle ahead confidence 0.71 at bbox 3,4,5,6"
     * → "Obstacle ahead").
     */
    fun stripDebug(line: String): String {
        var out = line
        for (marker in DEBUG_MARKERS) {
            val i = out.indexOf(marker, ignoreCase = true)
            if (i >= 0) out = out.substring(0, i)
        }
        return out.trim().trimEnd(',', ';', '·', '-').trim()
    }

    private val DEBUG_MARKERS = listOf(" confidence", " conf ", " bbox", " at bbox", " score", " iou")
}
