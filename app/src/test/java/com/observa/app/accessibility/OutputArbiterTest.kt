package com.observa.app.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

/** Priority ordering: HAZARD > NAVIGATION > OCR > MODE > INFO. */
class OutputArbiterTest {

    private fun ev(p: OutputPriority, s: String) = OutputEvent(priority = p, speech = s)

    @Test fun hazardIsHighestPriority() {
        val events = listOf(
            ev(OutputPriority.INFO, "info"),
            ev(OutputPriority.OCR, "ocr"),
            ev(OutputPriority.HAZARD, "hazard"),
            ev(OutputPriority.MODE, "mode"),
            ev(OutputPriority.NAVIGATION, "nav"),
        )
        assertEquals("hazard", OutputArbiter.highest(events)?.speech)
    }

    @Test fun fullOrderingIsHazardNavOcrModeInfo() {
        val events = listOf(
            ev(OutputPriority.MODE, "mode"),
            ev(OutputPriority.HAZARD, "hazard"),
            ev(OutputPriority.INFO, "info"),
            ev(OutputPriority.NAVIGATION, "nav"),
            ev(OutputPriority.OCR, "ocr"),
        )
        assertEquals(
            listOf("hazard", "nav", "ocr", "mode", "info"),
            OutputArbiter.sortByPriority(events).map { it.speech },
        )
    }

    @Test fun hazardOutranksOcrSoItInterrupts() {
        val ocr = ev(OutputPriority.OCR, "long ocr text")
        val hazard = ev(OutputPriority.HAZARD, "Obstacle ahead")
        assertEquals("Obstacle ahead", OutputArbiter.highest(listOf(ocr, hazard))?.speech)
    }
}
