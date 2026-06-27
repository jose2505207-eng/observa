package com.observa.app.accessibility

/**
 * A concise snapshot of OBSERVA state for a refreshable Braille display. Object labels appear only
 * when a real detector produced them (passed in via [hazard]); this type never invents them.
 */
data class BrailleSnapshot(
    val observing: Boolean,
    val muted: Boolean,
    /** Short AI model state: "fallback", "CPU", or "QNN". */
    val aiModel: String,
    /** Concise hazard line if one is active, e.g. "Obstacle center" / "Clear path". */
    val hazard: String? = null,
    /** Whether the current hazard is safety-critical (must not be dropped). */
    val urgent: Boolean = false,
)

/**
 * Formats short, structured lines suited to a Braille display / TalkBack. Pure logic, no Android
 * deps, fully unit-testable. An active hazard always wins the line (urgent or not) so safety text
 * is never buried behind ambient status; otherwise it shows a compact status summary.
 */
object BrailleStatusPresenter {

    fun format(s: BrailleSnapshot): String {
        s.hazard?.let { return it.trim() }
        val parts = ArrayList<String>(3)
        parts += if (s.observing) "observing on" else "observing off"
        if (s.muted) parts += "muted"
        parts += aiShort(s.aiModel)
        return "OBSERVA: " + parts.joinToString(", ")
    }

    private fun aiShort(aiModel: String): String = when {
        aiModel.contains("QNN", ignoreCase = true) -> "AI QNN"
        aiModel.contains("CPU", ignoreCase = true) -> "AI CPU"
        else -> "AI fallback"
    }
}
