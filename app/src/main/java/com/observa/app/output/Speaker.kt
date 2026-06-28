package com.observa.app.output

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/** Thin TextToSpeech wrapper. Speech is the primary output channel; can be muted. */
class Speaker(context: Context) {

    @Volatile var ready: Boolean = false
        private set
    @Volatile var muted: Boolean = false

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                tts.setSpeechRate(1.0f)
                ready = true
            } else {
                Log.e("OBSERVA_TTS", "TextToSpeech init failed: $status")
            }
        }
    }

    /** Speak [text]. High-severity alerts flush the queue; others queue behind. Always English. */
    fun speak(text: String, urgent: Boolean = false) {
        if (muted || !ready) return
        if (tts.language != Locale.US) tts.language = Locale.US // alerts are always English
        val mode = if (urgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts.speak(text, mode, null, text.hashCode().toString())
    }

    /**
     * Speak [text] in the given BCP-47 [languageCode] (for translated speech / voice-to-voice). If the
     * voice for that language isn't installed, returns false so the caller can say so honestly. Not
     * muted by the alert mute (translation output is user-requested), but skipped if TTS isn't ready.
     */
    fun speakIn(text: String, languageCode: String): Boolean {
        if (!ready) return false
        val loc = Locale.forLanguageTag(languageCode)
        val avail = runCatching { tts.isLanguageAvailable(loc) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (avail < TextToSpeech.LANG_AVAILABLE) return false
        tts.language = loc
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
        return true
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() {
        try {
            tts.stop()
            tts.shutdown()
        } catch (e: Exception) {
            Log.e("OBSERVA_TTS", "shutdown failed", e)
        }
    }
}
