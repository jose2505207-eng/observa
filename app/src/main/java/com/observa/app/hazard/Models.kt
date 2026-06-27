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

/**
 * Per-frame summary the analyzer hands to a runtime. The luminance summary is always present and
 * is all the heuristic needs (no pixels leave the analyzer for that path).
 *
 * [rgb] is an OPTIONAL, on-device-only, interleaved RGB float buffer (values 0..1, row-major,
 * length = rgbWidth * rgbHeight * 3). It is populated *only* when a real ML model is loaded and
 * needs pixels; otherwise it stays null and the heuristic path runs exactly as before. Pixels are
 * used in-process for inference and never stored or transmitted.
 */
data class FrameInput(
    val width: Int,
    val height: Int,
    val leftLuma: Float,
    val centerLuma: Float,
    val rightLuma: Float,
    val avgLuma: Float,
    val rgb: FloatArray? = null,
    val rgbWidth: Int = 0,
    val rgbHeight: Int = 0,
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
