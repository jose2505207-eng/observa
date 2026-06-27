package com.observa.app.runtime

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.observa.app.hazard.Detection
import com.observa.app.hazard.FrameInput
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.io.File

/**
 * Real ExecuTorch inference path. It genuinely attempts to load and run a bundled `.pte`, but is
 * honest at every step:
 *
 *  - model absent  → [InferenceStatus.UNAVAILABLE], heuristic fallback, no detections.
 *  - load fails    → [InferenceStatus.FAILED], heuristic fallback, exact error logged, no crash.
 *  - load succeeds → [InferenceStatus.LOADED_QNN] only if the `forward` method's backends actually
 *                    include a QNN backend; otherwise [InferenceStatus.LOADED_CPU].
 *  - any output    → routed through a [DetectionParser]; the default refuses to emit labels for an
 *                    unverified output shape, so no fabricated detections are ever produced.
 *
 * Pixels for inference come from [FrameInput.rgb], which the analyzer fills only while a model is
 * loaded. Inference is meant to be called off the UI thread (see ObservaController).
 */
class ExecuTorchDetector(
    private val modelAssetPath: String = "models/observa_detector.pte",
    private val parser: DetectionParser = StrictUnknownShapeParser(),
    private val inputWidth: Int = 224,
    private val inputHeight: Int = 224,
) : InferenceEngine {

    @Volatile
    override var status: InferenceStatus = InferenceStatus.UNAVAILABLE
        private set

    @Volatile private var module: Module? = null
    var qnn: QnnStatus = QnnStatus.UNKNOWN; private set
    var lastLatencyMs: Long = 0L; private set
    var detail: String = "not initialized"; private set

    override fun initialize(context: Context): InferenceStatus {
        qnn = QnnRuntimeChecker.check(context)
        if (!assetExists(context, modelAssetPath)) {
            status = InferenceStatus.UNAVAILABLE
            detail = "model absent · ${qnn.label}"
            Log.i(TAG, "model absent at assets/$modelAssetPath → heuristic fallback. ${qnn.label}")
            return status
        }
        return try {
            val path = copyAssetToFiles(context, modelAssetPath)
            val t0 = SystemClock.elapsedRealtime()
            val m = Module.load(path)
            module = m
            val loadMs = SystemClock.elapsedRealtime() - t0
            val methods = runCatching { m.getMethods().joinToString() }.getOrDefault("?")
            val backends = runCatching { m.getMethodMetadata("forward").backends.joinToString() }
                .getOrDefault("")
            val usesQnn = backends.contains("qnn", ignoreCase = true)
            status = if (usesQnn) InferenceStatus.LOADED_QNN else InferenceStatus.LOADED_CPU
            detail = "${status.label} · backends=[$backends]"
            Log.i(
                TAG,
                "load success in ${loadMs}ms; methods=[$methods]; forward backends=[$backends]; " +
                    "QNN ${if (usesQnn) "ACTIVE for this model" else "present but NOT active (model not QNN-delegated)"}.",
            )
            status
        } catch (e: Throwable) {
            module = null
            status = InferenceStatus.FAILED
            detail = "load failed: ${e.message} · ${qnn.label}"
            Log.e(TAG, "model load failed → heuristic fallback", e)
            status
        }
    }

    override suspend fun analyzeFrame(frame: FrameInput): List<Detection> {
        val m = module ?: return emptyList()
        val rgb = frame.rgb ?: return emptyList()
        return try {
            val input = preprocess(rgb, frame.rgbWidth, frame.rgbHeight)
            val tensor = Tensor.fromBlob(
                input,
                longArrayOf(1, 3, inputHeight.toLong(), inputWidth.toLong()),
            )
            val t0 = SystemClock.elapsedRealtime()
            val out: Array<EValue> = m.forward(EValue.from(tensor))
            lastLatencyMs = SystemClock.elapsedRealtime() - t0
            val outputs = out.filter { it.isTensor }.map {
                val t = it.toTensor()
                TensorOutput(t.shape(), t.dataAsFloatArray)
            }
            Log.i(
                TAG,
                "inference ${lastLatencyMs}ms; output tensor shapes=${outputs.map { it.shape.contentToString() }}",
            )
            val detections = parser.parse(outputs)
            Log.i("OBSERVA_MODEL", "detection count=${detections.size} via parser=${parser.name}")
            detections
        } catch (e: Throwable) {
            Log.e(TAG, "inference failed → no detections this frame (no crash)", e)
            emptyList()
        }
    }

    override fun close() {
        runCatching { module?.close() }
        module = null
    }

    /**
     * Resize interleaved RGB (0..1) at [w]x[h] to a normalized CHW float buffer at the model input
     * size using nearest-neighbor sampling. Normalization is plain 0..1 — documented in
     * assets/models/README.md as the assumed input contract.
     */
    private fun preprocess(rgb: FloatArray, w: Int, h: Int): FloatArray {
        val out = FloatArray(3 * inputWidth * inputHeight)
        val plane = inputWidth * inputHeight
        for (y in 0 until inputHeight) {
            val sy = if (h > 0) y * h / inputHeight else 0
            for (x in 0 until inputWidth) {
                val sx = if (w > 0) x * w / inputWidth else 0
                val si = (sy * w + sx) * 3
                val o = y * inputWidth + x
                out[o] = rgb[si]
                out[plane + o] = rgb[si + 1]
                out[2 * plane + o] = rgb[si + 2]
            }
        }
        return out
    }

    private companion object {
        const val TAG = "OBSERVA_EXECUTORCH"

        fun assetExists(context: Context, path: String): Boolean = try {
            context.assets.open(path).use { true }
        } catch (_: Exception) {
            false
        }

        /** ExecuTorch loads from a filesystem path, so copy the bundled asset into internal storage. */
        fun copyAssetToFiles(context: Context, assetPath: String): String {
            val outFile = File(context.filesDir, assetPath.substringAfterLast('/'))
            context.assets.open(assetPath).use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            return outFile.absolutePath
        }
    }
}
