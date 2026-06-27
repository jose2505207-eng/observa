package com.observa.app.service

/**
 * Pure, deterministic state machine for the ambient-awareness service. No Android dependencies, so
 * every transition is unit-testable. Priority of degrade reasons (highest first):
 * PERMISSION_REVOKED > CAMERA_UNAVAILABLE > THERMAL(critical) > LOW_BATTERY. Safety bias: the
 * service stays in DEGRADED (still trying) rather than silently STOPPED whenever it can run at all.
 */
object ServiceStateReducer {

    const val LOW_BATTERY_PERCENT = 15

    fun reduce(state: ServiceState, event: ServiceEvent): ServiceState {
        val s = apply(state, event)
        // Recompute run/reason from the resulting conditions (except when fully stopped).
        return if (s.run == RunState.STOPPED) s else resolve(s)
    }

    private fun apply(s: ServiceState, e: ServiceEvent): ServiceState = when (e) {
        is ServiceEvent.Start -> s.copy(run = RunState.STARTING, reason = DegradeReason.NONE)
        is ServiceEvent.Stop -> s.copy(run = RunState.STOPPED, reason = DegradeReason.NONE)
        is ServiceEvent.ScreenOn -> s.copy(screenOn = e.on)
        is ServiceEvent.CameraAvailable -> s.copy(cameraAvailable = e.available)
        is ServiceEvent.PermissionRevoked -> s.copy(cameraAvailable = false, reason = DegradeReason.PERMISSION_REVOKED)
        is ServiceEvent.Battery -> s.copy(batteryPercent = e.percent, charging = e.charging)
        is ServiceEvent.Thermal -> s.copy(thermal = e.level)
    }

    /** Derive run/reason from current conditions for a non-stopped service. */
    private fun resolve(s: ServiceState): ServiceState {
        val reason = when {
            s.reason == DegradeReason.PERMISSION_REVOKED -> DegradeReason.PERMISSION_REVOKED
            !s.cameraAvailable -> DegradeReason.CAMERA_UNAVAILABLE
            s.thermal == ThermalLevel.CRITICAL -> DegradeReason.THERMAL
            s.batteryPercent <= LOW_BATTERY_PERCENT && !s.charging -> DegradeReason.LOW_BATTERY
            else -> DegradeReason.NONE
        }
        val run = if (reason == DegradeReason.NONE) RunState.RUNNING else RunState.DEGRADED
        return s.copy(run = run, reason = reason)
    }
}
