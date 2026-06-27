package com.observa.app.demo

import com.observa.app.hazard.Direction
import com.observa.app.hazard.Hazard
import com.observa.app.hazard.HazardType
import com.observa.app.hazard.Severity

/** One scripted, deterministic demo event at [atMs] after Demo Mode starts. */
data class DemoEvent(val atMs: Long, val hazard: Hazard)

/**
 * Deterministic, judge-friendly sequence. Every event is flagged simulated = true so the UI
 * labels it "Demo". These flow through the same HazardEngine + output as the live path.
 */
object DemoScript {
    val sequence: List<DemoEvent> = listOf(
        ev(2_000, HazardType.CLEAR_PATH, Direction.CENTER, Severity.LOW, "Camera active. Offline mode ready."),
        ev(5_000, HazardType.OBSTACLE_AHEAD, Direction.CENTER, Severity.HIGH, "Obstacle ahead, two steps."),
        ev(9_000, HazardType.TEXT_LEFT, Direction.LEFT, Severity.LOW, "Text detected on your left."),
        ev(13_000, HazardType.DOORWAY_LEFT, Direction.LEFT, Severity.LOW, "Doorway left."),
        ev(17_000, HazardType.CLEAR_PATH, Direction.CENTER, Severity.LOW, "Path clear."),
    )

    val durationMs: Long get() = (sequence.maxOfOrNull { it.atMs } ?: 0L) + 2_000L

    private fun ev(at: Long, type: HazardType, dir: Direction, sev: Severity, msg: String) =
        DemoEvent(at, Hazard(type, dir, sev, msg, timestampMs = at, simulated = true))
}
