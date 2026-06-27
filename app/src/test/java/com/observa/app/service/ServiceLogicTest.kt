package com.observa.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the pure foreground-service logic: reducer, policy, notification, permissions. */
class ServiceLogicTest {

    private val running = ServiceStateReducer.reduce(ServiceState(), ServiceEvent.Start)

    // --- reducer transitions ---

    @Test fun startThenStop() {
        assertEquals(RunState.RUNNING, running.run)
        val stopped = ServiceStateReducer.reduce(running, ServiceEvent.Stop)
        assertEquals(RunState.STOPPED, stopped.run)
    }

    @Test fun cameraLossDegradesNotStops() {
        val s = ServiceStateReducer.reduce(running, ServiceEvent.CameraAvailable(false))
        assertEquals(RunState.DEGRADED, s.run)
        assertEquals(DegradeReason.CAMERA_UNAVAILABLE, s.reason)
        assertTrue("degraded still counts as active (keeps trying)", s.active)
    }

    @Test fun permissionRevokedIsHighestPriority() {
        var s = ServiceStateReducer.reduce(running, ServiceEvent.Thermal(ThermalLevel.CRITICAL))
        s = ServiceStateReducer.reduce(s, ServiceEvent.PermissionRevoked)
        assertEquals(DegradeReason.PERMISSION_REVOKED, s.reason)
    }

    @Test fun lowBatteryWhileDischargingDegrades_butChargingDoesNot() {
        val low = ServiceStateReducer.reduce(running, ServiceEvent.Battery(10, charging = false))
        assertEquals(DegradeReason.LOW_BATTERY, low.reason)
        val charging = ServiceStateReducer.reduce(running, ServiceEvent.Battery(10, charging = true))
        assertEquals(DegradeReason.NONE, charging.reason)
        assertEquals(RunState.RUNNING, charging.run)
    }

    @Test fun recoveryReturnsToRunning() {
        var s = ServiceStateReducer.reduce(running, ServiceEvent.CameraAvailable(false))
        assertEquals(RunState.DEGRADED, s.run)
        s = ServiceStateReducer.reduce(s, ServiceEvent.CameraAvailable(true))
        assertEquals(RunState.RUNNING, s.run)
        assertEquals(DegradeReason.NONE, s.reason)
    }

    @Test fun screenOffDoesNotStop() {
        val s = ServiceStateReducer.reduce(running, ServiceEvent.ScreenOn(false))
        assertFalse(s.screenOn)
        assertEquals(RunState.RUNNING, s.run)
    }

    // --- duty-cycle policy ---

    @Test fun normalStateUsesNormalInterval() {
        val d = BatteryThermalPolicy.dutyCycle(ServiceState())
        assertEquals(BatteryThermalPolicy.NORMAL_INTERVAL_MS, d.analysisIntervalMs)
        assertFalse(d.reduceCues)
    }

    @Test fun thermalCriticalUsesMinimalRateAndReducesCues() {
        val d = BatteryThermalPolicy.dutyCycle(ServiceState(thermal = ThermalLevel.CRITICAL))
        assertEquals(BatteryThermalPolicy.MINIMAL_INTERVAL_MS, d.analysisIntervalMs)
        assertTrue(d.reduceCues)
    }

    @Test fun lowBatteryReducesRate() {
        val d = BatteryThermalPolicy.dutyCycle(ServiceState(batteryPercent = 10, charging = false))
        assertEquals(BatteryThermalPolicy.REDUCED_INTERVAL_MS, d.analysisIntervalMs)
        assertTrue(d.reduceCues)
    }

    // --- notification content (honest) ---

    @Test fun notificationStatesOfflineAndActions() {
        val n = NotificationContent.build(running)
        assertTrue(n.title.contains("offline", ignoreCase = true))
        assertEquals(listOf("Stop", "Mute speech", "Repeat last"), n.actions)
    }

    @Test fun degradedNotificationExplainsReasonHonestly() {
        val degraded = ServiceStateReducer.reduce(running, ServiceEvent.CameraAvailable(false))
        val n = NotificationContent.build(degraded)
        assertTrue(n.body.contains("camera", ignoreCase = true))
    }

    // --- permission education ---

    @Test fun permissionCopyIsHonestAboutNoNetwork() {
        assertTrue(PermissionEducation.network.contains("airplane mode", ignoreCase = true))
        assertTrue(PermissionEducation.camera.contains("never uploaded", ignoreCase = true))
        assertEquals(4, PermissionEducation.all().size)
    }
}
