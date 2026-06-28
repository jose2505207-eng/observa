package com.observa.app.navigation

/**
 * Decides **when** to run OCR on a likely sign/text region during Navigation Mode, so OCR never runs
 * every frame and never blocks hazard detection. A "candidate" frame (the detector saw a stable
 * sign-like object, e.g. a stop sign) must persist for [stableFrames] consecutive frames before a
 * single read is triggered; the tracker then arms again only after the candidate goes away. Pure
 * logic; unit-testable. OBSERVA never fabricates sign text — this only gates the real OCR pass.
 */
class StreetSignTracker(private val stableFrames: Int = 5, private val cooldownMs: Long = 6_000L) {

    private var streak = 0
    private var firedForThisStreak = false
    private var lastFireMs = -1L // -1 = never fired, so the first read is not blocked by the cooldown

    /**
     * Feed one frame. [candidate] = a sign/text-like region is present this frame. Returns true at most
     * once per stable streak (and respecting [cooldownMs]) to trigger a single OCR read.
     */
    fun onFrame(candidate: Boolean, nowMs: Long): Boolean {
        if (!candidate) { streak = 0; firedForThisStreak = false; return false }
        streak++
        val cooledDown = lastFireMs < 0L || nowMs - lastFireMs >= cooldownMs
        if (streak >= stableFrames && !firedForThisStreak && cooledDown) {
            firedForThisStreak = true
            lastFireMs = nowMs
            return true
        }
        return false
    }

    fun reset() { streak = 0; firedForThisStreak = false }
}
