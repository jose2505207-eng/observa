package com.observa.app.cue

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays short, stereo-panned tones so the user can localize cues by ear (left object = left ear).
 * Tones are synthesized in memory (no assets, fully offline) and played on a background thread.
 */
class AudioCuePlayer {

    private val executor = Executors.newSingleThreadExecutor()
    private val sampleRate = 22_050

    /** @param pan -1f (left) .. +1f (right); urgent uses a higher, longer tone. */
    fun playCue(pan: Float, urgent: Boolean) {
        executor.execute {
            try {
                val durationMs = if (urgent) 240 else 110
                val freq = if (urgent) 880.0 else 440.0
                val data = synthesize(pan.coerceIn(-1f, 1f), freq, durationMs)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(data.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(data, 0, data.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 60)
                track.release()
            } catch (e: Exception) {
                Log.e("OBSERVA_AUDIO", "cue playback failed", e)
            }
        }
    }

    private fun synthesize(pan: Float, freq: Double, durationMs: Int): ShortArray {
        val frames = sampleRate * durationMs / 1000
        val out = ShortArray(frames * 2)
        // Linear pan with both channels audible at center.
        val leftGain = (1f - pan.coerceAtLeast(0f))
        val rightGain = (1f + pan.coerceAtMost(0f))
        val amp = 0.6
        for (i in 0 until frames) {
            // simple attack/decay envelope to avoid clicks
            val env = when {
                i < frames * 0.1 -> i / (frames * 0.1)
                i > frames * 0.8 -> (frames - i) / (frames * 0.2)
                else -> 1.0
            }
            val s = sin(2.0 * PI * freq * i / sampleRate) * amp * env
            out[2 * i] = (s * leftGain * Short.MAX_VALUE).toInt().toShort()
            out[2 * i + 1] = (s * rightGain * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    fun release() = executor.shutdown()
}
