package com.observa.app.accessibility

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Compose-observable, braille-friendly current status. Surfaced as a TalkBack live region so
 * connected braille displays show concise, structured OBSERVA state.
 */
class BrailleStatusState {
    var text by mutableStateOf("Mode: ambient awareness.")
        private set

    fun update(status: String) { text = status }
}
