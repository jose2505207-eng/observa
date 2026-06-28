package com.observa.app.nav

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Heading from the device compass (rotation-vector sensor) — works fully offline. Location is a
 * **documented demo fix** ([[offline-navigation]]): this build has no live GPS provider (and no
 * `ACCESS_FINE_LOCATION`), so navigation demonstrates real, heading-relative clock-face guidance
 * from a fixed start point. [sourceLabel] states this honestly; the controller announces it.
 */
class SensorNavFixProvider(
    context: Context,
    private val demoStart: GeoPoint = DestinationStore.DEMO.first().point,
) : NavFixProvider, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    @Volatile private var headingDeg = 0.0
    @Volatile private var headingAccuracy = HeadingAccuracy.UNRELIABLE

    override val sourceLabel = "demo location, live compass"

    fun start() {
        rotationSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun current(): NavFix = NavFix(
        point = demoStart,
        headingDeg = headingDeg,
        headingAccuracy = headingAccuracy,
        // Honest: this is a fixed demo location, not a live GPS fix. Marked LOW so guidance always
        // appends the caution note until a real GPS provider is wired.
        gps = GpsAccuracy.LOW,
    )

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val r = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(r, event.values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(r, orientation)
        val azimuth = Math.toDegrees(orientation[0].toDouble())
        headingDeg = (azimuth + 360.0) % 360.0
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        headingAccuracy = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> HeadingAccuracy.HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> HeadingAccuracy.MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> HeadingAccuracy.LOW
            else -> HeadingAccuracy.UNRELIABLE
        }
    }
}
