package com.observa.app.hazard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the deterministic detection-to-alert mapping and anti-spam gating. */
class HazardEngineTest {

    @Test fun obstacleMapsToHighSeverityAhead() {
        val engine = HazardEngine()
        val out = engine.process(listOf(Detection("OBSTACLE", 0.9f, Direction.CENTER)), nowMs = 1_000L)
        assertEquals(1, out.size)
        assertEquals(HazardType.OBSTACLE_AHEAD, out[0].type)
        assertEquals(Severity.HIGH, out[0].severity)
    }

    @Test fun severityMappingIsDeterministic() {
        fun severityOf(label: String): Severity =
            HazardEngine().process(listOf(Detection(label, 1f, Direction.CENTER)), 0L).first().severity

        assertEquals(Severity.HIGH, severityOf("OBSTACLE"))
        assertEquals(Severity.HIGH, severityOf("STAIRS"))
        assertEquals(Severity.MEDIUM, severityOf("PERSON"))
        assertEquals(Severity.LOW, severityOf("DOORWAY"))
        assertEquals(Severity.LOW, severityOf("TEXT"))
    }

    /** The engine must never invent a label it was not given. */
    @Test fun unknownLabelsProduceNoFakeHazards() {
        val engine = HazardEngine()
        val out = engine.process(listOf(Detection("UNICORN", 1f, Direction.CENTER)), 0L)
        assertTrue(out.isEmpty())
    }

    @Test fun repeatedHazardWithinCooldownIsSuppressed() {
        val engine = HazardEngine()
        val first = engine.process(listOf(Detection("OBSTACLE", 1f, Direction.CENTER)), 1_000L)
        assertEquals(1, first.size)
        val second = engine.process(listOf(Detection("OBSTACLE", 1f, Direction.CENTER)), 1_200L)
        assertTrue("same hazard right away must be suppressed", second.isEmpty())
    }

    @Test fun pathClearAnnouncedOnceAfterHazardDisappears() {
        val engine = HazardEngine()
        engine.process(listOf(Detection("OBSTACLE", 1f, Direction.CENTER)), 10_000L)
        val cleared = engine.process(emptyList(), 17_000L)
        assertEquals(1, cleared.size)
        assertEquals(HazardType.CLEAR_PATH, cleared[0].type)
    }

    @Test fun emptyDetectionsWithNoPriorHazardSayNothing() {
        val engine = HazardEngine()
        assertTrue(engine.process(emptyList(), 0L).isEmpty())
    }
}
