package com.observa.app.inference

import android.util.Log

/**
 * Collects [BackendAttempt]s across the fallback chain and exposes a truthful summary for the in-app
 * NPU Debug screen + a copy-paste debug report. Single grep tag: **OBSERVA_NPU**. Thread-safe enough
 * for the init path (attempts are appended on one init thread; reads are cheap snapshots).
 *
 * "NPU active" is derived ONLY from a recorded [BackendStage.ACTIVE]/[BackendStage.WARMUP_FORWARD]
 * success on a QNN backend — never from library/asset presence.
 */
class BackendDiagnostics {

    private val attempts = ArrayList<BackendAttempt>()

    @Synchronized
    fun record(attempt: BackendAttempt) {
        attempts.add(attempt)
        Log.i(TAG, attempt.logLine())
    }

    @Synchronized fun all(): List<BackendAttempt> = attempts.toList()

    @Synchronized fun clear() { attempts.clear() }

    /** The backend that actually reached ACTIVE (real warm-up forward), or null if none did. */
    @Synchronized
    fun activeBackend(): BackendKind? =
        attempts.lastOrNull { it.stage == BackendStage.ACTIVE && it.success }?.backend

    /** True only when a QNN backend truly went ACTIVE. */
    fun npuActive(): Boolean = activeBackend().let { it == BackendKind.EXECUTORCH_QNN || it == BackendKind.LITERT_QNN }

    /** The first failing attempt (for "first failing log line"). */
    @Synchronized
    fun firstFailure(): BackendAttempt? = attempts.firstOrNull { !it.success }

    /** Why the final backend was chosen / why NPU was not used. */
    @Synchronized
    fun fallbackReason(): String {
        if (npuActive()) return "QNN/NPU active (warm-up forward succeeded)."
        val qnnFail = attempts.firstOrNull {
            it.backend == BackendKind.EXECUTORCH_QNN && !it.success
        }
        return qnnFail?.let {
            "Fell back from QNN at ${it.stage.name}: ${it.nativeHint.ifBlank { it.message }}"
        } ?: "No QNN attempt recorded."
    }

    /** One-line summary for the accessible summary node. */
    fun summaryLine(): String {
        val active = activeBackend()
        return if (npuActive()) "Backend: ${active?.label} active."
        else "Backend: ${active?.label ?: BackendKind.XNNPACK_CPU.label}. ${fallbackReason()}"
    }

    /** Full copy-paste debug report (judges / Qualcomm). */
    @Synchronized
    fun report(header: String): String = buildString {
        appendLine("== OBSERVA NPU DEBUG REPORT ==")
        appendLine(header)
        appendLine("NPU active: ${npuActive()}")
        appendLine("Selected backend: ${(activeBackend() ?: BackendKind.XNNPACK_CPU).label}")
        appendLine("Fallback reason: ${fallbackReason()}")
        appendLine("Priority: ${BackendSelector.priorityDescription()}")
        appendLine("-- attempts --")
        attempts.forEach { a ->
            appendLine("[${a.backend.name}/${a.stage.name}] success=${a.success}" +
                (if (a.elapsedMs > 0) " ${a.elapsedMs}ms" else "") +
                (if (a.errorCode.isNotBlank()) " code=${a.errorCode}" else "") +
                (if (a.nativeHint.isNotBlank()) " native=${a.nativeHint}" else "") +
                (if (a.message.isNotBlank()) " :: ${a.message}" else "") +
                (if (a.modelPath.isNotBlank()) "\n    model=${a.modelPath} sha=${a.modelChecksum.take(16)}" else "") +
                (if (a.libraryPaths.isNotBlank()) "\n    libs=${a.libraryPaths}" else ""))
        }
    }

    companion object {
        const val TAG = "OBSERVA_NPU"

        /** Extract a short native hint from an exception/message (skel/device_handle codes). */
        fun nativeHintFrom(text: String): String {
            val parts = ArrayList<String>(2)
            Regex("skel,? error:? (\\d+)", RegexOption.IGNORE_CASE).find(text)?.let { parts += "skel ${it.groupValues[1]}" }
            Regex("device_handle.*?(\\d{4,})", RegexOption.IGNORE_CASE).find(text)?.let { parts += "device_handle ${it.groupValues[1]}" }
            Regex("error=?(\\d{4,})", RegexOption.IGNORE_CASE).find(text)?.let { if (parts.isEmpty()) parts += "error ${it.groupValues[1]}" }
            return parts.joinToString(" / ")
        }
    }
}
