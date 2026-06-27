package com.observa.app.runtime

import com.observa.app.hazard.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the real YOLO output parser, class mapping, direction, NMS, and thresholding. */
class YoloDetectionParserTest {

    private val inputSize = 640
    private val numClasses = 80
    private val attrs = numClasses + 4 // YOLOv8: no objectness

    /** Build a channels-first [1, attrs, N] tensor. box coords in input-pixel units. */
    private fun tensor(boxes: List<FloatArray>): TensorOutput {
        val n = boxes.size
        val data = FloatArray(attrs * n)
        boxes.forEachIndexed { i, b -> for (a in 0 until attrs) data[a * n + i] = b[a] }
        return TensorOutput(longArrayOf(1, attrs.toLong(), n.toLong()), data)
    }

    /** cx,cy,w,h (pixels) + class prob set on [cls]. */
    private fun box(cx: Float, cy: Float, w: Float, h: Float, cls: Int, prob: Float): FloatArray {
        val b = FloatArray(attrs)
        b[0] = cx; b[1] = cy; b[2] = w; b[3] = h
        b[4 + cls] = prob
        return b
    }

    private fun parser() = YoloDetectionParser(numClasses = numClasses, inputSize = inputSize)

    @Test fun parsesPersonLeftAndVehicleRight() {
        val t = tensor(
            listOf(
                box(cx = 160f, cy = 320f, w = 128f, h = 256f, cls = 0, prob = 0.9f), // person, left
                box(cx = 512f, cy = 320f, w = 64f, h = 64f, cls = 2, prob = 0.8f),    // car, right
            ),
        )
        val out = parser().parse(listOf(t), nowMs = 123L).sortedBy { it.box.centerX }
        assertEquals(2, out.size)
        assertEquals("PERSON", out[0].label)
        assertEquals(Direction.LEFT, out[0].direction)
        assertEquals("VEHICLE", out[1].label)
        assertEquals("car", out[1].rawClass)
        assertEquals(Direction.RIGHT, out[1].direction)
        assertEquals(123L, out[0].timestampMs)
    }

    @Test fun lowConfidenceIsDropped() {
        val t = tensor(listOf(box(320f, 320f, 64f, 64f, cls = 0, prob = 0.30f)))
        assertTrue(parser().parse(listOf(t), 0L).isEmpty())
    }

    @Test fun boxIsNormalizedToUnitRange() {
        val t = tensor(listOf(box(160f, 320f, 128f, 256f, cls = 0, prob = 0.9f)))
        val d = parser().parse(listOf(t), 0L).single()
        assertEquals(0.15f, d.box.left, 0.001f)
        assertEquals(0.35f, d.box.right, 0.001f)
        assertEquals(0.25f, d.box.centerX, 0.001f)
        assertEquals(0.08f, d.box.area, 0.001f)
    }

    @Test fun nmsRemovesOverlappingSameClassBoxes() {
        val t = tensor(
            listOf(
                box(320f, 320f, 200f, 200f, cls = 0, prob = 0.9f),
                box(330f, 330f, 200f, 200f, cls = 0, prob = 0.6f), // heavy overlap, lower score
            ),
        )
        val out = parser().parse(listOf(t), 0L)
        assertEquals(1, out.size)
        assertEquals(0.9f, out[0].confidence, 0.001f)
    }

    @Test fun unexpectedShapeYieldsEmptyNotCrash() {
        val weird = TensorOutput(longArrayOf(1, 10, 10), FloatArray(100))
        assertTrue(parser().parse(listOf(weird), 0L).isEmpty())
        assertTrue(parser().parse(emptyList(), 0L).isEmpty())
    }

    @Test fun unmappedClassesAreDropped() {
        // class 77 = "teddy bear" → not a safety category → discarded.
        val t = tensor(listOf(box(320f, 320f, 64f, 64f, cls = 77, prob = 0.95f)))
        assertTrue(parser().parse(listOf(t), 0L).isEmpty())
    }

    @Test fun directionMapping() {
        assertEquals(Direction.LEFT, DirectionMapper.fromCenterX(0.2f))
        assertEquals(Direction.CENTER, DirectionMapper.fromCenterX(0.5f))
        assertEquals(Direction.RIGHT, DirectionMapper.fromCenterX(0.8f))
    }

    @Test fun classMappingIsSafe() {
        val m = CocoClassMapper()
        assertEquals("PERSON", m.safetyLabel("person"))
        assertEquals("VEHICLE", m.safetyLabel("car"))
        assertNull("furniture is dropped by default", m.safetyLabel("chair"))
        assertEquals("OBSTACLE", CocoClassMapper(includeGenericObstacles = true).safetyLabel("chair"))
        assertEquals("person", m.nameOf(0))
        assertEquals("car", m.nameOf(2))
    }
}
