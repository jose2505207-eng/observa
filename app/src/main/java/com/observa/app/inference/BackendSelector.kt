package com.observa.app.inference

/**
 * Encodes OBSERVA's backend priority. **NPU is preferred over everything; GPU is never preferred over
 * NPU and is in fact not implemented at all** (there is no Vulkan/GPU delegate in the app). XNNPACK CPU
 * is the honest last-resort fallback. The [disableGpuFallback] flag exists for NPU investigation and to
 * make the policy explicit; since no GPU path exists it is informational here, but it guarantees the
 * documented order if a GPU path were ever added.
 */
object BackendSelector {

    /** Session flag: when true, a GPU/Vulkan path (if it existed) must not be selected. Default true. */
    @Volatile var disableGpuFallback: Boolean = true

    /** The ordered priority, highest first. */
    fun priority(): List<BackendKind> = buildList {
        add(BackendKind.EXECUTORCH_QNN)
        add(BackendKind.LITERT_QNN)
        if (!disableGpuFallback) add(BackendKind.VULKAN_GPU)
        add(BackendKind.XNNPACK_CPU)
    }

    fun priorityDescription(): String =
        priority().joinToString(" → ") { it.label } +
            if (disableGpuFallback) "  (GPU fallback disabled for NPU investigation; no GPU path is implemented anyway)" else ""
}
