package com.observa.app.cue

import com.observa.app.hazard.Hazard
import com.observa.app.hazard.Severity

/** Picks the single most important hazard to act on this cycle. Pure logic; unit-testable. */
class HazardPriorityResolver {

    fun weight(severity: Severity): Int = when (severity) {
        Severity.HIGH -> 3
        Severity.MEDIUM -> 2
        Severity.LOW -> 1
    }

    /** Highest severity wins; ties broken by most recent. Returns null for an empty list. */
    fun resolve(hazards: List<Hazard>): Hazard? =
        hazards.maxWithOrNull(compareBy({ weight(it.severity) }, { it.timestampMs }))
}
