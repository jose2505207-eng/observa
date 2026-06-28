package com.observa.app.navigation

import com.observa.app.nav.GeoPoint
import com.observa.app.nav.GpsAccuracy
import com.observa.app.nav.HeadingAccuracy
import com.observa.app.nav.SavedDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
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

    private class FakeCompass(
        override var headingDegrees: Double = 0.0,
        override var accuracy: HeadingAccuracy = HeadingAccuracy.HIGH,
    ) : HeadingSource {
        var started = false
        override fun start() { started = true }
        override fun stop() { started = false }
    }

    // Destination ~ north of the start point.
    private val dest = SavedDestination("test", GeoPoint(37.5710, 126.9780))
    private val start = GeoPoint(37.5665, 126.9780)

    private fun controller(loc: FakeLocation, compass: FakeCompass = FakeCompass()) =
        OrientationController(
            location = loc,
            compass = compass,
            destinations = DestinationStore(dest),
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
        val compass = FakeCompass(headingDegrees = 0.0) // facing north
        val c = controller(loc, compass)
        c.start()
        assertTrue(loc.started)
        assertTrue(compass.started)
        val g = c.tick(1_000L)!!
        // North-facing user, destination due north → ahead, with a distance in meters.
        assertTrue(g.speech.contains("meters"))
        assertTrue(g.speech.contains("ahead"))
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
            compass = FakeCompass(),
            destinations = DestinationStore(dest),
            minUpdateMs = 5_000L,
        )
        c.start()
        assertNotNull(c.tick(10_000L))   // first tick emits
        assertNull(c.tick(11_000L))      // within window → suppressed (no braille flooding)
        assertNotNull(c.tick(16_000L))   // after window → emits again
    }

    @Test
    fun hazard_suppressesGuidance_safetyFirst() {
        val c = controller(FakeLocation(start))
        c.start()
        // A hazard fired 100 ms ago → orientation must stay quiet (vision hazard beats navigation).
        assertNull(c.tick(nowMs = 10_000L, lastHazardMs = 9_900L))
        // Well after the hazard hold window → guidance resumes.
        assertNotNull(c.tick(nowMs = 20_000L, lastHazardMs = 9_900L))
    }

    @Test
    fun noGpsFix_reportsWeakGps_neverFakes() {
        val c = controller(FakeLocation(point = null, accuracy = GpsAccuracy.NONE))
        c.start()
        val g = c.tick(1_000L)!!
        assertTrue(g.speech.contains("GPS signal weak"))
        assertEquals(OrientationConfidence.WEAK_GPS, g.confidence)
    }
}
