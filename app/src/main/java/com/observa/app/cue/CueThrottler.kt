package com.observa.app.cue

import com.observa.app.accessibility.OutputThrottler

/** Rate-limits non-speech cues so frequent low-level awareness does not overwhelm the user. */
class CueThrottler(private val cooldownMs: Long = 1_200L) {
    private val throttler = OutputThrottler(cooldownMs)
    fun allow(key: String, nowMs: Long): Boolean = throttler.shouldEmit(key, nowMs, cooldownMs)
    fun reset() = throttler.reset()
}
