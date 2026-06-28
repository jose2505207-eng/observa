package com.observa.app.navigation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.observa.app.nav.HeadingAccuracy

/** Minimal heading surface so [OrientationController] stays unit-testable (see [CompassProvider]). */
interface HeadingSource {
    fun start()
    fun stop()
    /** Current compass heading in degrees [0,360), 0 = magnetic north. */
    val headingDegrees: Double
    val accuracy: HeadingAccuracy
}

/**
 * Device compass heading from the rotation-vector sensor (accelerometer + magnetometer fused).
 * Fully offline — no network, no Play Services. Reports [HeadingAccuracy] honestly so guidance can
 * warn when the compass is unstable instead of giving a confident-but-wrong direction.
 *
 * This is heading-only and carries no location (unlike the older `SensorNavFixProvider`, which pairs
 * the compass with a fixed demo coordinate for the saved-place navigation feature). GPS Orientation
 * Lite pairs this real compass with the real [LocationProvider] GPS fix.
 */
class CompassProvider(context: Context) : HeadingSource, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    @Volatile override var headingDegrees: Double = 0.0; private set
    @Volatile override var accuracy: HeadingAccuracy = HeadingAccuracy.UNRELIABLE; private set

    /** True when the device actually exposes a rotation-vector sensor. */
    val available: Boolean get() = rotationSensor != null

    override fun start() {
        rotationSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val r = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(r, event.values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(r, orientation)
        val azimuth = Math.toDegrees(orientation[0].toDouble())
        headingDegrees = (azimuth + 360.0) % 360.0
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        this.accuracy = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> HeadingAccuracy.HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> HeadingAccuracy.MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> HeadingAccuracy.LOW
            else -> HeadingAccuracy.UNRELIABLE
        }
    }
}
