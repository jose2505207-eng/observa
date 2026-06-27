package com.observa.app.service

/**
 * Decouples the foreground service's notification actions from the app's controller. MainActivity
 * sets [instance] to a live bridge while the app is running; the service invokes it for Stop / Mute
 * / Repeat. Kept tiny and process-local (no IPC, no data leaves the app).
 */
interface ServiceBridge {
    fun onStopRequested()
    fun onMuteRequested()
    fun onRepeatRequested()

    companion object {
        @Volatile
        var instance: ServiceBridge? = null
    }
}
