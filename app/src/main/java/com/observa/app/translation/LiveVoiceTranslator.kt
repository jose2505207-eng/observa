package com.observa.app.translation

import com.observa.app.voice.OfflineSpeechRecognizer

/**
 * Real-time **voice-to-voice** translation: listen (on-device speech) → translate offline (ML Kit) →
 * speak the result in the target language → listen again. Fully offline once the language packs and TTS
 * voices are installed; never fabricates — if a stage is unavailable it says so. Source→target in one
 * direction at a time; [swap] flips it for a two-way conversation.
 *
 * Android-coupled orchestration (owns the shared recognizer while active); the pure ML Kit translate +
 * readiness logic it relies on is unit-tested separately.
 */
class LiveVoiceTranslator(
    private val recognizer: OfflineSpeechRecognizer,
    private val translator: MlKitOnDeviceTranslator,
    /** Speak [text] in [code]; returns false if no TTS voice for that language is installed. */
    private val speakIn: (text: String, code: String) -> Boolean,
    private val onState: (String) -> Unit,
) {
    @Volatile var active = false; private set
    @Volatile var source = "es"; private set
    @Volatile var target = "en"; private set
    @Volatile private var continuous = true

    fun start(source: String, target: String, continuous: Boolean = true) {
        if (!recognizer.available) { onState("Voice input unavailable offline on this device."); return }
        this.source = source; this.target = target; this.continuous = continuous
        active = true
        onState("Live translation on: ${source} to ${target}. Speak now.")
        listen()
    }

    fun stop() {
        active = false
        runCatching { recognizer.stopListening() }
        onState("Live translation stopped.")
    }

    /** Flip translation direction (two-way conversation). */
    fun swap() {
        val t = source; source = target; target = t
        onState("Now translating ${source} to ${target}.")
    }

    private fun listen() {
        if (!active) return
        recognizer.startListening(
            onResult = { text -> handle(text) },
            onError = { msg -> if (active && continuous) listen() else { active = false; onState(msg) } },
        )
    }

    private fun handle(text: String) {
        if (text.isBlank()) { if (active && continuous) listen(); return }
        translator.translate(text, source, target) { ok, out ->
            if (ok) {
                val spoke = speakIn(out, target)
                onState(if (spoke) "“$text” → “$out”" else "“$text” → “$out” (no $target voice installed; text only)")
            } else {
                onState(out) // honest error (e.g. language pack missing)
            }
            if (active && continuous) listen()
        }
    }
}
