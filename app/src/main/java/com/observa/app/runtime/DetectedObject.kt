package com.observa.app.runtime

import com.observa.app.hazard.Direction

/** Normalized bounding box in [0,1] image coordinates (xyxy). */
data class BoundingBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val area: Float get() = width * height
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * A real detection produced by a [DetectionParser]. Carries everything the hazard layer needs:
 * a safety [label], the original model [rawClass], [confidence], normalized [box]/center,
 * relative [direction], whether it is [hazardRelevant], and the frame [timestampMs].
 */
data class DetectedObject(
    val label: String,        // safety category: "PERSON" | "VEHICLE" | "OBSTACLE"
    val rawClass: String,     // original model class name, e.g. "car"
    val confidence: Float,
    val box: BoundingBox,
    val direction: Direction,
    val hazardRelevant: Boolean,
    val timestampMs: Long,
) {
    val centerX: Float get() = box.centerX
    val centerY: Float get() = box.centerY
}

/** Pure left/center/right mapping from a normalized horizontal center. Unit-testable. */
object DirectionMapper {
    fun fromCenterX(centerX: Float, leftEdge: Float = 0.40f, rightEdge: Float = 0.60f): Direction =
        when {
            centerX < leftEdge -> Direction.LEFT
            centerX > rightEdge -> Direction.RIGHT
            else -> Direction.CENTER
        }
}

/**
 * Maps COCO-80 class indices to OBSERVA safety categories. Pure logic; unit-testable.
 *
 *  - person                       → PERSON
 *  - bicycle/car/motorcycle/bus/truck/train/boat/airplane → VEHICLE
 *  - a documented set of furniture/obstacle classes → OBSTACLE, only when
 *    [includeGenericObstacles] is true (off by default to avoid noisy/unsafe labels).
 *  - everything else              → null (dropped; never spoken).
 */
class CocoClassMapper(private val includeGenericObstacles: Boolean = false) {

    fun nameOf(index: Int): String? = COCO.getOrNull(index)

    /** Returns the safety category for a class name, or null if it must not be surfaced. */
    fun safetyLabel(className: String): String? = when (className) {
        "person" -> "PERSON"
        in VEHICLES -> "VEHICLE"
        in OBSTACLES -> if (includeGenericObstacles) "OBSTACLE" else null
        else -> null
    }

    companion object {
        private val VEHICLES = setOf("bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat")
        private val OBSTACLES = setOf(
            "bench", "chair", "couch", "potted plant", "bed", "dining table", "toilet",
            "tv", "refrigerator", "fire hydrant", "stop sign", "parking meter", "suitcase",
        )

        /** Standard 80-class COCO label order used by YOLO/SSD exports. */
        val COCO = listOf(
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
            "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
            "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
            "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
            "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair",
            "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse",
            "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
            "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
            "toothbrush",
        )
    }
}
