package com.observa.app.service

/**
 * Adaptive duty-cycle policy. Pure logic: maps battery + thermal state to an analysis interval and
 * cue verbosity so OBSERVA backs off under heat/low battery without going silent on safety. A
 * higher [analysisIntervalMs] means the heuristic/model runs less often (lower power/heat).
 */
data class DutyCycle(
    val analysisIntervalMs: Long,
    /** When true, suppress low-priority chatter (keep HAZARD); reduces cue spam under stress. */
    val reduceCues: Boolean,
    /** Short, honest status for the diagnostics/notification, e.g. "thermal: reduced rate". */
    val statusLabel: String,
)

object BatteryThermalPolicy {

    const val NORMAL_INTERVAL_MS = 400L   // matches the existing processing loop cadence
    const val REDUCED_INTERVAL_MS = 800L
    const val MINIMAL_INTERVAL_MS = 1_500L

    fun dutyCycle(state: ServiceState): DutyCycle = when {
        state.thermal == ThermalLevel.CRITICAL ->
            DutyCycle(MINIMAL_INTERVAL_MS, reduceCues = true, statusLabel = "thermal critical: minimal rate")
        state.thermal == ThermalLevel.WARNING ->
            DutyCycle(REDUCED_INTERVAL_MS, reduceCues = true, statusLabel = "thermal warning: reduced rate")
        state.batteryPercent <= ServiceStateReducer.LOW_BATTERY_PERCENT && !state.charging ->
            DutyCycle(REDUCED_INTERVAL_MS, reduceCues = true, statusLabel = "low battery: reduced rate")
        else ->
            DutyCycle(NORMAL_INTERVAL_MS, reduceCues = false, statusLabel = "normal rate")
    }
}
