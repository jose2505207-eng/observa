package com.observa.app.input

/** A raw screen gesture the main surface can detect when TalkBack is OFF. */
enum class BlindGesture { SINGLE_TAP, DOUBLE_TAP, TRIPLE_TAP, SWIPE_UP, SWIPE_DOWN, LONG_PRESS }

/** The app action a gesture maps to. [NONE] = ignore (routed to native accessibility actions). */
enum class GestureAction { OPEN_VOICE, REPEAT_LAST, START_TRANSLATION, START_ORIENTATION, PUSH_TO_TALK, NONE }

/**
 * Two-layer, blind-first input brain. Raw single-finger screen gestures are only a reliable channel
 * **when TalkBack is OFF** — when TalkBack/touch-exploration is on, one-finger gestures are consumed
 * by the screen reader, so OBSERVA must route those users to the native `CustomAccessibilityAction`s
 * instead (Layer A). This class encodes that rule and the gesture→action mapping so it stays pure and
 * unit-testable; the Compose surface only does raw event detection and asks this brain what to do.
 *
 * Hazard alerts are unaffected: gesture actions emit at non-hazard priority, so a hazard always
 * interrupts (the output router enforces it).
 */
class BlindGestureController(private val isTalkBackOn: () -> Boolean) {

    /** Resolve a detected gesture to an action. Returns [GestureAction.NONE] when TalkBack is on. */
    fun resolve(gesture: BlindGesture): GestureAction {
        if (isTalkBackOn()) return GestureAction.NONE
        return when (gesture) {
            BlindGesture.TRIPLE_TAP -> GestureAction.OPEN_VOICE
            BlindGesture.DOUBLE_TAP -> GestureAction.REPEAT_LAST
            BlindGesture.SWIPE_UP -> GestureAction.START_TRANSLATION
            BlindGesture.SWIPE_DOWN -> GestureAction.START_ORIENTATION
            BlindGesture.LONG_PRESS -> GestureAction.PUSH_TO_TALK
            BlindGesture.SINGLE_TAP -> GestureAction.NONE // single tap is reserved for normal UI
        }
    }

    /** True only when raw gestures should be wired (TalkBack off). */
    fun rawGesturesActive(): Boolean = !isTalkBackOn()

    /** Map a completed tap count to a gesture, or null if it isn't an actionable multi-tap. */
    fun tapGesture(count: Int): BlindGesture? = when (count) {
        2 -> BlindGesture.DOUBLE_TAP
        3 -> BlindGesture.TRIPLE_TAP
        else -> null
    }

    /** Map a vertical drag delta (px; up is negative) to a swipe gesture past [thresholdPx]. */
    fun swipeGesture(dyPx: Float, thresholdPx: Float): BlindGesture? = when {
        dyPx <= -thresholdPx -> BlindGesture.SWIPE_UP
        dyPx >= thresholdPx -> BlindGesture.SWIPE_DOWN
        else -> null
    }

    /** Status line for sighted/normal use (TalkBack off). */
    val normalStatusLine =
        "Triple tap for voice commands. Swipe up translation. Swipe down navigation. Double tap repeats."

    /** Status line when TalkBack is on — points users to the guaranteed native path. */
    val accessibilityStatusLine = "Gestures available through TalkBack actions."

    fun statusLine(): String = if (isTalkBackOn()) accessibilityStatusLine else normalStatusLine
}
