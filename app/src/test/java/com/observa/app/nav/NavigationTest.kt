package com.observa.app.nav

import com.observa.app.voice.CommandIntent
import com.observa.app.voice.VoiceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Offline navigation: geometry, clock-face guidance, accuracy honesty, sessions, parsing. */
class NavigationTest {

    private val seoul = GeoPoint(37.5665, 126.9780)

    // --- geometry ---

    @Test fun bearingNorthIsZero() {
        val north = GeoPoint(seoul.lat + 0.01, seoul.lon)
        assertEquals(0.0, Geo.bearingDegrees(seoul, north), 1.0)
    }

    @Test fun bearingEastIsNinety() {
        val east = GeoPoint(seoul.lat, seoul.lon + 0.01)
        assertEquals(90.0, Geo.bearingDegrees(seoul, east), 1.0)
    }

    @Test fun distanceIsReasonable() {
        val east = GeoPoint(seoul.lat, seoul.lon + 0.01)
        val d = Geo.distanceMeters(seoul, east)
        assertTrue("~880m at this latitude", d in 800.0..950.0)
    }

    // --- clock-face translation (RelativeDirectionTranslator) ---

    @Test fun bearingNormalizationWrapsAt360() {
        assertEquals(0.0, RelativeDirectionTranslator.normalize(360.0), 0.001)
        assertEquals(10.0, RelativeDirectionTranslator.normalize(370.0), 0.001)
        assertEquals(-10.0, RelativeDirectionTranslator.normalize(350.0), 0.001)
    }

    @Test fun straightAheadAndSlightRight() {
        assertEquals(12, RelativeDirectionTranslator.translate(0.0, 0.0).clock)
        assertEquals(1, RelativeDirectionTranslator.translate(30.0, 0.0).clock)
        assertEquals(3, RelativeDirectionTranslator.translate(90.0, 0.0).clock)
    }

    // --- guidance formatting + accuracy honesty ---

    private fun fix(point: GeoPoint?, heading: Double = 0.0, ha: HeadingAccuracy = HeadingAccuracy.HIGH, gps: GpsAccuracy = GpsAccuracy.GOOD) =
        NavFix(point, heading, ha, gps)

    private val dest = SavedDestination("the park", GeoPoint(seoul.lat + 0.005, seoul.lon))

    @Test fun noFixIsHonest() {
        val g = GuidanceEngine().guide(fix(point = null, gps = GpsAccuracy.NONE), dest)
        assertTrue(g.speech.contains("GPS unavailable"))
        assertFalse(g.arrived)
    }

    @Test fun arrivalWhenClose() {
        val here = SavedDestination("here", seoul.copy(lat = seoul.lat + 0.00005))
        val g = GuidanceEngine().guide(fix(seoul), here)
        assertTrue(g.arrived)
        assertTrue(g.speech.contains("Destination ahead"))
    }

    @Test fun straightGuidanceSaysContinueStraight() {
        // destination due north, facing north → 12 o'clock
        val g = GuidanceEngine().guide(fix(seoul, heading = 0.0), dest)
        assertTrue(g.speech.startsWith("Continue straight"))
        assertTrue(g.speech.contains("meters"))
    }

    @Test fun lowGpsAndCompassWarn() {
        val g = GuidanceEngine().guide(fix(seoul, ha = HeadingAccuracy.LOW, gps = GpsAccuracy.LOW), dest)
        assertTrue(g.speech.contains("GPS accuracy low"))
        assertTrue(g.speech.contains("Compass accuracy low"))
    }

    // --- destination store ---

    @Test fun savedDestinationLookup() {
        val store = DestinationStore()
        assertEquals("the park", store.find("park")?.name)
        assertEquals("home", store.find("HOME")?.name)
        assertNull(store.find("nonexistent place"))
    }

    @Test fun addOverridesByName() {
        val store = DestinationStore(emptyList())
        store.add(SavedDestination("office", GeoPoint(1.0, 2.0)))
        store.add(SavedDestination("Office", GeoPoint(3.0, 4.0)))
        assertEquals(1, store.all().size)
        assertEquals(3.0, store.find("office")!!.point.lat, 0.001)
    }

    // --- session throttling + arrival ---

    @Test fun sessionSpeaksThenStaysQuietThenArrives() {
        val session = NavigationSession(minRepeatMs = 1_000L)
        session.start(dest)
        assertTrue(session.active)
        val first = session.tick(fix(seoul), nowMs = 0L)
        assertTrue("first tick speaks", first != null)
        val quiet = session.tick(fix(seoul), nowMs = 100L)
        assertNull("same direction within cooldown is quiet", quiet)
        // jump to arrival
        val here = seoul
        val arrived = session.tick(fix(here).copy(point = dest.point), nowMs = 5_000L)
        assertTrue(arrived?.arrived == true)
        assertFalse("session ends on arrival", session.active)
    }

    @Test fun noRouteFallbackWhenStopped() {
        val session = NavigationSession()
        assertNull(session.tick(fix(seoul), 0L))
    }

    // --- voice parsing ---

    @Test fun navigateAndStopNavigationParse() {
        val p = VoiceCommandParser()
        val nav = p.parse("navigate to the park").intent
        assertTrue(nav is CommandIntent.NavigateTo)
        assertEquals("the park", (nav as CommandIntent.NavigateTo).destination)
        assertTrue(p.parse("stop navigation").intent is CommandIntent.StopNavigation)
        // bare "stop" must remain Stop (observing), not StopNavigation
        assertTrue(p.parse("stop").intent is CommandIntent.Stop)
        assertTrue(p.parse("where am i").intent is CommandIntent.WhereAmI)
    }
}
