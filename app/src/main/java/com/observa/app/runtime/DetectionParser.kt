package com.observa.app.runtime

import android.util.Log
import com.observa.app.hazard.Direction

/**
 * One model output tensor, decoupled from ExecuTorch types (no `org.pytorch.executorch` import)
 * so that parsing logic is pure JVM code and fully unit-testable.
 */
class TensorOutput(val shape: LongArray, val data: FloatArray)

/**
 * Turns raw model output tensors into [DetectedObject]s. The contract is strict: an implementation
 * MUST return an empty list for any output shape it does not explicitly understand, so the app can
 * never fabricate detections from an unverified tensor.
 */
interface DetectionParser {
    val name: String
    fun parse(outputs: List<TensorOutput>, nowMs: Long): List<DetectedObject>
}

/**
 * Default parser used until a real model's output contract is verified. It NEVER emits a label — it
 * logs the observed output shapes (so the contract can be discovered) and returns empty. This keeps
 * "AI detection" honest: no model output is trusted until its format is proven on device.
 */
class StrictUnknownShapeParser : DetectionParser {
    override val name = "strict-unknown-shape"

    override fun parse(outputs: List<TensorOutput>, nowMs: Long): List<DetectedObject> {
        val shapes = outputs.joinToString { it.shape.contentToString() }
        Log.i(
            "OBSERVA_MODEL",
            "parser=$name observed output shapes=[$shapes]; refusing to emit labels until the " +
                "output contract is documented and verified.",
        )
        return emptyList()
    }
}

/**
 * Parser for a YOLO-family single output tensor (e.g. exported YOLOv8n). Supports both common
 * layouts and both with/without an explicit objectness channel:
 *
 *  - `[1, C, N]` (channels-first, YOLOv8)   and  `[1, N, C]` (channels-last, YOLOv5)
 *  - `C = 4 + numClasses` (no objectness)   and  `C = 5 + numClasses` (with objectness)
 *
 * Box coordinates are assumed cx,cy,w,h in input-pixel units (0..[inputSize]); they are normalized
 * to [0,1]. Scores below [confThreshold] are dropped; overlapping boxes are removed by class-aware
 * NMS at [iouThreshold]. Classes are mapped to safety categories via [classMapper]; anything that
 * maps to null (e.g. a teddy bear) is discarded — never spoken. Pure logic; unit-testable.
 *
 * The exact model + export that produces this layout is documented in
 * `assets/models/README.md` and `scripts/export_detector.py`.
 */
class YoloDetectionParser(
    private val numClasses: Int = 80,
    private val confThreshold: Float = 0.40f,
    private val iouThreshold: Float = 0.45f,
    private val inputSize: Int = 640,
    private val classMapper: CocoClassMapper = CocoClassMapper(),
) : DetectionParser {

    override val name = "yolo"

    override fun parse(outputs: List<TensorOutput>, nowMs: Long): List<DetectedObject> {
        val t = outputs.firstOrNull() ?: return emptyList()
        val s = t.shape
        if (s.size != 3 || s[0] != 1L) return emptyList()
        val a = s[1].toInt()
        val b = s[2].toInt()

        // Resolve which axis is "attributes per box" (4/5 + classes) vs "number of boxes".
        val withObj = numClasses + 5
        val noObj = numClasses + 4
        val (attrs, numBoxes, channelsFirst) = when {
            a == withObj || a == noObj -> Triple(a, b, true)   // [1, C, N]
            b == withObj || b == noObj -> Triple(b, a, false)  // [1, N, C]
            else -> return emptyList()                          // unknown layout → honest empty
        }
        val hasObjectness = attrs == withObj
        val classOffset = if (hasObjectness) 5 else 4

        fun at(box: Int, attr: Int): Float =
            if (channelsFirst) t.data[attr * numBoxes + box] else t.data[box * attrs + attr]

        val candidates = ArrayList<DetectedObject>()
        for (i in 0 until numBoxes) {
            val obj = if (hasObjectness) at(i, 4) else 1f
            // best class
            var bestCls = -1
            var bestProb = 0f
            for (c in 0 until numClasses) {
                val p = at(i, classOffset + c)
                if (p > bestProb) { bestProb = p; bestCls = c }
            }
            val score = obj * bestProb
            if (score < confThreshold || bestCls < 0) continue
            val className = classMapper.nameOf(bestCls) ?: continue
            val safety = classMapper.safetyLabel(className) ?: continue

            val cx = at(i, 0) / inputSize
            val cy = at(i, 1) / inputSize
            val w = at(i, 2) / inputSize
            val h = at(i, 3) / inputSize
            val box = BoundingBox(
                left = (cx - w / 2f).coerceIn(0f, 1f),
                top = (cy - h / 2f).coerceIn(0f, 1f),
                right = (cx + w / 2f).coerceIn(0f, 1f),
                bottom = (cy + h / 2f).coerceIn(0f, 1f),
            )
            candidates.add(
                DetectedObject(
                    label = safety,
                    rawClass = className,
                    confidence = score,
                    box = box,
                    direction = DirectionMapper.fromCenterX(box.centerX),
                    hazardRelevant = isHazardRelevant(safety, box),
                    timestampMs = nowMs,
                )
            )
        }
        return nms(candidates, iouThreshold)
    }

    private fun isHazardRelevant(label: String, box: BoundingBox): Boolean = when (label) {
        "PERSON", "VEHICLE" -> true
        else -> box.area >= 0.15f && box.centerX in 0.30f..0.70f // a large, central obstacle
    }

    companion object {
        /** Class-aware non-max suppression. Pure; unit-testable. */
        fun nms(dets: List<DetectedObject>, iouThreshold: Float): List<DetectedObject> {
            val sorted = dets.sortedByDescending { it.confidence }.toMutableList()
            val kept = ArrayList<DetectedObject>()
            while (sorted.isNotEmpty()) {
                val best = sorted.removeAt(0)
                kept.add(best)
                sorted.removeAll { it.label == best.label && iou(it.box, best.box) > iouThreshold }
            }
            return kept
        }

        fun iou(a: BoundingBox, b: BoundingBox): Float {
            val ix = maxOf(0f, minOf(a.right, b.right) - maxOf(a.left, b.left))
            val iy = maxOf(0f, minOf(a.bottom, b.bottom) - maxOf(a.top, b.top))
            val inter = ix * iy
            val union = a.area + b.area - inter
            return if (union <= 0f) 0f else inter / union
        }
    }
}
