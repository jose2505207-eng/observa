package com.observa.app.accessibility

/** Relative importance of an output event. HAZARD always wins. Ordinal order = priority order. */
enum class OutputPriority { HAZARD, NAVIGATION, OCR, MODE, INFO }

/** Direction used to steer stereo audio panning and directional haptics. */
enum class CueDirection { LEFT, RIGHT, CENTER, NONE }

/**
 * A single unit of user-facing output. The same event drives speech, on-screen text,
 * TalkBack/Braille live regions, directional audio/haptic cues, and the repeat store, so every
 * channel stays consistent.
 *
 * @param braille short, structured text for braille displays / status (e.g. "Person right").
 * @param direction direction for audio panning / directional haptics (NONE = no cue).
 */
data class OutputEvent(
    val priority: OutputPriority,
    val speech: String,
    val braille: String = speech,
    val urgent: Boolean = priority == OutputPriority.HAZARD,
    val direction: CueDirection = CueDirection.NONE,
    val timestampMs: Long = 0L,
)

/** Receives non-speech cues. Implemented by the spatial cue engine. */
interface CueSink {
    fun hazardCue(direction: CueDirection, urgent: Boolean, nowMs: Long)
    fun confirmation()
    fun error()
}

/**
 * Pure priority arbitration, unit-testable. Orders events so HAZARD > NAVIGATION > OCR > MODE >
 * INFO. A stable sort preserves submission order within a priority level.
 */
object OutputArbiter {
    fun sortByPriority(events: List<OutputEvent>): List<OutputEvent> =
        events.sortedBy { it.priority.ordinal }

    fun highest(events: List<OutputEvent>): OutputEvent? =
        events.minByOrNull { it.priority.ordinal }
}
