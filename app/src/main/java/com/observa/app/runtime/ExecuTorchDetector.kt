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
 *  - Tries the **QNN/NPU** model first ([qnnAssetPath], raw-head export, decoded by [YoloRawHeadParser]).
 *    QNN is accepted only if (a) the model's `forward` metadata actually lists a QNN backend AND
 *    (b) a warm-up `forward` executes without throwing — so "QNN active" can never be faked when the
 *    device QNN/HTP runtime is missing or fails to initialize.
 *  - Falls back to the proven **XNNPACK CPU** model ([cpuAssetPath], decoded head, [YoloDetectionParser]).
 *  - model absent → [InferenceStatus.UNAVAILABLE]; load/run fails → fall back or
 *    [InferenceStatus.FAILED]; exact errors logged; never crashes.
 *  - any output → routed through the selected [DetectionParser], which returns empty for any
 *    unrecognized shape, so no fabricated detections are ever produced.
 *
 * Pixels for inference come from [FrameInput.rgb], which the analyzer fills only while a model is
 * loaded. Inference runs off the UI thread (see ObservaController).
 */
class ExecuTorchDetector(
    private val qnnAssetPath: String = "models/detector_yolov8n_qnn_sm8750.pte",
    private val cpuAssetPath: String = "models/observa_detector.pte",
    private val inputWidth: Int = 320,
    private val inputHeight: Int = 320,
) : InferenceEngine {

    @Volatile
    override var status: InferenceStatus = InferenceStatus.UNAVAILABLE
        private set

    @Volatile private var module: Module? = null
    // Parser is chosen per loaded model: raw-head for QNN, decoded-head for XNNPACK.
    @Volatile private var parser: DetectionParser = YoloDetectionParser(inputSize = inputWidth)
    var qnn: QnnStatus = QnnStatus.UNKNOWN; private set
    var detail: String = "not initialized"; private set
    /** Exact reason the QNN/NPU path was not used (empty if QNN is active). For debug status. */
    var qnnError: String = ""; private set

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

    /** A model OBSERVA may load, in priority order. */
    private class Candidate(
        val path: String,
        val parser: DetectionParser,
        val requireQnn: Boolean,
    )

    override fun initialize(context: Context): InferenceStatus {
        qnn = QnnRuntimeChecker.check(context)
        // Point the Hexagon DSP (cDSP) at our extracted skel so QNN HTP can load libQnnHtpV79Skel.so.
        // Without this the device reports "Failed to load skel, error 4000" and HTP init fails.
        runCatching {
            val nativeDir = context.applicationInfo.nativeLibraryDir
            val dspPaths = "$nativeDir;/vendor/dsp/cdsp;/vendor/lib/rfsa/adsp;/vendor/lib64/rfsa/adsp;/system/lib/rfsa/adsp"
            android.system.Os.setenv("ADSP_LIBRARY_PATH", dspPaths, true)
            Log.i(TAG, "ADSP_LIBRARY_PATH=$dspPaths")
        }.onFailure { Log.w(TAG, "could not set ADSP_LIBRARY_PATH: ${it.message}") }
        val candidates = listOf(
            // QNN/NPU first — must prove a QNN backend AND a working warm-up forward.
            Candidate(qnnAssetPath, YoloRawHeadParser(), requireQnn = true),
            // Proven XNNPACK CPU fallback (decoded head).
            Candidate(cpuAssetPath, YoloDetectionParser(inputSize = inputWidth), requireQnn = false),
        )

        for (cand in candidates) {
            if (!assetExists(context, cand.path)) {
                if (cand.requireQnn) qnnError = "QNN model not bundled ($cand.path)"
                continue
            }
            try {
                val path = copyAssetToFiles(context, cand.path)
                val bytes = File(path).length()
                val t0 = SystemClock.elapsedRealtime()
                val m = Module.load(path)
                val ms = SystemClock.elapsedRealtime() - t0
                val backendList = runCatching { m.getMethodMetadata("forward").backends.joinToString() }
                    .getOrDefault("")
                val usesQnn = backendList.contains("qnn", ignoreCase = true)
                if (cand.requireQnn && !usesQnn) {
                    m.close()
                    qnnError = "model loaded but no QNN backend (backends=[$backendList])"
                    Log.w(TAG, "QNN candidate rejected: $qnnError → trying next")
                    continue
                }
                // Warm-up forward: proves the backend actually executes on this device. If the QNN/HTP
                // runtime libs are missing or HTP fails to init, this throws and we fall back honestly.
                warmUp(m)

                module = m
                parser = cand.parser
                modelBytes = bytes
                modelVersion = readAsset(context, "${cand.path}.version") ?: "unspecified"
                loadMs = ms
                backends = backendList
                status = if (usesQnn) InferenceStatus.LOADED_QNN else InferenceStatus.LOADED_CPU
                detail = "${status.label} · ${modelBytes / 1024}KB · v=$modelVersion · backends=[$backends]" +
                    if (status == InferenceStatus.LOADED_CPU && qnnError.isNotBlank()) " · QNN: $qnnError" else ""
                Log.i(
                    TAG,
                    "load success in ${loadMs}ms; model=${cand.path}; bytes=$modelBytes; " +
                        "version=$modelVersion; forward backends=[$backends]; parser=${parser.name}; " +
                        if (usesQnn) "QNN/NPU ACTIVE." else "XNNPACK CPU. ${if (qnnError.isNotBlank()) "QNN attempted: $qnnError" else ""}",
                )
                return status
            } catch (e: Throwable) {
                module = null
                if (cand.requireQnn) {
                    qnnError = e.message ?: e.toString()
                    Log.e(TAG, "QNN candidate failed to load/warm-up → falling back to XNNPACK", e)
                    continue
                }
                status = InferenceStatus.FAILED
                detail = "load failed: ${e.message} · ${qnn.label}"
                Log.e(TAG, "model load failed → heuristic fallback", e)
                return status
            }
        }

        status = InferenceStatus.UNAVAILABLE
        detail = "no model loaded · ${qnn.label}" + if (qnnError.isNotBlank()) " · QNN: $qnnError" else ""
        Log.i(TAG, "no detector model available → heuristic fallback. $detail")
        return status
    }

    /** One forward on a zeroed input to confirm the loaded backend can actually execute. */
    private fun warmUp(m: Module) {
        val input = FloatArray(3 * inputWidth * inputHeight)
        val tensor = Tensor.fromBlob(input, longArrayOf(1, 3, inputHeight.toLong(), inputWidth.toLong()))
        m.forward(EValue.from(tensor))
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
