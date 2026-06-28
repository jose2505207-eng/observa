package com.observa.app.nav

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A WGS84 coordinate. */
data class GeoPoint(val lat: Double, val lon: Double)

/** Pure great-circle geometry. No Android deps; unit-testable. */
object Geo {
    private const val EARTH_R = 6_371_000.0 // meters

    /** Haversine distance in meters between two points. */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val la1 = Math.toRadians(a.lat)
        val la2 = Math.toRadians(b.lat)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(la1) * cos(la2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_R * atan2(sqrt(h), sqrt(1 - h))
    }

    /** Initial bearing from [a] to [b] in degrees, normalized to [0,360). */
    fun bearingDegrees(a: GeoPoint, b: GeoPoint): Double {
        val la1 = Math.toRadians(a.lat)
        val la2 = Math.toRadians(b.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val y = sin(dLon) * cos(la2)
        val x = cos(la1) * sin(la2) - sin(la1) * cos(la2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }
}
