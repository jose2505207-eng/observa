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
    /**
     * How far the QNN/NPU path got, for the debug status. One of: "not started", "libs present" /
     * "libs missing", "model loaded", "no QNN backend", "backend init / warm-up failed", "active".
     * Never reports "active" unless a real warm-up `forward` ran on the QNN backend.
     */
    var qnnStage: String = "not started"; private set

    /** Structured, grep-friendly (OBSERVA_NPU) backend diagnostics for the in-app NPU Debug screen. */
    val diagnostics = com.observa.app.inference.BackendDiagnostics()

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
        diagnostics.clear()
        qnn = QnnRuntimeChecker.check(context)
        qnnStage = if (qnn == QnnStatus.NOT_PRESENT) "libs missing" else "libs present"
        val nativeDirPath = context.applicationInfo.nativeLibraryDir
        diagnostics.record(
            com.observa.app.inference.BackendAttempt(
                backend = com.observa.app.inference.BackendKind.EXECUTORCH_QNN,
                stage = com.observa.app.inference.BackendStage.LIB_LOAD,
                success = qnn != QnnStatus.NOT_PRESENT,
                message = "QNN runtime libs ${qnn.label}",
                libraryPaths = nativeDirPath,
            )
        )
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
            val kind = if (cand.requireQnn) com.observa.app.inference.BackendKind.EXECUTORCH_QNN
            else com.observa.app.inference.BackendKind.XNNPACK_CPU
            if (!assetExists(context, cand.path)) {
                diagnostics.record(attempt(kind, com.observa.app.inference.BackendStage.ASSET_CHECK,
                    false, message = "model not bundled: ${cand.path}", modelPath = cand.path))
                if (cand.requireQnn) qnnError = "QNN model not bundled (${cand.path})"
                continue
            }
            diagnostics.record(attempt(kind, com.observa.app.inference.BackendStage.ASSET_CHECK, true, modelPath = cand.path))
            var checksum = "" // hoisted so the failure path can also record the .pte sha
            try {
                val path = copyAssetToFiles(context, cand.path)
                val bytes = File(path).length()
                checksum = sha256Of(path)
                val t0 = SystemClock.elapsedRealtime()
                val m = Module.load(path)
                val ms = SystemClock.elapsedRealtime() - t0
                if (cand.requireQnn) qnnStage = "model loaded"
                val backendList = runCatching { m.getMethodMetadata("forward").backends.joinToString() }
                    .getOrDefault("")
                diagnostics.record(attempt(kind, com.observa.app.inference.BackendStage.MODEL_LOAD, true,
                    elapsedMs = ms, modelPath = cand.path, modelChecksum = checksum,
                    message = "loaded ${bytes / 1024}KB, backends=[$backendList]"))
                val usesQnn = backendList.contains("qnn", ignoreCase = true)
                if (cand.requireQnn && !usesQnn) {
                    m.close()
                    qnnStage = "no QNN backend"
                    qnnError = "model loaded but no QNN backend (backends=[$backendList])"
                    diagnostics.record(attempt(kind, com.observa.app.inference.BackendStage.BACKEND_INIT, false,
                        message = qnnError, modelPath = cand.path))
                    Log.w(TAG, "QNN candidate rejected: $qnnError → trying next")
                    continue
                }
                // Warm-up forward: proves the backend actually executes on this device. If the QNN/HTP
                // runtime libs are missing or HTP fails to init, this throws and we fall back honestly.
                val w0 = SystemClock.elapsedRealtime()
                warmUp(m)
                val warmupMs = SystemClock.elapsedRealtime() - w0
                if (cand.requireQnn) qnnStage = "active"
                diagnostics.record(attempt(kind, com.observa.app.inference.BackendStage.WARMUP_FORWARD, true, elapsedMs = warmupMs))

                module = m
                parser = cand.parser
                modelBytes = bytes
                modelVersion = readAsset(context, "${cand.path}.version") ?: "unspecified"
                loadMs = ms
                backends = backendList
                status = if (usesQnn) InferenceStatus.LOADED_QNN else InferenceStatus.LOADED_CPU
                // ACTIVE is recorded only after a real warm-up forward succeeded.
                diagnostics.record(attempt(kind, com.observa.app.inference.BackendStage.ACTIVE, true,
                    modelPath = cand.path, modelChecksum = checksum, message = "backends=[$backendList]"))
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
                // The skel/device_handle code lives in the native [Qnn ExecuTorch] log, not the Java
                // exception, so enrich the hint by reading our own process's recent logcat.
                val hint = com.observa.app.inference.BackendDiagnostics.nativeHintFrom(e.message ?: e.toString())
                    .ifBlank { com.observa.app.inference.BackendDiagnostics.nativeHintFrom(readNativeQnnLog()) }
                if (cand.requireQnn) {
                    qnnStage = "backend init / warm-up failed"
                    qnnError = e.message ?: e.toString()
                    diagnostics.record(attempt(kind, com.observa.app.inference.BackendStage.WARMUP_FORWARD, false,
                        errorCode = hint, exceptionClass = e.javaClass.simpleName, message = e.message ?: e.toString(),
                        nativeHint = hint, modelPath = cand.path, modelChecksum = checksum))
                    diagnostics.record(attempt(kind, com.observa.app.inference.BackendStage.FALLBACK, true,
                        message = "falling back to next candidate"))
                    Log.e(TAG, "QNN candidate failed to load/warm-up → falling back to XNNPACK", e)
                    continue
                }
                status = InferenceStatus.FAILED
                detail = "load failed: ${e.message} · ${qnn.label}"
                diagnostics.record(attempt(kind, com.observa.app.inference.BackendStage.MODEL_LOAD, false,
                    exceptionClass = e.javaClass.simpleName, message = e.message ?: e.toString(), modelPath = cand.path))
                Log.e(TAG, "model load failed → heuristic fallback", e)
                return status
            }
        }

        status = InferenceStatus.UNAVAILABLE
        detail = "no model loaded · ${qnn.label}" + if (qnnError.isNotBlank()) " · QNN: $qnnError" else ""
        Log.i(TAG, "no detector model available → heuristic fallback. $detail")
        return status
    }

    /** Convenience constructor for a [com.observa.app.inference.BackendAttempt]. */
    private fun attempt(
        backend: com.observa.app.inference.BackendKind,
        stage: com.observa.app.inference.BackendStage,
        success: Boolean,
        elapsedMs: Long = 0L,
        errorCode: String = "",
        exceptionClass: String = "",
        message: String = "",
        nativeHint: String = "",
        modelPath: String = "",
        modelChecksum: String = "",
        libraryPaths: String = "",
    ) = com.observa.app.inference.BackendAttempt(
        backend, stage, success, elapsedMs = elapsedMs, errorCode = errorCode,
        exceptionClass = exceptionClass, message = message, nativeHint = nativeHint,
        modelPath = modelPath, modelChecksum = modelChecksum, libraryPaths = libraryPaths,
    )

    /** Read this process's own recent logcat for the native QnnDsp skel/device_handle line. */
    private fun readNativeQnnLog(): String = runCatching {
        val p = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "400"))
        p.inputStream.bufferedReader().useLines { lines ->
            lines.filter { (it.contains("QnnDsp") || it.contains("device_handle")) &&
                (it.contains("skel") || it.contains("device_handle") || it.contains("4000") || it.contains("14001")) }
                .joinToString(" | ").take(300)
        }
    }.getOrDefault("")

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

        /** SHA-256 of a file (for the debug report — proves which .pte actually loaded). One-time, off UI. */
        fun sha256Of(path: String): String = runCatching {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            File(path).inputStream().use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) { val n = input.read(buf); if (n <= 0) break; md.update(buf, 0, n) }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }.getOrDefault("")
    }
}
