package com.observa.app.hazard

import com.observa.app.cue.HazardPriorityResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Real detections (as the ExecuTorch path emits: label/confidence/direction) route through the
 * existing HazardEngine into hazards, and the priority resolver picks the most urgent.
 */
class DetectionToHazardTest {

    @Test fun personAndObstacleDetectionsBecomeHazards() {
        val engine = HazardEngine()
        val detections = listOf(
            Detection(label = "PERSON", confidence = 0.9f, direction = Direction.LEFT),
            Detection(label = "OBSTACLE", confidence = 0.8f, direction = Direction.CENTER),
        )
        val hazards = engine.process(detections, nowMs = 1_000L)
        val types = hazards.map { it.type }.toSet()
        assertTrue(types.contains(HazardType.PERSON_APPROACHING))
        assertTrue(types.contains(HazardType.OBSTACLE_AHEAD))
    }

    @Test fun resolverPicksHighestSeverity() {
        val engine = HazardEngine()
        val hazards = engine.process(
            listOf(
                Detection("PERSON", 0.9f, Direction.LEFT),     // MEDIUM
                Detection("OBSTACLE", 0.8f, Direction.CENTER),  // HIGH
            ),
            nowMs = 1_000L,
        )
        val top = HazardPriorityResolver().resolve(hazards)
        assertEquals(Severity.HIGH, top?.severity)
        assertEquals(HazardType.OBSTACLE_AHEAD, top?.type)
    }
}
