package com.observa.app.runtime

import kotlin.math.exp

/**
 * Parser for the **raw, pre-decode YOLOv8 detection head** (the export used for the Qualcomm QNN/HTP
 * delegate). The QNN-lowered model returns the three multi-scale head tensors *before* anchor decode
 * / DFL / NMS — that decode logic (`make_anchors`, int64 grids) is what blocked QNN lowering, so it
 * is performed here on the CPU after inference instead. See `scripts/export_detector.py --qnn-raw-head`
 * and `docs/implementation/MODEL_RUNTIME.md`.
 *
 * Input: three tensors, one per scale, each `[1, C, H, W]` channels-first, where
 * `C = 4*regMax + numClasses` (64 + 80 = 144 for YOLOv8n). Channels `0..4*regMax-1` are the box DFL
 * logits (4 sides × `regMax` bins, side order left/top/right/bottom); the rest are class logits.
 *
 * Decode is resolution-independent: boxes are normalized directly by the grid dimensions, so no
 * stride/input-size constant is needed. Classes use sigmoid (YOLOv8 has no objectness). Results pass
 * through the same class-aware NMS, COCO→safety mapping, and direction logic as [YoloDetectionParser],
 * so detector behavior is identical regardless of which backend produced the tensors. Pure JVM;
 * unit-tested.
 */
class YoloRawHeadParser(
    private val numClasses: Int = 80,
    private val regMax: Int = 16,
    private val confThreshold: Float = 0.40f,
    private val iouThreshold: Float = 0.45f,
    private val classMapper: CocoClassMapper = CocoClassMapper(),
) : DetectionParser {

    override val name = "yolo-raw-head"

    private val boxChannels = 4 * regMax
    private val channels = boxChannels + numClasses

    override fun parse(outputs: List<TensorOutput>, nowMs: Long): List<DetectedObject> {
        // Accept exactly the multi-scale head tensors we understand; ignore anything else.
        val heads = outputs.filter {
            it.shape.size == 4 && it.shape[0] == 1L && it.shape[1].toInt() == channels
        }
        if (heads.isEmpty()) return emptyList()

        // sigmoid is monotonic, so we can compare class logits against the inverse-sigmoid of the
        // threshold and only run the (more expensive) DFL box decode for cells that survive.
        val logitThreshold = inverseSigmoid(confThreshold)

        val candidates = ArrayList<DetectedObject>()
        for (head in heads) {
            val h = head.shape[2].toInt()
            val w = head.shape[3].toInt()
            val hw = h * w
            val data = head.data
            if (data.size < channels * hw) continue // malformed → skip honestly

            for (y in 0 until h) {
                for (x in 0 until w) {
                    val cell = y * w + x
                    // Best class by logit (one sigmoid only if it can clear the threshold).
                    var bestCls = -1
                    var bestLogit = Float.NEGATIVE_INFINITY
                    for (c in 0 until numClasses) {
                        val v = data[(boxChannels + c) * hw + cell]
                        if (v > bestLogit) { bestLogit = v; bestCls = c }
                    }
                    if (bestLogit < logitThreshold || bestCls < 0) continue
                    val className = classMapper.nameOf(bestCls) ?: continue
                    val safety = classMapper.safetyLabel(className) ?: continue
                    val score = sigmoid(bestLogit)

                    // DFL distances (left, top, right, bottom) in grid-cell units.
                    val dl = dfl(data, hw, cell, 0)
                    val dt = dfl(data, hw, cell, 1)
                    val dr = dfl(data, hw, cell, 2)
                    val db = dfl(data, hw, cell, 3)
                    val ax = x + 0.5f
                    val ay = y + 0.5f
                    val box = BoundingBox(
                        left = ((ax - dl) / w).coerceIn(0f, 1f),
                        top = ((ay - dt) / h).coerceIn(0f, 1f),
                        right = ((ax + dr) / w).coerceIn(0f, 1f),
                        bottom = ((ay + db) / h).coerceIn(0f, 1f),
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
            }
        }
        return YoloDetectionParser.nms(candidates, iouThreshold)
    }

    /** Distributional Focal Loss expectation for one box side: softmax over regMax bins × bin index. */
    private fun dfl(data: FloatArray, hw: Int, cell: Int, side: Int): Float {
        val base = side * regMax
        var maxV = Float.NEGATIVE_INFINITY
        for (b in 0 until regMax) {
            val v = data[(base + b) * hw + cell]
            if (v > maxV) maxV = v
        }
        var sum = 0f
        var weighted = 0f
        for (b in 0 until regMax) {
            val e = exp(data[(base + b) * hw + cell] - maxV)
            sum += e
            weighted += e * b
        }
        return if (sum > 0f) weighted / sum else 0f
    }

    private fun isHazardRelevant(label: String, box: BoundingBox): Boolean = when (label) {
        "PERSON", "VEHICLE" -> true
        else -> box.area >= 0.15f && box.centerX in 0.30f..0.70f
    }

    private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))
    private fun inverseSigmoid(p: Float): Float = kotlin.math.ln(p / (1f - p))
}
