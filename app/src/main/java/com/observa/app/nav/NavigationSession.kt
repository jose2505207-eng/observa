package com.observa.app.nav

/** Supplies the current position + heading. Implementations may be real sensors or a demo source. */
interface NavFixProvider {
    fun current(): NavFix
    /** Honest label for the fix source, e.g. "live GPS" or "demo location". */
    val sourceLabel: String
}

/**
 * Holds an active navigation target and decides *when* to speak guidance, so the user is guided
 * without being flooded. Pure logic; unit-testable. Re-announces on a time interval, on a
 * meaningful clock-direction change, or on arrival. Hazards override navigation at the output
 * router (NAVIGATION priority), not here.
 */
class NavigationSession(
    private val engine: GuidanceEngine = GuidanceEngine(),
    private val minRepeatMs: Long = 6_000L,
) {
    var destination: SavedDestination? = null
        private set
    val active: Boolean get() = destination != null

    private var lastSpeakMs = 0L
    private var lastBraille: String? = null

    fun start(dest: SavedDestination) {
        destination = dest
        lastSpeakMs = 0L
        lastBraille = null
    }

    fun stop() {
        destination = null
    }

    /**
     * Produce guidance to speak now, or null if it should stay quiet this tick. Returns the final
     * arrival message exactly once, then ends the session.
     */
    fun tick(fix: NavFix, nowMs: Long): NavGuidance? {
        val dest = destination ?: return null
        val g = engine.guide(fix, dest)
        val changed = g.braille != lastBraille
        val due = nowMs - lastSpeakMs >= minRepeatMs
        if (g.arrived) {
            destination = null
            lastBraille = g.braille
            lastSpeakMs = nowMs
            return g
        }
        if (changed || due) {
            lastBraille = g.braille
            lastSpeakMs = nowMs
            return g
        }
        return null
    }
}
