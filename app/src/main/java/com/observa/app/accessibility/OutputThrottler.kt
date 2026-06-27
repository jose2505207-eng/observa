package com.observa.app.accessibility

/**
 * Per-key rate limiter. Prevents flooding TalkBack / braille / audio with repeated messages.
 * Pure logic (no Android deps) so it is unit-testable.
 */
class OutputThrottler(private val defaultCooldownMs: Long = 1_500L) {

    private val lastEmit = HashMap<String, Long>()

    /** Returns true if [key] may emit at [nowMs] (and records it); false if still cooling down. */
    fun shouldEmit(key: String, nowMs: Long, cooldownMs: Long = defaultCooldownMs): Boolean {
        val prev = lastEmit[key]
        if (prev != null && nowMs - prev < cooldownMs) return false
        lastEmit[key] = nowMs
        return true
    }

    fun reset() = lastEmit.clear()
}
