package com.observa.app.runtime

import android.content.Context
import com.observa.app.hazard.Detection
import com.observa.app.hazard.FrameInput

/** Truthful state of the ML inference path. Drives the UI/Braille "AI model" row verbatim. */
enum class InferenceStatus(val label: String) {
    UNAVAILABLE("unavailable — heuristic fallback"),
    LOADED_CPU("loaded — CPU"),
    LOADED_QNN("loaded — QNN active"),
    FAILED("failed — heuristic fallback"),
}

/**
 * Contract for a real on-device model. Kept separate from [VisionRuntime] because it has a
 * lifecycle ([initialize]/[close]) and a richer [InferenceStatus]. Implementations MUST fall back
 * honestly: never claim a model is loaded, never claim QNN acceleration, and never emit object
 * labels unless those things are actually true.
 */
interface InferenceEngine {
    val status: InferenceStatus
    fun initialize(context: Context): InferenceStatus
    suspend fun analyzeFrame(frame: FrameInput): List<Detection>
    fun close()
}
