package com.observa.app.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendDiagnosticsTest {

    @Test
    fun npuActive_onlyAfterQnnWarmupActive() {
        val d = BackendDiagnostics()
        d.record(BackendAttempt(BackendKind.EXECUTORCH_QNN, BackendStage.MODEL_LOAD, true))
        // Model loaded is NOT active — must not claim NPU yet.
        assertFalse(d.npuActive())
        d.record(BackendAttempt(BackendKind.EXECUTORCH_QNN, BackendStage.WARMUP_FORWARD, true))
        d.record(BackendAttempt(BackendKind.EXECUTORCH_QNN, BackendStage.ACTIVE, true))
        assertTrue(d.npuActive())
        assertEquals(BackendKind.EXECUTORCH_QNN, d.activeBackend())
    }

    @Test
    fun cpuActive_isNotNpu_andFallbackReasonHonest() {
        val d = BackendDiagnostics()
        d.record(BackendAttempt(BackendKind.EXECUTORCH_QNN, BackendStage.WARMUP_FORWARD, false,
            nativeHint = "skel 4000 / device_handle 14001", message = "Init failed for backend QnnBackend"))
        d.record(BackendAttempt(BackendKind.EXECUTORCH_QNN, BackendStage.FALLBACK, true))
        d.record(BackendAttempt(BackendKind.XNNPACK_CPU, BackendStage.ACTIVE, true))
        assertFalse(d.npuActive()) // XNNPACK active is CPU, never NPU
        assertEquals(BackendKind.XNNPACK_CPU, d.activeBackend())
        assertTrue(d.fallbackReason().contains("skel 4000"))
        assertTrue(d.summaryLine().contains("XNNPACK CPU"))
    }

    @Test
    fun firstFailure_isTheQnnWarmup() {
        val d = BackendDiagnostics()
        d.record(BackendAttempt(BackendKind.EXECUTORCH_QNN, BackendStage.MODEL_LOAD, true))
        d.record(BackendAttempt(BackendKind.EXECUTORCH_QNN, BackendStage.WARMUP_FORWARD, false, nativeHint = "skel 4000"))
        assertEquals(BackendStage.WARMUP_FORWARD, d.firstFailure()?.stage)
    }

    @Test
    fun nativeHint_extractsSkelAndDeviceHandle() {
        val hint = BackendDiagnostics.nativeHintFrom(
            "QnnDsp Failed to load skel, error: 4000 ... Failed to create device_handle for Backend ID 6, error=14001")
        assertTrue(hint.contains("skel 4000"))
        assertTrue(hint.contains("device_handle 14001") || hint.contains("14001"))
    }

    @Test
    fun report_includesAttemptsAndPriority() {
        val d = BackendDiagnostics()
        d.record(BackendAttempt(BackendKind.EXECUTORCH_QNN, BackendStage.WARMUP_FORWARD, false, nativeHint = "skel 4000"))
        val r = d.report("device header")
        assertTrue(r.contains("NPU active: false"))
        assertTrue(r.contains("EXECUTORCH_QNN"))
        assertTrue(r.contains("Priority"))
    }

    @Test
    fun report_namesNativeBlocker_whenQnnLoadsButDeviceInitFails() {
        val d = BackendDiagnostics()
        // QNN .pte loads, then the on-device warm-up forward fails — the device-side QNN/HTP path.
        d.record(BackendAttempt(BackendKind.EXECUTORCH_QNN, BackendStage.MODEL_LOAD, true, message = "backends=[QnnBackend]"))
        d.record(BackendAttempt(BackendKind.EXECUTORCH_QNN, BackendStage.WARMUP_FORWARD, false))
        d.record(BackendAttempt(BackendKind.XNNPACK_CPU, BackendStage.ACTIVE, true))
        assertTrue(d.qnnLoadedButDeviceInitFailed())
        val r = d.report("hdr")
        // The copyable report must point judges/Qualcomm at the native QnnDsp error + the adb command.
        assertTrue(r.contains("adb logcat | grep -i QnnDsp"))
        assertTrue(r.contains("error 4000"))
        assertTrue(r.contains("14001"))
        assertFalse(d.npuActive())
    }

    @Test
    fun report_noNativeBlocker_whenQnnNeverLoaded() {
        val d = BackendDiagnostics()
        d.record(BackendAttempt(BackendKind.EXECUTORCH_QNN, BackendStage.ASSET_CHECK, false, message = "not bundled"))
        d.record(BackendAttempt(BackendKind.XNNPACK_CPU, BackendStage.ACTIVE, true))
        assertFalse(d.qnnLoadedButDeviceInitFailed())
    }
}
