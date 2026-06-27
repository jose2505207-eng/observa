package com.observa.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.observa.app.accessibility.AccessibilityOutputRouter
import com.observa.app.accessibility.OutputEvent
import com.observa.app.accessibility.OutputPriority
import com.observa.app.cue.AudioCuePlayer
import com.observa.app.cue.HapticCuePlayer
import com.observa.app.cue.SpatialCueEngine
import com.observa.app.demo.DemoScript
import com.observa.app.hazard.FrameInput
import com.observa.app.hazard.Hazard
import com.observa.app.hazard.HazardEngine
import com.observa.app.hazard.Severity
import com.observa.app.output.Speaker
import com.observa.app.runtime.ExecuTorchVisionRuntime
import com.observa.app.runtime.HeuristicVisionRuntime
import com.observa.app.voice.CommandActions
import com.observa.app.voice.CommandRouter
import com.observa.app.voice.OfflineSpeechRecognizer
import com.observa.app.voice.PushToTalkController
import com.observa.app.voice.VoiceCommandParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Single source of UI state and the wiring between the camera analyzer, the active
 * [com.observa.app.runtime.VisionRuntime], the [HazardEngine], speech/braille output, spatial
 * audio+haptic cues, and offline voice control.
 *
 * Threading: [submitFrame] runs on the camera executor and only touches @Volatile fields.
 * [runProcessingLoop] and [runDemo] run on the Compose main coroutine, where all Compose
 * state writes and alert emission happen. Voice components are created/used on the main thread.
 */
class ObservaController(context: Context) {

    private val speaker = Speaker(context)
    private val router = AccessibilityOutputRouter(speaker)
    private val spatialCue = SpatialCueEngine(AudioCuePlayer(), HapticCuePlayer(context))
    private val engine = HazardEngine()
    private val heuristic = HeuristicVisionRuntime()
    private val executorch = ExecuTorchVisionRuntime()

    // --- Voice (offline) ---
    private val recognizer = OfflineSpeechRecognizer(context)
    private val commandRouter = CommandRouter(VoiceCommandParser(), VoiceActions())
    private val pushToTalk = PushToTalkController(recognizer, commandRouter) { isListening, message ->
        listening = isListening
        voiceState = message
    }

    // --- Compose-observable UI state (written on main thread only) ---
    var cameraActive by mutableStateOf(false); private set
    var frameCount by mutableLongStateOf(0L); private set
    var fps by mutableStateOf(0f); private set
    var observing by mutableStateOf(true); private set
    var demoMode by mutableStateOf(false); private set
    var muted by mutableStateOf(false); private set
    var lastAlert by mutableStateOf("No alerts yet"); private set
    var cooldownNote by mutableStateOf("idle"); private set
    var voiceState by mutableStateOf("Voice idle"); private set
    var listening by mutableStateOf(false); private set
    val alerts = mutableStateListOf<String>()

    val brailleStatus: String get() = router.brailleStatus.text
    val backendName: String get() = if (demoMode) "Demo (simulated)" else heuristic.name
    val backendStatus: String get() = if (demoMode) "scripted events" else heuristic.status.label
    val executorchStatus: String get() = "${executorch.name}: ${executorch.status.label}"
    val privacyLabel: String = "Local only · No network required"
    val ttsReady: Boolean get() = speaker.ready
    val hapticsAvailable: Boolean get() = spatialCue.hapticsAvailable
    val voiceAvailable: Boolean get() = recognizer.available
    val voiceOnDevice: Boolean get() = recognizer.onDeviceOnly

    // --- Volatile metrics written by the analyzer thread ---
    @Volatile private var rawFrameCount = 0L
    @Volatile private var fpsValue = 0f
    @Volatile private var latestFrame: FrameInput? = null
    @Volatile private var lastFrameNs = 0L

    /** Initialize on-device speech recognition. Must be called on the main thread. */
    fun initVoice() = recognizer.init()

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

    fun setMute(value: Boolean) {
        muted = value
        router.setMuted(value)
    }

    fun toggleMute() = setMute(!muted)

    fun onMicPressed() = pushToTalk.onPressed()
    fun onMicReleased() = pushToTalk.onReleased()

    /** Main-thread loop: refresh metrics and (when observing and not in Demo Mode) run the heuristic. */
    suspend fun runProcessingLoop() {
        while (coroutineContext.isActive) {
            frameCount = rawFrameCount
            fps = fpsValue
            cooldownNote = engine.lastDecision
            if (!demoMode && observing) {
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
        respond("Demo mode on.")
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
        val urgent = hazard.severity == Severity.HIGH
        router.emit(
            OutputEvent(
                priority = if (urgent) OutputPriority.HAZARD else OutputPriority.NAVIGATION,
                speech = hazard.message,
                braille = line,
            )
        )
        // Non-speech spatial cues stay on even when speech is muted (safety).
        spatialCue.cue(hazard, System.currentTimeMillis())
    }

    /** On-demand spoken/braille response that bypasses the per-message throttle. */
    private fun respond(message: String) {
        voiceState = message
        router.emit(
            // urgent = true bypasses the throttle so explicit command responses always surface.
            OutputEvent(priority = OutputPriority.MODE, speech = message, braille = message, urgent = true)
        )
    }

    /** Honest brightness-based scene summary (no vision model yet — labeled as such). */
    private fun describeScene() {
        val f = latestFrame
        if (f == null) { respond("No camera frame yet."); return }
        val brightness = when {
            f.avgLuma < 40f -> "dark"
            f.avgLuma > 170f -> "bright"
            else -> "moderately lit"
        }
        val thirds = listOf("left" to f.leftLuma, "center" to f.centerLuma, "right" to f.rightLuma)
        val darkest = thirds.minByOrNull { it.second }!!.first
        respond("Brightness $brightness. Darkest region on your $darkest. No vision model loaded yet.")
    }

    private fun whatIsAhead() {
        val f = latestFrame
        if (f == null) { respond("No camera frame yet."); return }
        val msg = if (f.centerLuma < f.avgLuma * 0.62f)
            "Center is darker than the surroundings — possible obstacle ahead."
        else "Center looks clear."
        respond(msg)
    }

    private fun speakHelp() {
        respond(
            "Commands: start observing, stop observing, describe scene, what is ahead, " +
                "read text, repeat, mute, unmute, help."
        )
    }

    fun shutdown() {
        recognizer.destroy()
        spatialCue.release()
        speaker.shutdown()
    }

    /** Bridges parsed voice commands to controller behavior. Honest about what is not built yet. */
    private inner class VoiceActions : CommandActions {
        override fun start() { observing = true; respond("Observing on.") }
        override fun stop() { observing = false; respond("Observing off.") }
        override fun help() = speakHelp()
        override fun repeat() = router.repeatLast()
        override fun readText() = respond("Text reading is not available offline yet.")
        override fun describeScene() = this@ObservaController.describeScene()
        override fun navigateTo(destination: String) = respond("Navigation is not available yet.")
        override fun find(target: String) = respond("Object finding is not available yet.")
        override fun whereAmI() = respond("Location is not available offline yet.")
        override fun whatIsAhead() = this@ObservaController.whatIsAhead()
        override fun cancel() = respond("Cancelled.")
        override fun mute() { setMute(true); respond("Muted.") }
        override fun unmute() { setMute(false); respond("Speech on.") }
        override fun confirm(message: String) = respond(message)
    }
}
