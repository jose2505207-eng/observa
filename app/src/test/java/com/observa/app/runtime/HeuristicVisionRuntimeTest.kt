package com.observa.app.runtime

import com.observa.app.hazard.Direction
import com.observa.app.hazard.FrameInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the brightness heuristic. It is a deterministic proxy (not ML); these tests pin
 * its safe / too-dark / blocked behavior and confirm it never emits a non-OBSTACLE object label.
 */
class HeuristicVisionRuntimeTest {

    private val runtime = HeuristicVisionRuntime()

    @Test fun evenlyLitSceneIsSafe() = runBlocking {
        val frame = FrameInput(640, 480, leftLuma = 120f, centerLuma = 120f, rightLuma = 120f, avgLuma = 120f)
        assertTrue("uniform brightness should yield no obstacle", runtime.analyzeFrame(frame).isEmpty())
    }

    @Test fun tooDarkSceneIsNotJudged() = runBlocking {
        val frame = FrameInput(640, 480, leftLuma = 4f, centerLuma = 4f, rightLuma = 4f, avgLuma = 4f)
        assertTrue("a covered/black frame must not produce detections", runtime.analyzeFrame(frame).isEmpty())
    }

    @Test fun markedlyDarkerRegionReadsAsObstacleInThatDirection() = runBlocking {
        val frame = FrameInput(640, 480, leftLuma = 20f, centerLuma = 120f, rightLuma = 120f, avgLuma = 86.7f)
        val out = runtime.analyzeFrame(frame)
        assertEquals(1, out.size)
        assertEquals("OBSTACLE", out[0].label)
        assertEquals(Direction.LEFT, out[0].direction)
        assertTrue(out[0].confidence > 0f && out[0].confidence <= 1f)
    }

    @Test fun onlyEverEmitsTheGenericObstacleLabel() = runBlocking {
        val frame = FrameInput(640, 480, leftLuma = 120f, centerLuma = 120f, rightLuma = 20f, avgLuma = 86.7f)
        val out = runtime.analyzeFrame(frame)
        // No fabricated semantic classes ("person", "stairs", ...) — only the honest proxy label.
        assertTrue(out.all { it.label == "OBSTACLE" })
    }
}
