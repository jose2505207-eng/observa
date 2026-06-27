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

    /** Speak [text]. High-severity alerts flush the queue; others queue behind. */
    fun speak(text: String, urgent: Boolean = false) {
        if (muted || !ready) return
        val mode = if (urgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts.speak(text, mode, null, text.hashCode().toString())
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
