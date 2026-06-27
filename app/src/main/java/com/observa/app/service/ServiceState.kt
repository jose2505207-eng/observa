package com.observa.app.service

/** Whether ambient awareness is actively running, paused, degraded, or stopped. */
enum class RunState { STOPPED, STARTING, RUNNING, DEGRADED }

/** Why the service is degraded or stopped (drives honest user-facing copy). */
enum class DegradeReason { NONE, CAMERA_UNAVAILABLE, PERMISSION_REVOKED, LOW_BATTERY, THERMAL }

/** Coarse thermal pressure, abstracted from Android's PowerManager so logic stays pure/testable. */
enum class ThermalLevel { NORMAL, WARNING, CRITICAL }

/**
 * Immutable snapshot of the ambient-awareness service. Pure data; the reducer
 * ([ServiceStateReducer]) is the only thing that produces new instances, so transitions are
 * deterministic and unit-testable.
 */
data class ServiceState(
    val run: RunState = RunState.STOPPED,
    val reason: DegradeReason = DegradeReason.NONE,
    val cameraAvailable: Boolean = true,
    val screenOn: Boolean = true,
    val batteryPercent: Int = 100,
    val charging: Boolean = false,
    val thermal: ThermalLevel = ThermalLevel.NORMAL,
) {
    /** True when the pipeline should actively run inference/cues. */
    val active: Boolean get() = run == RunState.RUNNING || run == RunState.DEGRADED
}

/** Events the service reacts to. Pure inputs to [ServiceStateReducer]. */
sealed interface ServiceEvent {
    data object Start : ServiceEvent
    data object Stop : ServiceEvent
    data class ScreenOn(val on: Boolean) : ServiceEvent
    data class CameraAvailable(val available: Boolean) : ServiceEvent
    data object PermissionRevoked : ServiceEvent
    data class Battery(val percent: Int, val charging: Boolean) : ServiceEvent
    data class Thermal(val level: ThermalLevel) : ServiceEvent
}
