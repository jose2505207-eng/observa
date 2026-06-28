package com.observa.app.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the CPU-side decode of the raw YOLOv8 head (the path used after QNN/HTP inference).
 * Uses a tiny synthetic 2×2 grid with hand-computable DFL distributions so the expected box is exact.
 */
class YoloRawHeadParserTest {

    private val numClasses = 80
    private val regMax = 16
    private val boxCh = 4 * regMax            // 64
    private val channels = boxCh + numClasses // 144

    /** Build a [1,144,H,W] tensor with all class logits very negative (no spurious detections). */
    private fun blankGrid(h: Int, w: Int): FloatArray {
        val hw = h * w
        val data = FloatArray(channels * hw)
        for (c in boxCh until channels) for (cell in 0 until hw) data[c * hw + cell] = -10f
        return data
    }

    private fun set(data: FloatArray, hw: Int, channel: Int, cell: Int, v: Float) {
        data[channel * hw + cell] = v
    }

    @Test
    fun decodesSinglePerson_withExactBoxFromDfl() {
        val h = 2; val w = 2; val hw = h * w
        val data = blankGrid(h, w)
        val cell = 1 * w + 1 // (y=1, x=1) → index 3

        // Class 0 (person) wins strongly at this cell.
        set(data, hw, boxCh + 0, cell, 20f)
        // DFL: left bin1, top bin1, right bin0, bottom bin0 (≈ distances 1,1,0,0 in cell units).
        set(data, hw, 0 * regMax + 1, cell, 20f)   // left  → ~1.0
        set(data, hw, 1 * regMax + 1, cell, 20f)   // top   → ~1.0
        set(data, hw, 2 * regMax + 0, cell, 20f)   // right → ~0.0
        set(data, hw, 3 * regMax + 0, cell, 20f)   // bottom→ ~0.0

        val out = YoloRawHeadParser().parse(listOf(TensorOutput(longArrayOf(1, 144, 2, 2), data)), 0L)

        assertEquals(1, out.size)
        val d = out[0]
        assertEquals("PERSON", d.label)
        assertEquals("person", d.rawClass)
        assertTrue("confidence ~1.0 but was ${d.confidence}", d.confidence > 0.99f)
        // ax=1.5, ay=1.5; left=(1.5-1)/2=.25, top=.25, right=(1.5+0)/2=.75, bottom=.75
        assertEquals(0.25f, d.box.left, 0.01f)
        assertEquals(0.25f, d.box.top, 0.01f)
        assertEquals(0.75f, d.box.right, 0.01f)
        assertEquals(0.75f, d.box.bottom, 0.01f)
        assertEquals(com.observa.app.hazard.Direction.CENTER, d.direction)
        assertTrue(d.hazardRelevant)
    }

    @Test
    fun belowThreshold_yieldsNoDetections() {
        val h = 2; val w = 2; val hw = h * w
        val data = blankGrid(h, w)
        // Person logit = 0 → sigmoid 0.5? No: keep it just under the 0.40 conf threshold.
        // sigmoid(-0.5)=0.378 < 0.40 → must be dropped.
        set(data, hw, boxCh + 0, 0, -0.5f)
        val out = YoloRawHeadParser().parse(listOf(TensorOutput(longArrayOf(1, 144, 2, 2), data)), 0L)
        assertTrue(out.isEmpty())
    }

    @Test
    fun nonPersonNonVehicleClass_isDropped() {
        // class 76 = "scissors" → not a safety category → must not be surfaced.
        val h = 2; val w = 2; val hw = h * w
        val data = blankGrid(h, w)
        set(data, hw, boxCh + 76, 0, 20f)
        val out = YoloRawHeadParser().parse(listOf(TensorOutput(longArrayOf(1, 144, 2, 2), data)), 0L)
        assertTrue(out.isEmpty())
    }

    @Test
    fun wrongShape_returnsEmpty_neverFabricates() {
        val bogus = TensorOutput(longArrayOf(1, 84, 2100), FloatArray(84 * 2100))
        assertTrue(YoloRawHeadParser().parse(listOf(bogus), 0L).isEmpty())
        assertTrue(YoloRawHeadParser().parse(emptyList(), 0L).isEmpty())
    }

    @Test
    fun multiScale_dedupesOverlappingBoxesViaNms() {
        // Same person in two scales at overlapping locations → NMS collapses to one.
        fun grid(h: Int, w: Int): FloatArray {
            val hw = h * w
            val d = blankGrid(h, w)
            val cell = 0
            set(d, hw, boxCh + 0, cell, 20f)
            set(d, hw, 0 * regMax + 2, cell, 20f) // left ~2
            set(d, hw, 1 * regMax + 2, cell, 20f) // top ~2
            set(d, hw, 2 * regMax + 2, cell, 20f) // right ~2
            set(d, hw, 3 * regMax + 2, cell, 20f) // bottom ~2
            return d
        }
        val out = YoloRawHeadParser().parse(
            listOf(
                TensorOutput(longArrayOf(1, 144, 4, 4), grid(4, 4)),
                TensorOutput(longArrayOf(1, 144, 4, 4), grid(4, 4)),
            ),
            0L,
        )
        assertEquals(1, out.size)
    }
}
