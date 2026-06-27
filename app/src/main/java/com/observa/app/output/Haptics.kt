package com.observa.app.output

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.observa.app.hazard.Severity

/** Vibration feedback. Pattern strength scales with hazard [Severity]. */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = run {
        val appCtx = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = appCtx.getSystemService(VibratorManager::class.java)
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appCtx.getSystemService(Vibrator::class.java)
        }
    }

    val available: Boolean get() = vibrator?.hasVibrator() == true

    fun pulse(severity: Severity) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        // minSdk is 26, so VibrationEffect is always available.
        val effect = when (severity) {
            Severity.HIGH -> VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1)
            Severity.MEDIUM -> VibrationEffect.createOneShot(160, VibrationEffect.DEFAULT_AMPLITUDE)
            Severity.LOW -> VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        v.vibrate(effect)
    }
}
