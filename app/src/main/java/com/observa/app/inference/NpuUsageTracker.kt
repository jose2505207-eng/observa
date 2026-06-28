package com.observa.app.inference

/** An immutable snapshot of live NPU/detector usage for the NPU Data graph. */
data class NpuUsageSnapshot(
    val backendLabel: String,
    val npuActive: Boolean,
    /** Recent per-inference latencies in ms, oldest→newest (for the graph line). */
    val samples: List<Float>,
    val currentMs: Float,
    val avgMs: Float,
    val minMs: Float,
    val maxMs: Float,
    /** Total inferences since load. */
    val total: Long,
    /** Inferences in the last second (live throughput). */
    val ratePerSec: Int,
) {
    /** Short, truthful accessible summary. */
    fun summaryLine(): String =
        if (samples.isEmpty()) "$backendLabel: no inferences yet."
        else "$backendLabel${if (npuActive) " (NPU)" else ""}: ${currentMs.toInt()} ms now, " +
            "${avgMs.toInt()} ms average, $ratePerSec per second."
}

/**
 * Records per-inference latency over a rolling window so the UI can draw a **live** graph of NPU
 * usage. Android exposes no DSP/HTP utilization percentage to a third-party app, so latency + live
 * throughput are the real, measurable signals (never a fabricated "%"). Thread-safe: the detector
 * records from the inference thread; the UI reads [snapshot] from the main thread.
 */
class NpuUsageTracker(private val capacity: Int = 120) {

    private val lock = Any()
    private val latencies = ArrayDeque<Float>()
    private val times = ArrayDeque<Long>()
    private var total = 0L

    @Volatile var backendLabel: String = "—"
    @Volatile var npuActive: Boolean = false

    fun record(latencyMs: Float, nowMs: Long = System.currentTimeMillis()) = synchronized(lock) {
        latencies.addLast(latencyMs); times.addLast(nowMs); total++
        while (latencies.size > capacity) { latencies.removeFirst(); times.removeFirst() }
    }

    fun reset() = synchronized(lock) { latencies.clear(); times.clear(); total = 0 }

    fun snapshot(nowMs: Long = System.currentTimeMillis()): NpuUsageSnapshot = synchronized(lock) {
        val s = latencies.toList()
        val windowStart = nowMs - 1_000L
        NpuUsageSnapshot(
            backendLabel = backendLabel,
            npuActive = npuActive,
            samples = s,
            currentMs = s.lastOrNull() ?: 0f,
            avgMs = if (s.isEmpty()) 0f else (s.sum() / s.size),
            minMs = s.minOrNull() ?: 0f,
            maxMs = s.maxOrNull() ?: 0f,
            total = total,
            ratePerSec = times.count { it >= windowStart },
        )
    }
}
