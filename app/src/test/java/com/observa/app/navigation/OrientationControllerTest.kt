package com.observa.app.navigation

import com.observa.app.nav.GeoPoint
import com.observa.app.nav.GpsAccuracy
import com.observa.app.nav.HeadingAccuracy
import com.observa.app.nav.SavedDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrientationControllerTest {

    private class FakeLocation(
        var point: GeoPoint?,
        override var accuracy: GpsAccuracy = GpsAccuracy.GOOD,
        override var hasPermission: Boolean = true,
    ) : LocationSource {
        var started = false
        override fun start() { started = true }
        override fun stop() { started = false }
        override fun current(): GeoPoint? = point
    }

    // Destination ~ north of the start point.
    private val dest = SavedDestination("test", GeoPoint(37.5710, 126.9780))
    private val start = GeoPoint(37.5665, 126.9780)

    private fun controller(loc: FakeLocation) = OrientationController(
        location = loc,
        heading = { 0.0 to HeadingAccuracy.HIGH }, // facing north
        destination = dest,
        minUpdateMs = 0L,
    )

    @Test
    fun offByDefault_repeatSaysOff() {
        val c = controller(FakeLocation(start))
        assertFalse(c.active)
        assertEquals("Orientation off", c.statusLine())
        assertTrue(c.repeat().speech.contains("off", ignoreCase = true))
    }

    @Test
    fun start_withGps_guidesTowardDestination() {
        val loc = FakeLocation(start, accuracy = GpsAccuracy.GOOD)
        val c = controller(loc)
        c.start()
        assertTrue(loc.started)
        val g = c.tick(1_000L)!!
        // North-facing user, destination due north → straight ahead, with a distance in meters.
        assertTrue(g.speech.contains("meters"))
        assertTrue(c.statusLine().startsWith("Orientation active"))
    }

    @Test
    fun start_withoutPermission_isHonest() {
        val loc = FakeLocation(start, hasPermission = false)
        val g = controller(loc).start()
        assertTrue(g.speech.contains("permission"))
    }

    @Test
    fun rateLimits_updates() {
        val c = OrientationController(
            location = FakeLocation(start),
            heading = { 0.0 to HeadingAccuracy.HIGH },
            destination = dest,
            minUpdateMs = 5_000L,
        )
        c.start()
        assertTrue(c.tick(10_000L) != null)   // first tick emits
        assertTrue(c.tick(11_000L) == null)   // within window → suppressed (no braille flooding)
        assertTrue(c.tick(16_000L) != null)   // after window → emits again
    }

    @Test
    fun noGpsFix_reportsUnavailable_neverFakes() {
        val c = controller(FakeLocation(point = null, accuracy = GpsAccuracy.NONE))
        c.start()
        val g = c.tick(1_000L)!!
        assertTrue(g.speech.contains("GPS unavailable"))
    }
}
