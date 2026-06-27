package com.observa.app.runtime

import com.observa.app.hazard.Detection
import com.observa.app.hazard.Direction
import com.observa.app.hazard.FrameInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM-side coverage for the model-loading logic that does NOT require an Android AssetManager or a
 * bundled .pte: fallback status, status strings, strict parsing, honest QNN labels, and crash-free
 * behavior when no model is loaded.
 */
class ModelLoadingTest {

    // --- Fallback: an uninitialized detector behaves like "no model" without crashing ---

    @Test fun uninitializedDetectorReportsUnavailable() {
        val det = ExecuTorchDetector()
        assertEquals(InferenceStatus.UNAVAILABLE, det.status)
    }

    @Test fun analyzeFrameWithoutModelReturnsEmptyAndDoesNotCrash() = runBlocking {
        val det = ExecuTorchDetector()
        val frame = FrameInput(640, 480, 100f, 100f, 100f, 100f) // no rgb payload
        assertTrue(det.analyzeFrame(frame).isEmpty())
        // even if pixels are present, no module → still empty, no crash
        val withPixels = frame.copy(rgb = FloatArray(256 * 256 * 3), rgbWidth = 256, rgbHeight = 256)
        assertTrue(det.analyzeFrame(withPixels).isEmpty())
    }

    @Test fun statusStringsAreExposedForTheUi() {
        assertEquals("unavailable — heuristic fallback", InferenceStatus.UNAVAILABLE.label)
        assertEquals("loaded — CPU", InferenceStatus.LOADED_CPU.label)
        assertEquals("loaded — QNN active", InferenceStatus.LOADED_QNN.label)
        assertEquals("failed — heuristic fallback", InferenceStatus.FAILED.label)
    }

    // --- Strict parser: never fabricates labels for an unverified output shape ---

    @Test fun strictParserRefusesUnknownShapes() {
        val parser = StrictUnknownShapeParser()
        val yoloLike = TensorOutput(longArrayOf(1, 25200, 85), FloatArray(25200 * 85))
        assertTrue(parser.parse(listOf(yoloLike), nowMs = 0L).isEmpty())
    }

    @Test fun strictParserHandlesEmptyOutput() {
        assertTrue(StrictUnknownShapeParser().parse(emptyList(), nowMs = 0L).isEmpty())
    }

    // --- QNN honesty: detecting the library must never read as active acceleration ---

    @Test fun qnnDelegatePresentDoesNotClaimActive() {
        assertEquals("QNN delegate present (not active)", QnnStatus.DELEGATE_PRESENT.label)
        assertFalse(
            "library presence must not be phrased as active acceleration",
            QnnStatus.DELEGATE_PRESENT.label.contains("QNN active"),
        )
        // The only status that says "QNN active" is the inference status, set solely from the
        // loaded model's forward() backends — never from library detection.
        assertTrue(InferenceStatus.LOADED_QNN.label.contains("QNN active"))
    }

    // --- Heuristic fallback never emits semantic object labels ---

    @Test fun heuristicFallbackOnlyEmitsGenericObstacle() = runBlocking {
        val runtime = HeuristicVisionRuntime()
        val blocked = FrameInput(640, 480, leftLuma = 20f, centerLuma = 120f, rightLuma = 120f, avgLuma = 86.7f)
        val dets: List<Detection> = runtime.analyzeFrame(blocked)
        assertTrue(dets.isNotEmpty())
        assertTrue(dets.all { it.label == "OBSTACLE" })
        assertEquals(Direction.LEFT, dets.first().direction)
    }
}
