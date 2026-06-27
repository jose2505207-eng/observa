package com.observa.app.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Thin wrapper around Android's on-device [SpeechRecognizer]. Prefers the on-device recognizer
 * (API 31+) and requests offline recognition; never uses cloud speech. If recognition is
 * unavailable, [available] is false and callers must fall back to accessible on-screen controls.
 *
 * Must be created and used on the main thread.
 */
class OfflineSpeechRecognizer(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    var available: Boolean = false
        private set

    private var onResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun init() {
        try {
            val onDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            recognizer = when {
                onDevice -> SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                SpeechRecognizer.isRecognitionAvailable(context) ->
                    SpeechRecognizer.createSpeechRecognizer(context)
                else -> null
            }
            available = recognizer != null
            recognizer?.setRecognitionListener(listener)
        } catch (e: Exception) {
            Log.e("OBSERVA_ASR", "Speech recognizer init failed", e)
            available = false
        }
    }

    /** True only when an on-device recognizer is in use (no network needed). */
    val onDeviceOnly: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        val r = recognizer
        if (r == null) { onError("Speech recognition unavailable"); return }
        this.onResult = onResult
        this.onError = onError
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        try {
            r.startListening(intent)
        } catch (e: Exception) {
            Log.e("OBSERVA_ASR", "startListening failed", e)
            onError("Could not start listening")
        }
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("OBSERVA_ASR", "stopListening failed", e)
        }
    }

    fun destroy() {
        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onResults(results: Bundle) {
            val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (text != null) onResult?.invoke(text) else onError?.invoke("No speech detected")
        }

        override fun onError(error: Int) {
            onError?.invoke("Recognition error $error")
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
