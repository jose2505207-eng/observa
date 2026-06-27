package com.observa.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Foreground service that gives OBSERVA a reliable, privacy-forward presence while ambient
 * awareness is active. It posts a persistent, honest notification ("running offline") with
 * accessible Stop / Mute / Repeat actions routed to the app via [ServiceBridge].
 *
 * Honesty note: camera capture + inference currently run in the Activity/`ObservaController`; this
 * service keeps the app foregrounded and surfaces controls/status. True screen-off background
 * camera capture is a documented future item (see docs/manual-test-foreground-service.md). The
 * service holds no network capability and moves no data off-device.
 */
class AmbientAwarenessService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ServiceAction.STOP -> { ServiceBridge.instance?.onStopRequested(); stopSelf(); return START_NOT_STICKY }
            ServiceAction.MUTE -> ServiceBridge.instance?.onMuteRequested()
            ServiceAction.REPEAT -> ServiceBridge.instance?.onRepeatRequested()
            else -> {}
        }
        startForegroundHonestly(ServiceState(run = RunState.RUNNING))
        return START_STICKY
    }

    /** Re-post the notification reflecting [state] (called on meaningful state changes). */
    fun update(state: ServiceState) = startForegroundHonestly(state)

    private fun startForegroundHonestly(state: ServiceState) {
        ensureChannel()
        val n = buildNotification(state)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
            } else {
                startForeground(NOTIF_ID, n)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
        }
    }

    private fun buildNotification(state: ServiceState): Notification {
        val text = NotificationContent.build(state)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Stop", actionIntent(ServiceAction.STOP))
            .addAction(0, "Mute speech", actionIntent(ServiceAction.MUTE))
            .addAction(0, "Repeat last", actionIntent(ServiceAction.REPEAT))
            .build()
    }

    private fun actionIntent(action: String): PendingIntent {
        val intent = Intent(this, AmbientAwarenessService::class.java).setAction(action)
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Ambient awareness", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "OBSERVA offline ambient awareness status."
                    setShowBadge(false)
                },
            )
        }
    }

    companion object {
        private const val TAG = "OBSERVA_SERVICE"
        private const val CHANNEL_ID = "observa_ambient"
        private const val NOTIF_ID = 1001

        fun start(context: Context) {
            val i = Intent(context, AmbientAwarenessService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
            else context.startService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AmbientAwarenessService::class.java))
        }
    }
}
