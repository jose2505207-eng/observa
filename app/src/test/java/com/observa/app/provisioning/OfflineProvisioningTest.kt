package com.observa.app.provisioning

import com.observa.app.maps.MapPackVerifier
import com.observa.app.translation.TranslationReadiness
import com.observa.app.translation.TranslationReadinessRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OfflineProvisioningTest {

    // --- TranslationReadinessRules: honest, never "ready" without the pack ---
    @Test
    fun translationReadiness_states() {
        assertEquals(TranslationReadiness.DOWNLOADING,
            TranslationReadinessRules.of(downloading = true, failed = false, bothInstalled = false))
        assertEquals(TranslationReadiness.READY_OFFLINE,
            TranslationReadinessRules.of(downloading = false, failed = false, bothInstalled = true))
        assertEquals(TranslationReadiness.FAILED,
            TranslationReadinessRules.of(downloading = false, failed = true, bothInstalled = false))
        assertEquals(TranslationReadiness.LANGUAGE_PACK_MISSING,
            TranslationReadinessRules.of(downloading = false, failed = false, bothInstalled = false))
        // "ready" requires both models installed even if a failure flag lingers.
        assertEquals(TranslationReadiness.READY_OFFLINE,
            TranslationReadinessRules.of(downloading = false, failed = true, bothInstalled = true))
    }

    // --- MapPackVerifier: "ready offline" only for a real, recognized, non-empty file ---
    @Test
    fun mapVerifier_acceptsDemoHeader_rejectsJunk() {
        val good = File.createTempFile("obs_map_good", ".map")
        good.writeText(MapPackVerifier.DEMO_HEADER + "\nwaypoint=37.0,127.0,Start\n")
        assertTrue(MapPackVerifier.isValid(good))

        val empty = File.createTempFile("obs_map_empty", ".map")
        assertFalse(MapPackVerifier.isValid(empty))

        val junk = File.createTempFile("obs_map_junk", ".map")
        junk.writeText("not a map file")
        assertFalse(MapPackVerifier.isValid(junk))

        good.delete(); empty.delete(); junk.delete()
    }
}
