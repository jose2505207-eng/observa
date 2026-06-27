package com.observa.app.service

/** Notification action identifiers (broadcast actions handled by the service). */
object ServiceAction {
    const val STOP = "com.observa.app.action.STOP"
    const val MUTE = "com.observa.app.action.MUTE"
    const val REPEAT = "com.observa.app.action.REPEAT"
}

/** Pure builder for the persistent-notification text. Unit-testable; no Android deps. */
data class NotificationText(val title: String, val body: String, val actions: List<String>)

object NotificationContent {

    /** Always honest: states offline operation and the current run/degrade condition. */
    fun build(state: ServiceState): NotificationText {
        val title = "OBSERVA running offline. Ambient awareness active."
        val body = when (state.run) {
            RunState.STOPPED -> "Stopped."
            RunState.STARTING -> "Starting…"
            RunState.RUNNING -> "Observing. No network used."
            RunState.DEGRADED -> "Limited: " + reasonText(state.reason)
        }
        return NotificationText(title, body, listOf("Stop", "Mute speech", "Repeat last"))
    }

    fun reasonText(reason: DegradeReason): String = when (reason) {
        DegradeReason.NONE -> "running"
        DegradeReason.CAMERA_UNAVAILABLE -> "camera unavailable"
        DegradeReason.PERMISSION_REVOKED -> "camera permission needed"
        DegradeReason.LOW_BATTERY -> "low battery, reduced rate"
        DegradeReason.THERMAL -> "device hot, reduced rate"
    }
}
