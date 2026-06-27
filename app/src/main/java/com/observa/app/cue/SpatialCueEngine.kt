package com.observa.app.cue

import com.observa.app.hazard.Direction
import com.observa.app.hazard.Hazard

/**
 * Turns a hazard into coordinated stereo-audio + directional-haptic cues, throttled so frequent
 * detections do not overwhelm the user. Speech is handled separately by the accessibility router;
 * these non-speech cues remain active even when speech is muted (safety).
 */
class SpatialCueEngine(
    private val audio: AudioCuePlayer,
    private val haptics: HapticCuePlayer,
    private val throttler: CueThrottler = CueThrottler(),
) {
    var hapticsEnabled: Boolean = true
    var audioEnabled: Boolean = true

    fun cue(hazard: Hazard, nowMs: Long) {
        val key = "${hazard.type}:${hazard.direction}"
        if (!throttler.allow(key, nowMs)) return
        val pan = panOf(hazard.direction)
        val urgent = hazard.severity == com.observa.app.hazard.Severity.HIGH
        if (audioEnabled) audio.playCue(pan, urgent)
        if (hapticsEnabled) haptics.forHazard(hazard)
    }

    fun confirmation() { if (hapticsEnabled) haptics.confirmation() }
    fun error() { if (hapticsEnabled) haptics.error() }

    private fun panOf(direction: Direction): Float = when (direction) {
        Direction.LEFT -> -1f
        Direction.RIGHT -> 1f
        Direction.CENTER, Direction.UNKNOWN -> 0f
    }

    fun release() = audio.release()
}
