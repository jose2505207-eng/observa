package com.observa.app.runtime

import android.content.Context
import android.util.Log
import com.observa.app.hazard.Detection
import com.observa.app.hazard.FrameInput

/**
 * Real ExecuTorch detection path — currently in validation-only mode. It checks whether a model
 * asset is bundled and whether QNN acceleration is available, logs the result clearly, and sets
 * its [status] truthfully. It does NOT fabricate detections: until inference is actually wired to
 * the bundled ExecuTorch AAR, [analyzeFrame] returns nothing and the app uses the heuristic
 * fallback. This keeps the architecture (and dashboard) honest and ready for a real model.
 */
class ExecuTorchDetector(
    context: Context,
    private val modelPath: String = "models/observa_detector.pte",
) : VisionRuntime {

    override val name = "ExecuTorch"

    /** True if a model file is actually bundled in assets and can be opened. */
    val modelPresent: Boolean = assetExists(context, modelPath)

    /** Whether QNN (NPU) acceleration libraries are packaged. */
    val qnn: QnnStatus = QnnRuntimeChecker.check(context)

    override val status: RuntimeStatus = when {
        !modelPresent -> RuntimeStatus.NOT_CONNECTED          // nothing to load yet
        else -> RuntimeStatus.BUNDLED_NOT_INVOKED             // model present, inference not wired
    }

    /** Human-readable detail for the dashboard, e.g. "model absent · QNN absent (CPU fallback)". */
    val detail: String = "model ${if (modelPresent) "found" else "absent"} · ${qnn.label}"

    init {
        Log.i(TAG, "ExecuTorch init: $detail → status=${status.label} (modelPath=$modelPath)")
        if (modelPresent) {
            Log.i(TAG, "Model bundled but inference not yet wired; running heuristic fallback.")
        } else {
            Log.i(TAG, "No model bundled at assets/$modelPath; running heuristic fallback.")
        }
    }

    override suspend fun analyzeFrame(frame: FrameInput): List<Detection> = emptyList()

    private companion object {
        const val TAG = "OBSERVA_EXECUTORCH"

        fun assetExists(context: Context, path: String): Boolean = try {
            context.assets.open(path).use { true }
        } catch (_: Exception) {
            false
        }
    }
}
