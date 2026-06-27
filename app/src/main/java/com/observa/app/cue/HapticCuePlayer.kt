package com.observa.app.cue

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.observa.app.hazard.Direction
import com.observa.app.hazard.Hazard
import com.observa.app.hazard.Severity

/** Directional and severity-based vibration patterns. Safety haptics keep working when muted. */
class HapticCuePlayer(context: Context) {

    private val vibrator: Vibrator? = run {
        val app = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Vibrator::class.java)
        }
    }

    val available: Boolean get() = vibrator?.hasVibrator() == true

    fun leftPulse() = waveform(longArrayOf(0, 60, 120, 60))
    fun rightPulse() = waveform(longArrayOf(0, 120, 60, 120))
    fun forwardHazard() = oneShot(180)
    fun escalatingStop() = waveform(longArrayOf(0, 80, 60, 120, 60, 200))
    fun confirmation() = oneShot(40)
    fun error() = waveform(longArrayOf(0, 200, 100, 200))

    /** Map a hazard to the most useful pattern. */
    fun forHazard(hazard: Hazard) {
        when {
            hazard.severity == Severity.HIGH -> escalatingStop()
            hazard.direction == Direction.LEFT -> leftPulse()
            hazard.direction == Direction.RIGHT -> rightPulse()
            else -> forwardHazard()
        }
    }

    private fun oneShot(ms: Long) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun waveform(pattern: LongArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}
