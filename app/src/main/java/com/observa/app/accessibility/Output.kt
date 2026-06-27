package com.observa.app.accessibility

/** Relative importance of an output event. HAZARD always wins. */
enum class OutputPriority { HAZARD, NAVIGATION, OCR, MODE, INFO }

/**
 * A single unit of user-facing output. The same event drives speech, on-screen text,
 * TalkBack live regions, and braille-display status, so every channel stays consistent.
 *
 * @param braille short, structured text for braille displays / status (e.g. "Person right. 2 o'clock.")
 * @param pan stereo pan hint in [-1f, 1f]; -1 = full left, +1 = full right (0 = center).
 */
data class OutputEvent(
    val priority: OutputPriority,
    val speech: String,
    val braille: String = speech,
    val urgent: Boolean = priority == OutputPriority.HAZARD,
    val pan: Float = 0f,
    val timestampMs: Long = 0L,
)
