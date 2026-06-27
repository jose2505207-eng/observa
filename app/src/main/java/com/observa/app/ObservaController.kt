package com.observa.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.observa.app.demo.DemoScript
import com.observa.app.hazard.FrameInput
import com.observa.app.hazard.Hazard
import com.observa.app.hazard.HazardEngine
import com.observa.app.hazard.Severity
import com.observa.app.output.Haptics
import com.observa.app.output.Speaker
import com.observa.app.runtime.ExecuTorchVisionRuntime
import com.observa.app.runtime.HeuristicVisionRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Single source of UI state and the wiring between the camera analyzer, the active
 * [com.observa.app.runtime.VisionRuntime], the [HazardEngine], and speech/haptic output.
 *
 * Threading: [submitFrame] runs on the camera executor and only touches @Volatile fields.
 * [runProcessingLoop] and [runDemo] run on the Compose main coroutine, where all Compose
 * state writes and alert emission happen.
 */
class ObservaController(context: Context) {

    private val speaker = Speaker(context)
    private val haptics = Haptics(context)
    private val engine = HazardEngine()
    private val heuristic = HeuristicVisionRuntime()
    private val executorch = ExecuTorchVisionRuntime()

    // --- Compose-observable UI state (written on main thread only) ---
    var cameraActive by mutableStateOf(false); private set
    var frameCount by mutableLongStateOf(0L); private set
    var fps by mutableStateOf(0f); private set
    var demoMode by mutableStateOf(false); private set
    var muted by mutableStateOf(false); private set
    var lastAlert by mutableStateOf("No alerts yet"); private set
    var cooldownNote by mutableStateOf("idle"); private set
    val alerts = mutableStateListOf<String>()

    val backendName: String get() = if (demoMode) "Demo (simulated)" else heuristic.name
    val backendStatus: String get() = if (demoMode) "scripted events" else heuristic.status.label
    val executorchStatus: String get() = "${executorch.name}: ${executorch.status.label}"
    val privacyLabel: String = "Local only · No network required"
    val ttsReady: Boolean get() = speaker.ready
    val hapticsAvailable: Boolean get() = haptics.available

    // --- Volatile metrics written by the analyzer thread ---
    @Volatile private var rawFrameCount = 0L
    @Volatile private var fpsValue = 0f
    @Volatile private var latestFrame: FrameInput? = null
    @Volatile private var lastFrameNs = 0L

    /** Called from the camera executor for every analyzed frame. */
    fun submitFrame(frame: FrameInput) {
        rawFrameCount++
        latestFrame = frame
        val now = System.nanoTime()
        if (lastFrameNs != 0L) {
            val deltaMs = (now - lastFrameNs) / 1_000_000f
            if (deltaMs > 0f) {
                val instant = 1000f / deltaMs
                fpsValue = if (fpsValue == 0f) instant else fpsValue * 0.8f + instant * 0.2f
            }
        }
        lastFrameNs = now
    }

    fun updateCameraActive(active: Boolean) {
        cameraActive = active
        if (!active) fpsValue = 0f
    }

    fun toggleMute() {
        muted = !muted
        speaker.muted = muted
        if (muted) speaker.stop()
    }

    /** Main-thread loop: refresh metrics and (when not in Demo Mode) run the live heuristic. */
    suspend fun runProcessingLoop() {
        while (coroutineContext.isActive) {
            frameCount = rawFrameCount
            fps = fpsValue
            cooldownNote = engine.lastDecision
            if (!demoMode) {
                val frame = latestFrame
                if (frame != null) {
                    val detections = heuristic.analyzeFrame(frame)
                    val hazards = engine.process(detections, System.currentTimeMillis())
                    hazards.forEach { emit(it) }
                }
            }
            delay(400)
        }
    }

    /** Play the deterministic demo sequence through the same engine/output. */
    suspend fun runDemo() {
        demoMode = true
        engine.reset()
        speaker.speak("Demo mode on.", urgent = true)
        val start = System.currentTimeMillis()
        var index = 0
        val seq = DemoScript.sequence
        while (coroutineContext.isActive && demoMode && index < seq.size) {
            val elapsed = System.currentTimeMillis() - start
            val event = seq[index]
            if (elapsed >= event.atMs) {
                if (engine.gate(event.hazard, System.currentTimeMillis())) emit(event.hazard)
                index++
            } else {
                delay(100)
            }
        }
        delay(2000)
        demoMode = false
        engine.reset()
    }

    fun stopDemo() {
        demoMode = false
        engine.reset()
        speaker.stop()
    }

    private fun emit(hazard: Hazard) {
        val tag = if (hazard.simulated) "[Demo] " else ""
        val line = tag + hazard.message
        lastAlert = line
        alerts.add(0, line)
        if (alerts.size > 6) alerts.removeAt(alerts.size - 1)
        speaker.speak(hazard.message, urgent = hazard.severity == Severity.HIGH)
        if (!muted) haptics.pulse(hazard.severity)
    }

    fun shutdown() {
        speaker.shutdown()
    }
}
