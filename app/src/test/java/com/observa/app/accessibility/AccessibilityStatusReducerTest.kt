package com.observa.app.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityStatusReducerTest {

    private fun state(
        awareness: Boolean = true,
        muted: Boolean = false,
        detector: DetectorBackend = DetectorBackend.XNNPACK,
        ocr: Boolean = true,
        translation: Boolean = false,
        navigating: Boolean = false,
        lastAlert: String? = null,
        orientation: String? = null,
    ) = A11yState(awareness, muted, detector, ocr, translation, navigating, lastAlert, orientation)

    @Test
    fun currentStatus_appendsOrientation_whenActive() {
        val line = AccessibilityStatusReducer.currentStatus(
            state(orientation = "Orientation active. 1 o'clock · 40m"),
        )
        assertTrue(line.contains("Orientation active"))
        assertTrue(line.contains("40m"))
    }

    @Test
    fun availableActions_offersNavigate_thenStopWhenActive() {
        assertTrue(AccessibilityStatusReducer.availableActions(state(orientation = null)).contains("navigate"))
        assertTrue(AccessibilityStatusReducer.availableActions(state(orientation = "Orientation active. x")).contains("stop navigation"))
    }

    @Test
    fun awarenessState_reflectsToggle() {
        assertEquals("Awareness active", AccessibilityStatusReducer.awarenessState(state(awareness = true)))
        assertEquals("Awareness paused", AccessibilityStatusReducer.awarenessState(state(awareness = false)))
    }

    @Test
    fun detectorState_namesBackendShort() {
        assertEquals("Detector backend: XNNPACK", AccessibilityStatusReducer.detectorState(state(detector = DetectorBackend.XNNPACK)))
        assertEquals("Detector backend: QNN/NPU", AccessibilityStatusReducer.detectorState(state(detector = DetectorBackend.QNN)))
        assertEquals("Detector backend: heuristic fallback", AccessibilityStatusReducer.detectorState(state(detector = DetectorBackend.HEURISTIC)))
    }

    @Test
    fun ocrAndTranslation_areHonest() {
        assertEquals("OCR ready", AccessibilityStatusReducer.ocrState(state(ocr = true)))
        assertEquals("OCR unavailable", AccessibilityStatusReducer.ocrState(state(ocr = false)))
        assertEquals("Translation not installed", AccessibilityStatusReducer.translationState(state(translation = false)))
    }

    @Test
    fun currentStatus_isConciseAndCoversSafetyState() {
        val s = state(awareness = true, detector = DetectorBackend.XNNPACK, ocr = true)
        val line = AccessibilityStatusReducer.currentStatus(s)
        assertEquals("Awareness active. Detector backend: XNNPACK. OCR ready.", line)
    }

    @Test
    fun currentStatus_includesMuteAndNav_whenSet() {
        val line = AccessibilityStatusReducer.currentStatus(state(muted = true, navigating = true))
        assertTrue(line.contains("Speech muted"))
        assertTrue(line.contains("Navigating"))
    }

    @Test
    fun lastAlert_placeholderWhenNothingFired() {
        assertEquals("Last alert: none", AccessibilityStatusReducer.lastAlert(state(lastAlert = null)))
        assertEquals("Last alert: none", AccessibilityStatusReducer.lastAlert(state(lastAlert = "No alerts yet")))
    }

    @Test
    fun lastAlert_formatsShortHazardLine() {
        assertEquals("Last alert: obstacle ahead", AccessibilityStatusReducer.lastAlert(state(lastAlert = "Obstacle ahead")))
    }

    @Test
    fun stripDebug_removesEngineeringDetailFromUserOutput() {
        // The bad example from the spec must never reach user output.
        assertEquals(
            "Obstacle ahead",
            AccessibilityStatusReducer.stripDebug("Obstacle ahead confidence 0.718 at bbox 39, 103, 224, 335"),
        )
        assertEquals("Person approaching on your left", AccessibilityStatusReducer.stripDebug("Person approaching on your left"))
    }

    @Test
    fun lastAlert_neverLeaksDebugNumbers() {
        val node = AccessibilityStatusReducer.lastAlert(state(lastAlert = "Obstacle ahead confidence 0.718 at bbox 39, 103, 224, 335"))
        assertEquals("Last alert: obstacle ahead", node)
        assertFalse(node.any { it.isDigit() })
    }

    @Test
    fun availableActions_listsCoreVerbs_andHonestTranslation() {
        val a = AccessibilityStatusReducer.availableActions(state(awareness = true, translation = false))
        assertTrue(a.contains("pause awareness"))
        assertTrue(a.contains("repeat last alert"))
        assertTrue(a.contains("read signs"))
        assertTrue(a.contains("voice commands"))
        assertTrue(a.contains("translate"))
        assertTrue(a.contains("translation not installed"))
        assertTrue(a.contains("silence alerts"))
        assertTrue(a.contains("debug status"))
        // When paused, the first action offered is to start awareness.
        assertTrue(AccessibilityStatusReducer.availableActions(state(awareness = false)).contains("start awareness"))
    }
}
