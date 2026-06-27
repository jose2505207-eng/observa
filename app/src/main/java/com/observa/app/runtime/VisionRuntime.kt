package com.observa.app.runtime

import com.observa.app.hazard.Detection
import com.observa.app.hazard.Direction
import com.observa.app.hazard.FrameInput

/** Truthful runtime state shown verbatim in the dashboard. Never overstate acceleration. */
enum class RuntimeStatus(val label: String) {
    ACTIVE("active"),
    FALLBACK("fallback (CPU)"),
    BUNDLED_NOT_INVOKED("bundled, not invoked"),
    NOT_CONNECTED("not connected"),
}

/**
 * One contract for every detection source: the brightness heuristic, the demo script, and a
 * future real ExecuTorch path. The UI reports [name]/[status] exactly as given here.
 */
interface VisionRuntime {
    val name: String
    val status: RuntimeStatus
    suspend fun analyzeFrame(frame: FrameInput): List<Detection>
}

/**
 * A real, on-device heuristic: compares the brightness of the left/center/right thirds of the
 * frame. A markedly darker region is treated as a possible near obstacle in that direction.
 * This is a deterministic proxy, NOT machine-learning detection — labeled as such in the UI.
 */
class HeuristicVisionRuntime(
    private val darkRatio: Float = 0.62f,
    private val minAvgLuma: Float = 8f,
) : VisionRuntime {
    override val name = "Heuristic (brightness)"
    override val status = RuntimeStatus.ACTIVE

    override suspend fun analyzeFrame(frame: FrameInput): List<Detection> {
        if (frame.avgLuma < minAvgLuma) return emptyList() // too dark to judge (e.g. covered)
        val out = ArrayList<Detection>()
        val thirds = listOf(
            Direction.LEFT to frame.leftLuma,
            Direction.CENTER to frame.centerLuma,
            Direction.RIGHT to frame.rightLuma,
        )
        // Darkest third, if clearly darker than the frame average, reads as a possible obstacle.
        val darkest = thirds.minByOrNull { it.second } ?: return emptyList()
        if (darkest.second < frame.avgLuma * darkRatio) {
            val conf = (1f - darkest.second / (frame.avgLuma + 1f)).coerceIn(0f, 1f)
            out.add(Detection(label = "OBSTACLE", confidence = conf, direction = darkest.first))
        }
        return out
    }
}

/**
 * Truthful stub for ExecuTorch. The AAR is bundled (app/libs/executorch.aar) but no model is
 * loaded or run yet, so this reports [RuntimeStatus.BUNDLED_NOT_INVOKED] and returns nothing.
 * It exists so the architecture (and the dashboard) is honest and ready for a real path.
 */
class ExecuTorchVisionRuntime : VisionRuntime {
    override val name = "ExecuTorch"
    override val status = RuntimeStatus.BUNDLED_NOT_INVOKED
    override suspend fun analyzeFrame(frame: FrameInput): List<Detection> = emptyList()
}
