package com.observa.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.graphics.Bitmap
import com.observa.app.accessibility.AccessibilityOutputRouter
import com.observa.app.accessibility.BrailleSnapshot
import com.observa.app.accessibility.BrailleStatusPresenter
import com.observa.app.accessibility.CueDirection
import com.observa.app.accessibility.OutputEvent
import com.observa.app.accessibility.OutputPriority
import com.observa.app.ocr.OcrEngine
import com.observa.app.ocr.MlKitOcrEngine
import com.observa.app.cue.AudioCuePlayer
import com.observa.app.cue.HapticCuePlayer
import com.observa.app.cue.SpatialCueEngine
import com.observa.app.demo.DemoScript
import com.observa.app.hazard.Direction
import com.observa.app.hazard.FrameInput
import com.observa.app.hazard.Hazard
import com.observa.app.hazard.HazardEngine
import com.observa.app.hazard.Severity
import com.observa.app.output.Speaker
import com.observa.app.runtime.ExecuTorchDetector
import com.observa.app.runtime.HeuristicVisionRuntime
import com.observa.app.runtime.InferenceStatus
import com.observa.app.voice.CommandActions
import com.observa.app.voice.CommandRouter
import com.observa.app.voice.OfflineSpeechRecognizer
import com.observa.app.voice.PushToTalkController
import com.observa.app.voice.VoiceCommandParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val spatialCue = SpatialCueEngine(AudioCuePlayer(), HapticCuePlayer(context))
    private val router = AccessibilityOutputRouter(speaker, cueSink = spatialCue)
    private val engine = HazardEngine()
    private val heuristic = HeuristicVisionRuntime()
    private val executorch = ExecuTorchDetector()
    private val ocr: OcrEngine = MlKitOcrEngine()
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    @Volatile private var modelInitialized = false

    // --- OCR on-demand capture (one-shot bitmap handed by the analyzer) ---
    @Volatile private var ocrCapturePending = false
    @Volatile private var pendingOcrBitmap: Bitmap? = null
    val wantsOcrCapture: Boolean get() = ocrCapturePending
    val ocrAvailable: Boolean get() = ocr.ready

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
    var brailleEnabled by mutableStateOf(true); private set
    var audioCuesEnabled by mutableStateOf(true); private set
    var hapticCuesEnabled by mutableStateOf(true); private set
    val alerts = mutableStateListOf<String>()

    val brailleStatus: String get() = router.brailleStatus.text
    val backendName: String get() = if (demoMode) "Demo (simulated)" else heuristic.name
    val backendStatus: String get() = if (demoMode) "scripted events" else heuristic.status.label
    val aiModelStatus: String get() = "AI model: ${executorch.status.label}"
    val aiModelDetail: String get() = executorch.detail

    /** Live inference diagnostics for the dashboard (input/output shapes, latency, QNN). */
    val aiDiagnostics: String
        get() = if (modelNeedsPixels)
            "in ${executorch.inputShape} · out ${executorch.lastOutputShapes} · " +
                "${executorch.lastLatencyMs}ms (avg ${executorch.avgLatencyMs}ms) · " +
                "QNN ${if (executorch.qnnActive) "active" else "off"}"
        else executorch.detail

    /** True only when a real model is loaded; gates pixel capture in the analyzer. */
    val modelNeedsPixels: Boolean
        get() = executorch.status == InferenceStatus.LOADED_CPU ||
            executorch.status == InferenceStatus.LOADED_QNN
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
        updateBrailleStatus()
    }

    fun toggleMute() = setMute(!muted)

    /** Single source of truth for the observing state — shared by the UI toggle and voice. */
    fun observe(value: Boolean) {
        observing = value
        respond(if (value) "Observing on." else "Observing off.")
        updateBrailleStatus()
    }

    fun toggleObserving() = observe(!observing)

    /** Single source of truth for Braille output — shared by the UI toggle and voice. */
    fun setBraille(value: Boolean) {
        brailleEnabled = value
        router.brailleEnabled = value
        if (value) updateBrailleStatus() else router.forceStatusLine("Braille off.")
        respond(if (value) "Braille on." else "Braille off.")
    }

    fun toggleBraille() = setBraille(!brailleEnabled)

    /** Audio cue on/off (safety cues remain independent of speech mute). */
    fun setAudioCues(value: Boolean) {
        audioCuesEnabled = value
        spatialCue.audioEnabled = value
        respond(if (value) "Audio cues on." else "Audio cues off.")
    }

    /** Directional haptic on/off. */
    fun setHapticCues(value: Boolean) {
        hapticCuesEnabled = value
        spatialCue.hapticsEnabled = value
        respond(if (value) "Haptics on." else "Haptics off.")
    }

    /** Speak + show the current concise Braille status line (the "braille status" command). */
    fun announceBrailleStatus() = respond(currentBrailleLine())

    /**
     * Capture one camera frame and run offline OCR on demand. Declines honestly if OCR is not
     * available, says "No readable text found." when appropriate, and never runs continuously.
     */
    fun readText() {
        if (!ocr.ready) { respond("Text reading is unavailable."); return }
        respond("Reading text…")
        pendingOcrBitmap = null
        ocrCapturePending = true
        scope.launch {
            val bmp = awaitOcrBitmap()
            ocrCapturePending = false
            if (bmp == null) { respond("Could not capture an image."); return@launch }
            val result = withContext(Dispatchers.Default) { ocr.recognize(bmp) }
            bmp.recycle()
            emitOcr(result.message)
        }
    }

    /** Called from the analyzer thread with a one-shot bitmap when [wantsOcrCapture] is set. */
    fun submitOcrFrame(bitmap: Bitmap) {
        pendingOcrBitmap = bitmap
        ocrCapturePending = false
    }

    private suspend fun awaitOcrBitmap(): Bitmap? {
        repeat(30) { // up to ~1.5s for a fresh frame
            pendingOcrBitmap?.let { return it }
            delay(50)
        }
        return pendingOcrBitmap
    }

    private fun emitOcr(message: String) {
        voiceState = message
        router.emit(
            OutputEvent(
                priority = OutputPriority.OCR,
                speech = message,
                braille = if (message.length > 60) message.take(57) + "…" else message,
                urgent = false,
            )
        )
    }

    private fun currentBrailleLine(): String =
        BrailleStatusPresenter.format(BrailleSnapshot(observing, muted, executorch.status.label))

    /** Refresh the ambient composite Braille/live-region line from current state. */
    private fun updateBrailleStatus() = router.setStatusLine(currentBrailleLine())

    fun onMicPressed() = pushToTalk.onPressed()
    fun onMicReleased() = pushToTalk.onReleased()

    /**
     * Main-thread loop: initialize the model once (off the UI thread), refresh metrics, and (when
     * observing and not in Demo Mode) run the active detector — the ExecuTorch model if it loaded,
     * otherwise the heuristic fallback. Inference runs off the UI thread.
     */
    suspend fun runProcessingLoop() {
        if (!modelInitialized) {
            withContext(Dispatchers.IO) { executorch.initialize(appContext) }
            modelInitialized = true
            updateBrailleStatus()
        }
        while (coroutineContext.isActive) {
            frameCount = rawFrameCount
            fps = fpsValue
            cooldownNote = engine.lastDecision
            if (!demoMode && observing) {
                val frame = latestFrame
                if (frame != null) {
                    val detections = if (modelNeedsPixels)
                        withContext(Dispatchers.Default) { executorch.analyzeFrame(frame) }
                    else heuristic.analyzeFrame(frame)
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
        // The router fans this to speech + Braille + the audio/haptic cue sink (cues fire even when
        // speech is muted, for safety). Direction steers panning/directional haptics.
        router.emit(
            OutputEvent(
                priority = if (urgent) OutputPriority.HAZARD else OutputPriority.NAVIGATION,
                speech = hazard.message,
                braille = line,
                direction = cueDirectionOf(hazard.direction),
                timestampMs = hazard.timestampMs,
            )
        )
    }

    private fun cueDirectionOf(d: Direction): CueDirection = when (d) {
        Direction.LEFT -> CueDirection.LEFT
        Direction.RIGHT -> CueDirection.RIGHT
        Direction.CENTER -> CueDirection.CENTER
        Direction.UNKNOWN -> CueDirection.NONE
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
        scope.cancel()
        recognizer.destroy()
        spatialCue.release()
        executorch.close()
        ocr.close()
        speaker.shutdown()
    }

    /** Bridges parsed voice commands to controller behavior. Honest about what is not built yet. */
    private inner class VoiceActions : CommandActions {
        override fun start() = observe(true)
        override fun stop() = observe(false)
        override fun help() = speakHelp()
        override fun repeat() = router.repeatLast()
        override fun readText() = this@ObservaController.readText()
        override fun describeScene() = this@ObservaController.describeScene()
        override fun navigateTo(destination: String) = respond("Navigation is not available yet.")
        override fun find(target: String) = respond("Object finding is not available yet.")
        override fun whereAmI() = respond("Location is not available offline yet.")
        override fun whatIsAhead() = this@ObservaController.whatIsAhead()
        override fun cancel() = respond("Cancelled.")
        override fun mute() { setMute(true); respond("Muted.") }
        override fun unmute() { setMute(false); respond("Speech on.") }
        override fun brailleOn() = setBraille(true)
        override fun brailleOff() = setBraille(false)
        override fun brailleStatus() = announceBrailleStatus()
        override fun confirm(message: String) = respond(message)
    }
}
