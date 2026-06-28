package com.observa.app.hotkey

/** Physical keys OBSERVA can act on (volume rocker). */
enum class HotkeyButton { VOLUME_UP, VOLUME_DOWN }

/** Commands triggered by counted key presses. */
enum class HotkeyCommand {
    REPEAT_LAST, STATUS, READ_TEXT, FIND_EXIT, VOICE_COMMANDS,
    MUTE_TOGGLE, STOP_NAVIGATION, EMERGENCY_PAUSE,
    NONE,
}

/**
 * Pure, time-driven counter for volume-button "press N times" shortcuts. The caller feeds presses
 * with timestamps and calls [flush] when the rolling window elapses (a Handler does this on device).
 * No Android deps → fully unit-testable. Debounce drops repeats closer than [debounceMs]; a press
 * for a *different* button resets the run. The window is per-run: presses within [windowMs] of the
 * previous press accumulate.
 */
class HotkeySequencer(
    private val windowMs: Long = 1_200L,
    private val debounceMs: Long = 60L,
) {
    private var button: HotkeyButton? = null
    private var count = 0
    private var lastPressMs = 0L

    val pending: Boolean get() = button != null

    /** Returns true if the press was accepted (false if debounced/ignored). */
    fun onPress(b: HotkeyButton, nowMs: Long): Boolean {
        if (button == b && nowMs - lastPressMs < debounceMs) return false // debounce repeats
        if (button != b) { button = b; count = 0 } // different button → new run
        count++
        lastPressMs = nowMs
        return true
    }

    /** True if the current run has timed out and should be resolved. */
    fun isExpired(nowMs: Long): Boolean = button != null && nowMs - lastPressMs >= windowMs

    /** Resolve and clear the current run into a command (NONE if nothing/unmapped). */
    fun flush(): HotkeyCommand {
        val cmd = map(button, count)
        button = null; count = 0
        return cmd
    }

    companion object {
        fun map(button: HotkeyButton?, count: Int): HotkeyCommand = when (button) {
            // Mission constraint: no more than three clicks per volume button. Find-exit is reached
            // by voice ("find exit") or the front-tap map instead of a 4th press.
            HotkeyButton.VOLUME_UP -> when (count) {
                1 -> HotkeyCommand.REPEAT_LAST
                2 -> HotkeyCommand.STATUS
                3 -> HotkeyCommand.VOICE_COMMANDS // tap volume-up 3x → open voice commands
                else -> HotkeyCommand.NONE
            }
            HotkeyButton.VOLUME_DOWN -> when (count) {
                1 -> HotkeyCommand.MUTE_TOGGLE
                2 -> HotkeyCommand.STOP_NAVIGATION
                3 -> HotkeyCommand.EMERGENCY_PAUSE
                else -> HotkeyCommand.NONE
            }
            null -> HotkeyCommand.NONE
        }
    }
}
