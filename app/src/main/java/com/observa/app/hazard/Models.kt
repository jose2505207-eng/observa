package com.observa.app.hazard

/** Kinds of scene events OBSERVA can surface to the user. */
enum class HazardType {
    OBSTACLE_AHEAD,
    PERSON_APPROACHING,
    DOORWAY_LEFT,
    STAIRS_DETECTED,
    TEXT_LEFT,
    CLEAR_PATH,
}

/** Rough horizontal direction of a hazard relative to the user. */
enum class Direction { LEFT, CENTER, RIGHT, UNKNOWN }

/** How urgent an alert is; drives speech priority and haptic strength. */
enum class Severity { LOW, MEDIUM, HIGH }

/**
 * A raw detection produced by a [com.observa.app.runtime.VisionRuntime].
 * Generic on purpose so real ML output and the demo/heuristic paths share one contract.
 */
data class Detection(
    val label: String,
    val confidence: Float,
    val direction: Direction = Direction.UNKNOWN,
)

/** Per-frame summary the analyzer hands to a runtime (no raw pixels leave the analyzer). */
data class FrameInput(
    val width: Int,
    val height: Int,
    val leftLuma: Float,
    val centerLuma: Float,
    val rightLuma: Float,
    val avgLuma: Float,
)

/** A user-facing alert after the [HazardEngine] has mapped and gated detections. */
data class Hazard(
    val type: HazardType,
    val direction: Direction,
    val severity: Severity,
    val message: String,
    val timestampMs: Long,
    /** True when this came from Demo Mode (simulated), so the UI can label it honestly. */
    val simulated: Boolean = false,
)
