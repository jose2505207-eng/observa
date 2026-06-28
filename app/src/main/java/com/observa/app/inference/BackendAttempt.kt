package com.observa.app.inference

/** Which inference backend an attempt targeted. GPU/Vulkan is listed for honesty — OBSERVA has no GPU
 *  path, so it is never selected; if one were ever added it must be labeled GPU, never NPU. */
enum class BackendKind(val label: String) {
    EXECUTORCH_QNN("ExecuTorch QNN/NPU"),
    LITERT_QNN("LiteRT QNN/NPU"),
    VULKAN_GPU("Vulkan/GPU"),
    XNNPACK_CPU("XNNPACK CPU"),
    UNKNOWN("unknown"),
}

/** The lifecycle stage a [BackendAttempt] reached. "ACTIVE" requires a real warm-up forward. */
enum class BackendStage {
    ASSET_CHECK,
    LIB_LOAD,
    MODEL_LOAD,
    BACKEND_INIT,
    WARMUP_FORWARD,
    FIRST_REAL_FORWARD,
    ACTIVE,
    FALLBACK,
}

/**
 * One structured record of a backend attempt at one stage. Captured by [BackendDiagnostics] so the
 * in-app NPU Debug screen and the `OBSERVA_NPU` logcat show exactly where and why a backend was chosen
 * or rejected. Nothing here is ever fabricated — `success=true` at [BackendStage.ACTIVE]/WARMUP_FORWARD
 * is only recorded when a real forward executed.
 */
data class BackendAttempt(
    val backend: BackendKind,
    val stage: BackendStage,
    val success: Boolean,
    val startedAtMs: Long = System.currentTimeMillis(),
    val elapsedMs: Long = 0L,
    val errorCode: String = "",
    val exceptionClass: String = "",
    val message: String = "",
    /** A short native-log hint extracted from the failure (e.g. "skel 4000 / device_handle 14001"). */
    val nativeHint: String = "",
    val modelPath: String = "",
    val modelChecksum: String = "",
    val libraryPaths: String = "",
) {
    /** Grep-friendly one-liner under the single OBSERVA_NPU tag. */
    fun logLine(): String = buildString {
        append("attempt=").append(backend.name)
        append(" stage=").append(stage.name)
        append(" success=").append(success)
        if (errorCode.isNotBlank()) append(" code=").append(errorCode)
        if (nativeHint.isNotBlank()) append(" native=").append(nativeHint)
        if (message.isNotBlank()) append(" detail=").append(message)
    }
}
