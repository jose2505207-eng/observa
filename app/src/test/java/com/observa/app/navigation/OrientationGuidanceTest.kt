package com.observa.app.navigation

import com.observa.app.nav.GeoPoint
import com.observa.app.nav.GpsAccuracy
import com.observa.app.nav.HeadingAccuracy
import com.observa.app.nav.SavedDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrientationGuidanceTest {

    private val here = GeoPoint(37.5665, 126.9780)
    private val north = SavedDestination("north", GeoPoint(37.5710, 126.9780))
    private val engine = OrientationGuidanceEngine()

    @Test
    fun bearing_relativeDirections_sixWay() {
        // Destination due north (bearing ~0). Facing the listed heading → expected direction.
        assertEquals(RelativeDirection.AHEAD, BearingCalculator.relativeDirection(0.0, 0.0))
        assertEquals(RelativeDirection.SLIGHT_RIGHT, BearingCalculator.relativeDirection(30.0, 0.0))
        assertEquals(RelativeDirection.SLIGHT_LEFT, BearingCalculator.relativeDirection(330.0, 0.0))
        assertEquals(RelativeDirection.RIGHT, BearingCalculator.relativeDirection(90.0, 0.0))
        assertEquals(RelativeDirection.LEFT, BearingCalculator.relativeDirection(270.0, 0.0))
        assertEquals(RelativeDirection.BEHIND, BearingCalculator.relativeDirection(180.0, 0.0))
    }

    @Test
    fun guide_aheadWithDistance() {
        val g = engine.guide(here, headingDeg = 0.0, headingAccuracy = HeadingAccuracy.HIGH, gps = GpsAccuracy.GOOD, dest = north)
        assertTrue(g.speech.startsWith("Destination ahead"))
        assertTrue(g.speech.contains("meters"))
        assertEquals(OrientationConfidence.GOOD, g.confidence)
        assertFalse(g.arrived)
    }

    @Test
    fun guide_offAxis_addsTurnHint() {
        val g = engine.guide(here, headingDeg = 90.0, headingAccuracy = HeadingAccuracy.HIGH, gps = GpsAccuracy.GOOD, dest = north)
        // Destination north while facing east → turn left.
        assertTrue(g.speech.contains("Turn left"))
    }

    @Test
    fun guide_noFix_isWeakGps_neverFakes() {
        val g = engine.guide(null, 0.0, HeadingAccuracy.HIGH, GpsAccuracy.NONE, north)
        assertEquals(OrientationConfidence.WEAK_GPS, g.confidence)
        assertTrue(g.speech.contains("GPS signal weak"))
    }

    @Test
    fun guide_unstableCompass_isHonest() {
        val g = engine.guide(here, 0.0, HeadingAccuracy.UNRELIABLE, GpsAccuracy.GOOD, north)
        assertEquals(OrientationConfidence.COMPASS_UNSTABLE, g.confidence)
        assertTrue(g.speech.contains("Compass unstable"))
    }

    @Test
    fun guide_arrival_whenClose() {
        val g = engine.guide(north.point, 0.0, HeadingAccuracy.HIGH, GpsAccuracy.GOOD, north)
        assertTrue(g.arrived)
        assertTrue(g.speech.contains("arrived"))
    }

    @Test
    fun safetyArbiter_blocksDuringHazardWindow() {
        val arb = NavigationSafetyArbiter(hazardHoldMs = 2_000L)
        assertFalse(arb.guidanceAllowed(nowMs = 1_500L, lastHazardMs = 1_000L)) // within window
        assertTrue(arb.guidanceAllowed(nowMs = 4_000L, lastHazardMs = 1_000L))  // after window
        assertTrue(arb.guidanceAllowed(nowMs = 4_000L, lastHazardMs = 0L))      // no hazard yet
    }
}
