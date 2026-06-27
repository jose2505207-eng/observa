package com.observa.app.accessibility

import com.observa.app.output.Speaker

/**
 * Single fan-out point for all user-facing output. Every event reaches speech (TTS), the
 * braille-friendly status (a TalkBack live region), and the last-message store for "Repeat".
 *
 * Non-urgent duplicates are debounced so TalkBack / braille are not flooded with frame-level
 * updates; HAZARD events bypass the debounce and flush the speech queue (they interrupt).
 */
class AccessibilityOutputRouter(
    private val speaker: Speaker,
    val brailleStatus: BrailleStatusState = BrailleStatusState(),
    private val lastMessageStore: LastMessageStore = LastMessageStore(),
    private val throttler: OutputThrottler = OutputThrottler(1_200L),
) {
    val muted: Boolean get() = speaker.muted

    /** When false, the Braille/live-region status stops updating; speech is unaffected. */
    @Volatile var brailleEnabled: Boolean = true

    fun setMuted(muted: Boolean) {
        speaker.muted = muted
        if (muted) speaker.stop()
    }

    fun emit(event: OutputEvent, nowMs: Long = System.currentTimeMillis()) {
        if (!event.urgent && !throttler.shouldEmit(event.braille, nowMs)) return
        if (brailleEnabled) brailleStatus.update(event.braille)
        lastMessageStore.set(event.speech)
        speaker.speak(event.speech, urgent = event.urgent)
    }

    /** Set the ambient composite status line (no speech). Respects [brailleEnabled]. */
    fun setStatusLine(text: String) {
        if (brailleEnabled) brailleStatus.update(text)
    }

    /** Force the status line regardless of [brailleEnabled] (used to show "Braille off"). */
    fun forceStatusLine(text: String) = brailleStatus.update(text)

    /** Replay the last meaningful message (the "Repeat" action). */
    fun repeatLast() {
        val last = lastMessageStore.last
        if (last.isNotBlank()) speaker.speak(last, urgent = false)
        else speaker.speak("No message to repeat.", urgent = false)
    }
}
