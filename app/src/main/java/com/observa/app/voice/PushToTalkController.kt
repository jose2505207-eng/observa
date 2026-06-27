package com.observa.app.voice

/**
 * Coordinates push-to-talk: starts the recognizer, feeds recognized text to the [CommandRouter],
 * and reports listening state for UI/accessibility. When speech recognition is unavailable it
 * announces the accessible fallback rather than failing silently.
 */
class PushToTalkController(
    private val recognizer: OfflineSpeechRecognizer,
    private val router: CommandRouter,
    private val onState: (listening: Boolean, message: String) -> Unit,
) {
    var listening: Boolean = false
        private set

    fun onPressed() {
        if (!recognizer.available) {
            onState(false, "Voice unavailable. Use on-screen command buttons.")
            return
        }
        if (listening) return
        listening = true
        onState(true, "Listening")
        recognizer.startListening(
            onResult = { text ->
                listening = false
                onState(false, "Heard: $text")
                router.handleText(text)
            },
            onError = { msg ->
                listening = false
                onState(false, msg)
            },
        )
    }

    fun onReleased() {
        if (listening) recognizer.stopListening()
    }
}
