package com.observa.app.navigation

import com.observa.app.nav.GeoPoint
import com.observa.app.nav.GpsAccuracy
import com.observa.app.nav.HeadingAccuracy
import com.observa.app.nav.SavedDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationModulesTest {

    private val here = GeoPoint(37.5665, 126.9780)
    private val north = SavedDestination("north", GeoPoint(37.5710, 126.9780))

    // --- RouteGuidanceEngine: honest bearing guidance, never claims a route it doesn't have ---
    @Test
    fun routeGuidance_fallsBackToBearing_whenNoRoute() {
        val eng = RouteGuidanceEngine()
        val g = eng.guide(here, 0.0, HeadingAccuracy.HIGH, GpsAccuracy.GOOD, north, routeAvailable = false)
        assertTrue(g.speech.contains("Destination"))
        assertTrue(g.speech.contains("meters"))
        assertFalse(eng.usingRoute(routeAvailable = false))
        assertTrue(eng.usingRoute(routeAvailable = true))
    }

    // --- NavigationSafetyArbiter: a vision hazard suppresses navigation guidance ---
    @Test
    fun safetyArbiter_hazardBeatsNavigation() {
        val arb = NavigationSafetyArbiter(hazardHoldMs = 2_000L)
        assertFalse(arb.guidanceAllowed(nowMs = 1_500L, lastHazardMs = 1_000L)) // within hazard window
        assertTrue(arb.guidanceAllowed(nowMs = 4_000L, lastHazardMs = 1_000L))  // after window
    }

    // --- StreetSignTracker: stability gate before OCR; fires once per stable streak ---
    @Test
    fun streetSign_requiresStableFrames_thenFiresOnce() {
        val t = StreetSignTracker(stableFrames = 3, cooldownMs = 1_000L)
        assertFalse(t.onFrame(candidate = true, nowMs = 0))   // 1
        assertFalse(t.onFrame(candidate = true, nowMs = 10))  // 2
        assertTrue(t.onFrame(candidate = true, nowMs = 20))   // 3 → fire
        assertFalse(t.onFrame(candidate = true, nowMs = 30))  // already fired this streak
        assertFalse(t.onFrame(candidate = false, nowMs = 40)) // streak broken
    }

    @Test
    fun streetSign_noCandidate_neverFires() {
        val t = StreetSignTracker(stableFrames = 2)
        repeat(10) { assertFalse(t.onFrame(candidate = false, nowMs = it.toLong())) }
    }
}
