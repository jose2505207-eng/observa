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
import com.observa.app.hotkey.HotkeyCommand
import com.observa.app.ocr.OcrEngine
import com.observa.app.ocr.MlKitOcrEngine
import com.observa.app.nav.DestinationStore
import com.observa.app.nav.DirectionalLockHaptics
import com.observa.app.nav.Geo
import com.observa.app.nav.HapticGuidanceMode
import com.observa.app.nav.HeadingAccuracy
import com.observa.app.nav.LockState
import com.observa.app.nav.NavigationSession
import com.observa.app.nav.SensorNavFixProvider
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
import com.observa.app.service.BatteryThermalPolicy
import com.observa.app.service.ServiceState
import com.observa.app.service.ThermalLevel
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
    private val destinations = DestinationStore()
    private val navSession = NavigationSession()
    private val navFixProvider = SensorNavFixProvider(context)
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
    var serviceStatus by mutableStateOf("normal rate"); private set
    var hapticMode by mutableStateOf(HapticGuidanceMode.NAVIGATION_LOCK_ON); private set
    var hotkeysEnabled by mutableStateOf(false); private set
    val alerts = mutableStateListOf<String>()

    // --- Lock-on haptic guidance timing/state ---
    @Volatile private var lastHazardMs = 0L
    private var lastLockTickMs = 0L
    private var lastLockState: LockState? = null

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

    /** Replay the last meaningful message (the foreground-notification "Repeat last" action). */
    fun repeatLast() = router.repeatLast()

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
    fun readText() = runOcr("Reading text…") { result -> emitOcr(result.message) }

    /**
     * Honest find-exit: runs one OCR pass and, only if the camera actually reads "exit", guides
     * toward it. Never hallucinates an exit. (Door/exit-sign detection would require a real model;
     * a mapped-exit path would require a map pack — both documented as future.)
     */
    fun findExit() = runOcr("Looking for an exit…") { result ->
        val sawExit = result.found && result.text.contains("exit", ignoreCase = true)
        emitOcr(
            if (sawExit) "Exit text seen ahead. Scan slowly toward it."
            else "Exit not found. Try scanning slowly or use navigation.",
        )
    }

    /** Shared one-shot OCR capture used by Read Text and Find Exit. */
    private fun runOcr(announce: String, onResult: (com.observa.app.ocr.OcrResult) -> Unit) {
        if (!ocr.ready) { respond("Text reading is unavailable."); return }
        respond(announce)
        pendingOcrBitmap = null
        ocrCapturePending = true
        scope.launch {
            val bmp = awaitOcrBitmap()
            ocrCapturePending = false
            if (bmp == null) { respond("Could not capture an image."); return@launch }
            val result = withContext(Dispatchers.Default) { ocr.recognize(bmp) }
            bmp.recycle()
            onResult(result)
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

    // --- Offline navigation (guidance-first; hazards always override via router priority) ---

    val navigating: Boolean get() = navSession.active
    val savedDestinations: List<String> get() = destinations.all().map { it.name }

    fun startNavigation(name: String) {
        val dest = destinations.find(name)
        if (dest == null) { respond("Destination $name not found. Try a saved place."); return }
        navFixProvider.start()
        navSession.start(dest)
        respond("Navigating to ${dest.name}. Source: ${navFixProvider.sourceLabel}.")
    }

    fun stopNavigation() {
        if (!navSession.active) { respond("Not navigating."); return }
        navSession.stop()
        navFixProvider.stop()
        respond("Navigation stopped.")
    }

    fun whereAmI() {
        val dest = navSession.destination
        if (dest == null) { respond("Not navigating. Say navigate to, then a saved place."); return }
        val g = com.observa.app.nav.GuidanceEngine().guide(navFixProvider.current(), dest)
        respond(g.speech)
    }

    /** Emit one navigation guidance utterance if due. NAVIGATION priority → a hazard interrupts it. */
    private fun navigationTick() {
        if (!navSession.active || demoMode) return
        val now = System.currentTimeMillis()
        val fix = navFixProvider.current()
        val dest = navSession.destination

        // Spoken/Braille distance guidance (throttled inside the session).
        navSession.tick(fix, now)?.let { g ->
            voiceState = g.speech
            router.emit(
                OutputEvent(
                    priority = OutputPriority.NAVIGATION,
                    speech = g.speech,
                    braille = g.braille,
                    urgent = false,
                )
            )
            if (g.arrived) { spatialCue.navArrived(); navFixProvider.stop(); lastLockState = null; return }
        }

        // Progressive lock-on haptics (on top of speech), unless a hazard just fired.
        if (dest == null || fix.point == null) return
        val hazardRecent = now - lastHazardMs < 2_000L
        if (!DirectionalLockHaptics.navigationEnabled(hapticMode)) return
        val bearing = Geo.bearingDegrees(fix.point, dest.point)
        val errorDeg = DirectionalLockHaptics.normalizeError(bearing - fix.headingDeg)
        val cue = DirectionalLockHaptics.cue(errorDeg, fix.headingAccuracy, hazard = hazardRecent)
        if (cue.state == LockState.HAZARD) return // hazard haptics own the channel

        // Announce alignment transition + poor-compass once per change.
        if (cue.state != lastLockState) {
            when (cue.state) {
                LockState.ALIGNED -> respond("Aligned. Continue forward.")
                LockState.SEARCHING -> if (fix.headingAccuracy == HeadingAccuracy.UNRELIABLE)
                    respond("Compass accuracy poor. Use caution.")
                else -> {}
            }
            lastLockState = cue.state
        }
        if (cue.state == LockState.ALIGNED) {
            if (now - lastLockTickMs >= cue.tickIntervalMs) { spatialCue.lockOn(); lastLockTickMs = now }
        } else if (cue.tickIntervalMs > 0 && now - lastLockTickMs >= cue.tickIntervalMs) {
            spatialCue.lockTick(cue.amplitude); lastLockTickMs = now
        }
    }

    /** Change the haptic guidance mode (UI/voice). */
    fun applyHapticMode(mode: HapticGuidanceMode) {
        hapticMode = mode
        spatialCue.hapticsEnabled = DirectionalLockHaptics.safetyEnabled(mode)
        respond("Haptic mode: ${mode.name.lowercase().replace('_', ' ')}.")
    }

    /** Cycle OFF → SAFETY_ONLY → NAVIGATION_LOCK_ON → FULL (accessible toggle). */
    fun cycleHapticMode() {
        val values = HapticGuidanceMode.values()
        applyHapticMode(values[(hapticMode.ordinal + 1) % values.size])
    }

    val hapticModeLabel: String get() = hapticMode.name.lowercase().replace('_', ' ')

    // --- Physical-button hotkeys (foreground; off by default so volume isn't hijacked) ---

    fun setHotkeys(value: Boolean) {
        hotkeysEnabled = value
        respond(if (value) "Button shortcuts on." else "Button shortcuts off.")
    }

    fun toggleHotkeys() = setHotkeys(!hotkeysEnabled)

    /** Dispatch a resolved hotkey command. Confirmation is spoken + shown in Braille status. */
    fun handleHotkey(cmd: HotkeyCommand) {
        when (cmd) {
            HotkeyCommand.REPEAT_LAST -> repeatLast()
            HotkeyCommand.STATUS -> announceBrailleStatus()
            HotkeyCommand.READ_TEXT -> { respond("Key: read text."); readText() }
            HotkeyCommand.FIND_EXIT -> { respond("Key: find exit."); findExit() }
            HotkeyCommand.MUTE_TOGGLE -> toggleMute()
            HotkeyCommand.STOP_NAVIGATION -> stopNavigation()
            HotkeyCommand.EMERGENCY_PAUSE -> emergencyPause()
            HotkeyCommand.NONE -> {}
        }
    }

    /** Pause non-hazard output. Hazards still fire (they bypass mute/pause for safety). */
    fun emergencyPause() {
        speaker.stop()
        if (navSession.active) { navSession.stop(); navFixProvider.stop() }
        respond("Non-hazard output paused. Hazards still active.")
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
            // Adaptive duty cycle from real battery + thermal state (back off under heat/low battery).
            val duty = BatteryThermalPolicy.dutyCycle(pollPowerState())
            serviceStatus = duty.statusLabel
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
            navigationTick()
            delay(duty.analysisIntervalMs)
        }
    }

    /** Read real battery + thermal state for the duty-cycle policy. Cheap; main-thread safe. */
    private fun pollPowerState(): ServiceState {
        val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
        val pct = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        val charging = bm?.isCharging ?: false
        val thermal = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            when (val t = pm?.currentThermalStatus ?: 0) {
                android.os.PowerManager.THERMAL_STATUS_NONE,
                android.os.PowerManager.THERMAL_STATUS_LIGHT -> ThermalLevel.NORMAL
                android.os.PowerManager.THERMAL_STATUS_MODERATE,
                android.os.PowerManager.THERMAL_STATUS_SEVERE -> ThermalLevel.WARNING
                else -> if (t >= android.os.PowerManager.THERMAL_STATUS_CRITICAL) ThermalLevel.CRITICAL else ThermalLevel.NORMAL
            }
        } else ThermalLevel.NORMAL
        return ServiceState(batteryPercent = pct, charging = charging, thermal = thermal, cameraAvailable = cameraActive)
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
        if (urgent) lastHazardMs = System.currentTimeMillis() // hazard haptics preempt nav lock-on
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
        navFixProvider.stop()
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
        override fun navigateTo(destination: String) = startNavigation(destination)
        override fun stopNavigation() = this@ObservaController.stopNavigation()
        override fun find(target: String) = respond("Object finding is not available yet.")
        override fun whereAmI() = this@ObservaController.whereAmI()
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
