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
 *  - load succeeds → [InferenceStatus.LOADED_QNN] ONLY if `forward`'s MethodMetadata.getBackends()
 *                    includes a QNN backend; otherwise [InferenceStatus.LOADED_CPU].
 *  - any output    → routed through a [DetectionParser]; the default YOLO parser returns empty for
 *                    any unrecognized output shape, so no fabricated detections are ever produced.
 *
 * Pixels for inference come from [FrameInput.rgb], which the analyzer fills only while a model is
 * loaded. Inference runs off the UI thread (see ObservaController).
 */
class ExecuTorchDetector(
    private val modelAssetPath: String = "models/observa_detector.pte",
    private val parser: DetectionParser = YoloDetectionParser(),
    private val inputWidth: Int = 640,
    private val inputHeight: Int = 640,
) : InferenceEngine {

    @Volatile
    override var status: InferenceStatus = InferenceStatus.UNAVAILABLE
        private set

    @Volatile private var module: Module? = null
    var qnn: QnnStatus = QnnStatus.UNKNOWN; private set
    var detail: String = "not initialized"; private set

    // --- Diagnostics (read by the UI/Braille diagnostics surface) ---
    var modelBytes: Long = 0L; private set
    var modelVersion: String = "unknown"; private set
    var loadMs: Long = 0L; private set
    var lastLatencyMs: Long = 0L; private set
    var lastOutputShapes: String = "—"; private set
    var inputShape: String = "[1, 3, $inputHeight, $inputWidth]"; private set
    var backends: String = ""; private set
    val qnnActive: Boolean get() = status == InferenceStatus.LOADED_QNN
    private var latencySum = 0L
    private var latencyCount = 0L
    val avgLatencyMs: Long get() = if (latencyCount == 0L) 0L else latencySum / latencyCount

    override fun initialize(context: Context): InferenceStatus {
        qnn = QnnRuntimeChecker.check(context)
        if (!assetExists(context, modelAssetPath)) {
            status = InferenceStatus.UNAVAILABLE
            detail = "model absent · ${qnn.label}"
            Log.i(TAG, "model absent at assets/$modelAssetPath → heuristic fallback. ${qnn.label}")
            return status
        }
        modelVersion = readAsset(context, "$modelAssetPath.version") ?: "unspecified"
        return try {
            val path = copyAssetToFiles(context, modelAssetPath)
            modelBytes = File(path).length()
            val t0 = SystemClock.elapsedRealtime()
            val m = Module.load(path)
            module = m
            loadMs = SystemClock.elapsedRealtime() - t0
            val methods = runCatching { m.getMethods().joinToString() }.getOrDefault("?")
            backends = runCatching { m.getMethodMetadata("forward").backends.joinToString() }
                .getOrDefault("")
            val usesQnn = backends.contains("qnn", ignoreCase = true)
            status = if (usesQnn) InferenceStatus.LOADED_QNN else InferenceStatus.LOADED_CPU
            detail = "${status.label} · ${modelBytes / 1024}KB · v=$modelVersion · backends=[$backends]"
            Log.i(
                TAG,
                "load success in ${loadMs}ms; bytes=$modelBytes; version=$modelVersion; " +
                    "methods=[$methods]; forward backends=[$backends]; parser=${parser.name}; " +
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
            val nowMs = System.currentTimeMillis()
            val input = preprocess(rgb, frame.rgbWidth, frame.rgbHeight)
            val tensor = Tensor.fromBlob(input, longArrayOf(1, 3, inputHeight.toLong(), inputWidth.toLong()))
            val t0 = SystemClock.elapsedRealtime()
            val out: Array<EValue> = m.forward(EValue.from(tensor))
            lastLatencyMs = SystemClock.elapsedRealtime() - t0
            latencySum += lastLatencyMs; latencyCount++
            val outputs = out.filter { it.isTensor }.map {
                val t = it.toTensor()
                TensorOutput(t.shape(), t.dataAsFloatArray)
            }
            lastOutputShapes = outputs.joinToString { it.shape.contentToString() }
            val objects = parser.parse(outputs, nowMs)
            Log.i(
                "OBSERVA_MODEL",
                "inference ${lastLatencyMs}ms (avg ${avgLatencyMs}ms); outputs=[$lastOutputShapes]; " +
                    "objects=${objects.size} via parser=${parser.name} " +
                    objects.joinToString(prefix = "[", postfix = "]") { "${it.rawClass}->${it.label}@${"%.2f".format(it.confidence)}/${it.direction}" },
            )
            // Map rich detections to the engine's generic Detection (label/confidence/direction).
            objects.filter { it.hazardRelevant }.map {
                Detection(label = engineLabel(it.label), confidence = it.confidence, direction = it.direction)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "inference failed → no detections this frame (no crash)", e)
            emptyList()
        }
    }

    override fun close() {
        runCatching { module?.close() }
        module = null
    }

    /** Safety category → an engine label the HazardEngine understands. */
    private fun engineLabel(safety: String): String = when (safety) {
        "PERSON" -> "PERSON"
        else -> "OBSTACLE" // VEHICLE and generic OBSTACLE both read as an obstacle hazard
    }

    /**
     * Resize interleaved RGB (0..1) at [w]x[h] to a normalized CHW float buffer at the model input
     * size using nearest-neighbor sampling. Normalization is plain 0..1 (YOLO default).
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

        fun readAsset(context: Context, path: String): String? = try {
            context.assets.open(path).use { it.readBytes().decodeToString().trim() }
        } catch (_: Exception) {
            null
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
