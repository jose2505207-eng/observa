package com.observa.app.cue

import com.observa.app.accessibility.CueDirection
import com.observa.app.accessibility.CueSink
import com.observa.app.hazard.Hazard

/**
 * Turns events into coordinated stereo-audio + directional-haptic cues, throttled so frequent
 * detections do not overwhelm the user. Speech is handled separately by the accessibility router;
 * these non-speech cues remain active even when speech is muted (safety). Audio and haptics each
 * have an independent on/off setting.
 */
class SpatialCueEngine(
    private val audio: AudioCuePlayer,
    private val haptics: HapticCuePlayer,
    private val throttler: CueThrottler = CueThrottler(),
) : CueSink {
    var hapticsEnabled: Boolean = true
    var audioEnabled: Boolean = true

    val hapticsAvailable: Boolean get() = haptics.available

    override fun hazardCue(direction: CueDirection, urgent: Boolean, nowMs: Long) {
        if (!throttler.allow("$direction:$urgent", nowMs)) return
        if (audioEnabled) audio.playCue(panOf(direction), urgent)
        if (hapticsEnabled) haptics.forDirection(direction, urgent)
    }

    companion object {
        /** Pure stereo-pan mapping: LEFT=-1, RIGHT=+1, CENTER/NONE=0. Unit-testable. */
        fun panOf(direction: CueDirection): Float = when (direction) {
            CueDirection.LEFT -> -1f
            CueDirection.RIGHT -> 1f
            CueDirection.CENTER, CueDirection.NONE -> 0f
        }
    }

    /** Convenience for hazard objects (maps to the directional cue). */
    fun cue(hazard: Hazard, nowMs: Long) =
        hazardCue(directionOf(hazard), hazard.severity == com.observa.app.hazard.Severity.HIGH, nowMs)

    override fun confirmation() {
        if (audioEnabled) audio.playConfirmation()
        if (hapticsEnabled) haptics.confirmation()
    }

    override fun error() {
        if (audioEnabled) audio.playError()
        if (hapticsEnabled) haptics.error()
    }

    private fun directionOf(hazard: Hazard): CueDirection = when (hazard.direction) {
        com.observa.app.hazard.Direction.LEFT -> CueDirection.LEFT
        com.observa.app.hazard.Direction.RIGHT -> CueDirection.RIGHT
        com.observa.app.hazard.Direction.CENTER -> CueDirection.CENTER
        com.observa.app.hazard.Direction.UNKNOWN -> CueDirection.NONE
    }

    fun release() = audio.release()
}
