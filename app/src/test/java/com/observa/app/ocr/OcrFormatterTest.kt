package com.observa.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** OCR result cleaning and the honest "no text" message. */
class OcrFormatterTest {

    @Test fun collapsesWhitespaceAndNewlines() {
        val r = OcrFormatter.fromRaw("EXIT\n  this   way\n")
        assertTrue(r.found)
        assertEquals("EXIT this way", r.text)
        assertEquals("EXIT this way", r.message)
    }

    @Test fun emptyRawIsNotFound() {
        val r = OcrFormatter.fromRaw("   \n  ")
        assertFalse(r.found)
        assertEquals("No readable text found.", r.message)
    }

    @Test fun blankModelOutputIsHonest() {
        assertEquals("No readable text found.", OcrFormatter.fromRaw("").message)
    }
}
