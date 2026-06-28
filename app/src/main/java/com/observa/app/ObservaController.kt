package com.observa.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.graphics.Bitmap
import com.observa.app.accessibility.A11yState
import com.observa.app.accessibility.AccessibilityOutputRouter
import com.observa.app.accessibility.AccessibilityStatusReducer
import com.observa.app.accessibility.BrailleSnapshot
import com.observa.app.accessibility.BrailleStatusPresenter
import com.observa.app.accessibility.CueDirection
import com.observa.app.accessibility.DetectorBackend
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

    // --- Navigation Mode = GPS Orientation Lite (real GPS + compass) + offline map-pack status ---
    private val locationProvider = com.observa.app.navigation.LocationProvider(appContext)
    private val compassProvider = com.observa.app.navigation.CompassProvider(appContext)
    private val orientation = com.observa.app.navigation.OrientationController(
        location = locationProvider,
        compass = compassProvider,
    )
    private val mapPacks = com.observa.app.navigation.OfflineMapPackManager(appContext)
    private val mapRepo = com.observa.app.navigation.OfflineMapRepository(appContext, mapPacks)
    private val navigationMode = com.observa.app.navigation.NavigationModeController(
        orientation = orientation,
        mapStatus = { mapPacks.status() },
        hasRenderableMap = { mapRepo.hasRenderableMap },
    )
    private val streetSigns = com.observa.app.navigation.StreetSignTracker()

    // --- Offline Translation Mode — real on-device ML Kit translation ---
    /** Real ML Kit translation + language-pack download (download needs the provisioning build). */
    private val mlkitTranslator = com.observa.app.translation.MlKitOnDeviceTranslator()
    val languageDownloads = com.observa.app.translation.LanguageDownloadController(mlkitTranslator)
    /** Real-time voice-to-voice translation (listen → translate offline → speak in target language). */
    private val liveTranslator = com.observa.app.translation.LiveVoiceTranslator(
        recognizer = recognizer,
        translator = mlkitTranslator,
        speakIn = { text, code -> speaker.speakIn(text, code) },
        onState = { msg -> voiceState = msg },
    )
    /** Real offline map-pack download/install (demo pack works offline; area maps need provisioning). */
    val mapDownloads = com.observa.app.maps.MapDownloadController(appContext)
    private val translation = com.observa.app.translation.TranslationModeController(
        packs = { languageDownloads.pairReady() },
        speechAvailable = { recognizer.available },
        engineAvailable = { true }, // ML Kit translate engine is bundled; the gate is the language pack
    )

    // --- Offline readiness aggregation (setup/debug screen) ---
    private val readiness = com.observa.app.translation.OfflineReadinessChecker {
        listOf(
            com.observa.app.translation.AssetReadiness(
                com.observa.app.translation.OfflineReadinessChecker.DETECTOR,
                modelNeedsPixels, executorch.status.label,
            ),
            com.observa.app.translation.AssetReadiness("OCR", ocr.ready, if (ocr.ready) "ML Kit" else "unavailable"),
            com.observa.app.translation.AssetReadiness("Language pack", languageDownloads.pairReady(), languageDownloads.statusLine()),
            com.observa.app.translation.AssetReadiness("Map pack", mapDownloads.status == com.observa.app.maps.MapPackStatus.READY_OFFLINE, mapDownloads.statusLine()),
            com.observa.app.translation.AssetReadiness("Voice", recognizer.available, if (recognizer.available) "on-device" else "unavailable"),
        )
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
    var orientationActive by mutableStateOf(false); private set
    var orientationLine by mutableStateOf("Orientation off"); private set
    /** Whether TalkBack / touch-exploration is on; drives the two-layer input model. UI keeps it fresh. */
    var talkBackOn by mutableStateOf(false); private set
    val alerts = mutableStateListOf<String>()

    // --- Blind-first two-layer input (raw gestures only when TalkBack is OFF) ---
    private val gestures = com.observa.app.input.BlindGestureController(isTalkBackOn = { talkBackOn })

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

    // --- Native accessibility operating layer (TalkBack custom actions / stable nodes) ---

    /**
     * Short detector backend, mapped truthfully from the real inference status. Backed by a Compose
     * state set once the model finishes initializing, so the accessible status node recomposes and
     * stops showing the pre-load "heuristic fallback" placeholder.
     */
    var detectorBackend by mutableStateOf(DetectorBackend.HEURISTIC); private set

    private fun backendOf(status: InferenceStatus): DetectorBackend = when (status) {
        InferenceStatus.LOADED_QNN -> DetectorBackend.QNN
        InferenceStatus.LOADED_CPU -> DetectorBackend.XNNPACK
        else -> DetectorBackend.HEURISTIC
    }

    private fun a11ySnapshot() = A11yState(
        awarenessActive = observing,
        muted = muted,
        detector = detectorBackend,
        ocrReady = ocr.ready,
        translationInstalled = languageDownloads.pairReady(),
        navigating = navSession.active,
        lastAlert = if (lastAlert == "No alerts yet") null else lastAlert,
        orientation = if (orientationActive) orientationLine else null,
    )

    /** Stable accessible node texts for the operating layer (derived by the pure reducer). */
    val a11yCurrentStatus: String get() = AccessibilityStatusReducer.currentStatus(a11ySnapshot())
    val a11yLastAlertNode: String get() = AccessibilityStatusReducer.lastAlert(a11ySnapshot())
    val a11yActionsNode: String get() = AccessibilityStatusReducer.availableActions(a11ySnapshot())
    val awarenessStateDesc: String get() = AccessibilityStatusReducer.awarenessState(a11ySnapshot())
    val detectorStateDesc: String get() = AccessibilityStatusReducer.detectorState(a11ySnapshot())

    /** Honest scene question: a brightness-based summary (no scene VLM bundled yet — says so). */
    fun sceneQuestion() = describeScene()

    // --- Offline Translation Mode (honest readiness; never fakes a translation) ---
    /** Truthful translation status: "ready offline" / "language pack missing" / "speech unavailable". */
    /** Honest text-translation readiness from the real ML Kit language pack state. */
    val translationStatus: String get() = languageDownloads.statusLine()
    val translationPair: String get() = "${languageDownloads.sourceLang} to ${languageDownloads.targetLang}"

    /** "Start translation": begin real-time voice-to-voice translation if ready, else say what's missing. */
    fun startTranslation() {
        if (!languageDownloads.pairReady()) {
            respond("${languageDownloads.statusLine()}. Say \"download ${com.observa.app.translation.LanguageCatalog.nameFor(languageDownloads.targetLang)}\" or open Download Languages first.")
            return
        }
        // Live translation owns the mic; stop push-to-talk command listening first.
        pushToTalk.onReleased()
        respond("Live translation: ${com.observa.app.translation.LanguageCatalog.nameFor(languageDownloads.sourceLang)} to ${com.observa.app.translation.LanguageCatalog.nameFor(languageDownloads.targetLang)}. Speak now.")
        liveTranslator.start(languageDownloads.sourceLang, languageDownloads.targetLang, continuous = true)
    }

    /** Flip the live-translation direction for a two-way conversation. */
    fun swapTranslationDirection() { liveTranslator.swap(); languageDownloads.swap() }
    val liveTranslationActive: Boolean get() = liveTranslator.active

    /** Translate arbitrary text offline via ML Kit; speaks + shows the result. Never fabricates. */
    fun translateText(text: String) {
        if (!languageDownloads.pairReady()) { respond(languageDownloads.statusLine()); return }
        respond("Translating…")
        languageDownloads.translate(text) { out -> respond(out) }
    }

    fun stopTranslation() { liveTranslator.stop(); respond("Translation stopped.") }
    fun repeatTranslation() =
        respond(languageDownloads.lastTranslation.ifBlank { languageDownloads.statusLine() })

    /** Refresh real installed language packs (call when entering translation UI). */
    fun refreshLanguagePacks() = languageDownloads.refresh()

    // --- Download actions (also exposed as TalkBack/braille custom actions) ---
    /** Install the offline demo map pack (works offline) and announce honest status. */
    fun downloadMapPack() { mapDownloads.installDemoPack(); respond("Map: ${mapDownloads.statusLine()}. ${mapDownloads.detail}") }
    /** Start a language-pack download (provisioning build only) and announce honest status. */
    fun downloadLanguages() {
        languageDownloads.downloadSelected()
        respond("Languages ${translationPair}: ${languageDownloads.statusLine()}. ${languageDownloads.detail}")
    }

    /** Voice: "download <language>" — set that language as the translation target and download it. */
    fun downloadLanguageByName(name: String) {
        val code = com.observa.app.translation.LanguageCatalog.codeFor(name)
        if (code == null) { respond("Language $name is not supported on device."); return }
        languageDownloads.setTarget(code)
        languageDownloads.downloadSelected()
        respond("Downloading ${com.observa.app.translation.LanguageCatalog.nameFor(code)} for offline translation. ${languageDownloads.detail}")
    }

    /** Voice / button: download a real offline map of the user's current GPS area (provisioning build). */
    fun downloadCurrentAreaMap() {
        val pt = locationProvider.current()
        if (pt == null) { respond("No GPS fix yet. Go outside or near a window, then say download map again."); return }
        respond("Downloading map for your current area…")
        mapDownloads.downloadAreaMap(pt.lat, pt.lon) { msg ->
            respond(msg)
            refreshNavDestinationsFromMap()
        }
    }

    /** Load named places from the installed offline map pack as navigation destinations. */
    private fun refreshNavDestinationsFromMap() {
        mapRepo.places().forEach { destinations.add(it) }
    }

    // --- Navigation Mode = GPS Orientation Lite + offline map-pack status (hazards always override) ---
    /** Visible Navigation panel status: live guidance + honest map-pack line. */
    val navStatusLine: String get() = navigationMode.statusLine()
    /** Honest offline map-pack status ("Map ready offline" / "Map pack missing" / "corrupt"). */
    val mapPackStatusLine: String get() = mapDownloads.statusLine()
    val navigationModeActive: Boolean get() = orientationActive

    fun startOrientation() {
        orientationActive = true
        val g = orientation.start()
        orientationLine = orientation.statusLine()
        emitOrientation(g)
        respond("Navigation on. ${navigationMode.mapLine()}.")
    }

    fun stopOrientation() {
        orientation.stop()
        orientationActive = false
        orientationLine = "Orientation off"
        respond("Orientation off.")
    }

    fun repeatOrientation() {
        if (!orientation.active) { respond("Orientation is off."); return }
        emitOrientation(orientation.repeat())
        orientationLine = orientation.statusLine()
    }

    /** One orientation guidance tick (NAVIGATION priority → a hazard always interrupts it). */
    private fun orientationTick() {
        if (!orientation.active || demoMode) return
        orientation.tick(System.currentTimeMillis(), lastHazardMs)?.let {
            emitOrientation(it)
            orientationLine = orientation.statusLine()
        }
    }

    private fun emitOrientation(g: com.observa.app.navigation.OrientationGuidance) {
        voiceState = g.speech
        router.emit(
            OutputEvent(
                priority = OutputPriority.NAVIGATION,
                speech = g.speech,
                braille = g.braille,
                urgent = false,
            )
        )
        navHaptic(g) // directional haptic feedback so the user can FEEL which way to face
    }

    /** Directional haptic for navigation: left/right pulse to turn, forward when aligned, arrival buzz. */
    private fun navHaptic(g: com.observa.app.navigation.OrientationGuidance) {
        if (!hapticCuesEnabled) return
        if (g.arrived) { spatialCue.navArrived(); return }
        val cueDir = when (g.direction) {
            com.observa.app.navigation.RelativeDirection.LEFT,
            com.observa.app.navigation.RelativeDirection.SLIGHT_LEFT -> CueDirection.LEFT
            com.observa.app.navigation.RelativeDirection.RIGHT,
            com.observa.app.navigation.RelativeDirection.SLIGHT_RIGHT -> CueDirection.RIGHT
            com.observa.app.navigation.RelativeDirection.AHEAD -> CueDirection.CENTER
            else -> CueDirection.NONE // behind / no fix → no directional pulse
        }
        if (g.direction == com.observa.app.navigation.RelativeDirection.AHEAD) spatialCue.lockOn()
        else if (cueDir != CueDirection.NONE) spatialCue.navDirection(cueDir)
    }

    /** Silence non-hazard output. Hazards still fire (safety). Same path as the emergency pause. */
    fun silenceAlerts() = emergencyPause()

    // --- Blind-first two-layer input ---

    /** UI reports TalkBack/touch-exploration state so raw gestures are only used when it's off. */
    fun updateTalkBackOn(on: Boolean) { talkBackOn = on }

    /** Status line the UI shows: gesture hints when TalkBack is off, native-actions pointer when on. */
    val gestureStatusLine: String get() = gestures.statusLine()

    /** True only when raw single-finger gestures should be wired (TalkBack off). */
    val rawGesturesActive: Boolean get() = gestures.rawGesturesActive()

    /** UI hands a completed tap count; mapped to a double/triple-tap gesture and dispatched. */
    fun onTaps(count: Int) = gestures.tapGesture(count)?.let { onGesture(it) }

    /** UI hands a vertical swipe delta (px, up negative); mapped to a swipe gesture and dispatched. */
    fun onVerticalSwipe(dyPx: Float, thresholdPx: Float) =
        gestures.swipeGesture(dyPx, thresholdPx)?.let { onGesture(it) }

    /** Dispatch a raw gesture detected by the surface. No-op when TalkBack is on (returns NONE). */
    fun onGesture(gesture: com.observa.app.input.BlindGesture) {
        when (gestures.resolve(gesture)) {
            com.observa.app.input.GestureAction.OPEN_VOICE -> openVoiceCommands()
            com.observa.app.input.GestureAction.PUSH_TO_TALK -> openVoiceCommands()
            com.observa.app.input.GestureAction.REPEAT_LAST -> repeatLast()
            com.observa.app.input.GestureAction.START_TRANSLATION -> startTranslation()
            com.observa.app.input.GestureAction.START_ORIENTATION -> startOrientation()
            com.observa.app.input.GestureAction.NONE -> {}
        }
    }

    /** Open hold-to-talk voice commands (the "triple tap" / native action entry point). */
    fun openVoiceCommands() {
        if (!recognizer.available) { respond("Voice unavailable. Use the on-screen actions menu."); return }
        respond("Voice commands. Speak now.")
        pushToTalk.onPressed()
    }

    /** Truthful one-line backend status for current/debug surfaces. Never claims NPU unless active. */
    val backendStatusLine: String
        get() = when (detectorBackend) {
            DetectorBackend.QNN -> "Detector backend: QNN/NPU active"
            DetectorBackend.XNNPACK ->
                if (executorch.qnnError.isNotBlank())
                    "Detector backend: XNNPACK CPU fallback. QNN attempted: ${executorch.qnnError}"
                else "Detector backend: XNNPACK CPU"
            DetectorBackend.HEURISTIC -> "Detector backend: heuristic fallback (no model)"
        }

    /** QNN/NPU pipeline stage, for the debug surface. "active" only after a real warm-up forward. */
    val qnnStageLine: String get() = "QNN stage: ${executorch.qnnStage}"

    // --- NPU Debug screen (structured backend diagnostics) ---
    /** One-line accessible summary: "Backend: XNNPACK CPU. Fell back from QNN at WARMUP_FORWARD: skel 4000". */
    val npuSummaryLine: String get() = executorch.diagnostics.summaryLine()
    /** True only when a QNN warm-up forward actually succeeded on device. */
    val npuActive: Boolean get() = executorch.diagnostics.npuActive()

    /** Build + device identity header for the debug report. */
    private fun deviceHeader(): String = buildString {
        append("App ${com.observa.app.BuildConfig.VERSION_NAME} (vc ${com.observa.app.BuildConfig.VERSION_CODE}) ")
        append("sha ${com.observa.app.BuildConfig.GIT_SHA} built ${com.observa.app.BuildConfig.BUILD_TIME}\n")
        append("Device ${android.os.Build.MODEL} (${android.os.Build.DEVICE}) · Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
        append("ABI ${android.os.Build.SUPPORTED_ABIS.firstOrNull()} · board ${android.os.Build.BOARD} · hardware ${android.os.Build.HARDWARE}")
        if (android.os.Build.VERSION.SDK_INT >= 31) append(" · soc ${android.os.Build.SOC_MODEL}")
    }

    /** Structured lines for the NPU Debug screen (all TalkBack-readable). */
    fun npuDebugLines(): List<String> = buildList {
        add(npuSummaryLine)
        add(deviceHeader())
        add("Backend priority: ${com.observa.app.inference.BackendSelector.priorityDescription()}")
        add(backendStatusLine)
        add(qnnStageLine)
        add("NPU active: $npuActive")
        executorch.diagnostics.firstFailure()?.let {
            add("First failure: ${it.backend.name}/${it.stage.name} ${it.nativeHint.ifBlank { it.message }}")
        }
        if (!npuActive) add(
            "Native HTP blocker (via: adb logcat | grep QnnDsp): on this retail S25 Ultra the QNN " +
                "warm-up shows 'Failed to load skel, error 4000' / device_handle 14001 — the cDSP refuses " +
                "the third-party unsigned protection domain. In-app native-log read is blocked for " +
                "untrusted_app, so capture it over adb.")
        add("— attempts —")
        executorch.diagnostics.all().forEach { a ->
            add("${a.backend.label} · ${a.stage.name} · ${if (a.success) "ok" else "FAIL"}" +
                (if (a.elapsedMs > 0) " ${a.elapsedMs}ms" else "") +
                (if (a.nativeHint.isNotBlank()) " · ${a.nativeHint}" else "") +
                (if (a.message.isNotBlank()) " · ${a.message}" else ""))
        }
        add(mapPackLine); add(languagePackLine); add(gpsStatusLine); add(compassStatusLine)
        add(ocrStatusLine); add(voiceStatusLine); add(internetStatusLine); add(readinessSummaryLine)
    }

    /** Full copy-paste debug report (for judges / Qualcomm). */
    fun npuDebugReport(): String = executorch.diagnostics.report(deviceHeader())

    /** "Force QNN attempt once" / "Run detector warm-up": re-run init off the UI thread, then announce. */
    fun forceQnnAttempt() {
        respond("Re-running QNN attempt…")
        scope.launch {
            withContext(Dispatchers.IO) { executorch.initialize(appContext) }
            detectorBackend = backendOf(executorch.status)
            respond(npuSummaryLine)
        }
    }

    /** "Disable GPU fallback for this session" — honest no-op (no GPU path exists), but documents policy. */
    fun setDisableGpuFallback(disabled: Boolean) {
        com.observa.app.inference.BackendSelector.disableGpuFallback = disabled
        respond(if (disabled) "GPU fallback disabled for NPU investigation. No GPU path is implemented anyway; fallback is XNNPACK CPU."
        else "GPU fallback flag cleared. Note: OBSERVA has no GPU path.")
    }
    val gpuFallbackDisabled: Boolean get() = com.observa.app.inference.BackendSelector.disableGpuFallback
    fun announceCopied() = respond("NPU debug report copied to clipboard.")

    /** Live NPU/detector usage snapshot for the NPU Data graph (latency + throughput + backend). */
    fun npuUsageSnapshot() = executorch.usage.snapshot()
    /** Speak the current live NPU usage (the NPU Data accessible summary / action). */
    fun announceNpuUsage() = respond(executorch.usage.snapshot().summaryLine())

    /** Speak the engineering/debug detector status (kept out of normal alert output). */
    fun announceDebugStatus() = respond("$backendStatusLine. $qnnStageLine. ${aiDiagnostics}")

    // --- Debug/Status screen lines (all truthful; never claim more than is real) ---
    val qnnErrorLine: String get() = "QNN error: ${executorch.qnnError.ifBlank { "none" }}"
    val mapPackLine: String get() = "Map pack: ${mapDownloads.statusLine()}"
    val languagePackLine: String get() = "Language pack: ${languageDownloads.statusLine()} (${translationPair})"
    val gpsStatusLine: String get() = "GPS: ${if (locationProvider.hasPermission) "permission granted" else "permission needed"}"
    val compassStatusLine: String get() = "Compass: ${if (compassProvider.available) "available" else "unavailable"}"
    val ocrStatusLine: String get() = "OCR: ${if (ocr.ready) "ready (ML Kit, offline)" else "unavailable"}"
    val voiceStatusLine: String get() = "Voice: ${when {
        !recognizer.available -> "unavailable"
        recognizer.onDeviceOnly -> "on-device ready"
        else -> "ready"
    }}"
    val internetStatusLine: String = "INTERNET permission: not declared (offline by design)"
    val readinessSummaryLine: String get() = "Offline readiness: ${readiness.summaryLine()}"
    val appVersionLine: String
        get() = "Version ${com.observa.app.BuildConfig.VERSION_NAME} (${com.observa.app.BuildConfig.GIT_SHA}) · built ${com.observa.app.BuildConfig.BUILD_TIME}"

    /**
     * Read text / street signs on demand (the "Read Text / Signs" button). Same honest one-shot OCR as
     * [readText] but framed for signage; never fabricates sign text — says so when nothing is readable.
     */
    fun readSigns() = runOcr("Reading signs…") { result ->
        emitOcr(if (result.found) "Sign text: ${result.text}" else "No readable sign text.")
    }

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
            HotkeyCommand.VOICE_COMMANDS -> { respond("Voice commands."); openVoiceCommands() }
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
            detectorBackend = backendOf(executorch.status)
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
            orientationTick()
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
        orientation.stop()
        locationProvider.stop()
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
        override fun startNavigation() = startOrientation()
        override fun stopNavigation() { this@ObservaController.stopNavigation(); stopOrientation() }
        override fun startTranslation() = this@ObservaController.startTranslation()
        override fun stopTranslation() = this@ObservaController.stopTranslation()
        override fun downloadLanguage(language: String) = downloadLanguageByName(language)
        override fun downloadMap() = downloadCurrentAreaMap()
        override fun readSigns() = this@ObservaController.readSigns()
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
