package com.observa.app.navigation

/**
 * The single rule that vision hazard alerts always beat navigation guidance. Output priority already
 * routes hazards above NAVIGATION, but a hazard and an orientation update can land in the same moment;
 * this arbiter additionally *suppresses* orientation guidance for a short window after a hazard so the
 * user is never talked over the thing that matters for safety. Pure; unit-testable.
 */
class NavigationSafetyArbiter(private val hazardHoldMs: Long = 2_500L) {

    /** True only when enough time has passed since the last hazard that guidance may speak. */
    fun guidanceAllowed(nowMs: Long, lastHazardMs: Long): Boolean =
        lastHazardMs <= 0L || nowMs - lastHazardMs >= hazardHoldMs
}
