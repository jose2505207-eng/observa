package com.observa.app.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.observa.app.ObservaController
import com.observa.app.hazard.FrameInput
import com.observa.app.input.BlindGesture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

private val Bg = Color(0xFF000000)
private val Panel = Color(0xFF101418)
private val Accent = Color(0xFFFFEB3B)      // high-contrast yellow
private val OnDark = Color(0xFFFFFFFF)
private val Good = Color(0xFF69F0AE)

@Composable
fun ObservaScreen(controller: ObservaController) {
    // The processing loop (detector) and TalkBack tracking run at the top level, so detection keeps
    // running even while the user is on the Map/Language sub-screens.
    LaunchedEffect(Unit) { controller.runProcessingLoop() }
    TrackTalkBackState(controller)

    var screen by remember { mutableStateOf("main") }
    when (screen) {
        "map" -> MapDownloadScreen(controller, onBack = { screen = "main" })
        "lang" -> LanguageDownloadScreen(controller, onBack = { screen = "main" })
        "npu" -> NpuDebugScreen(controller, onBack = { screen = "main" })
        "npudata" -> NpuDataScreen(controller, onBack = { screen = "main" })
        else -> MainScreen(
            controller,
            onOpenMap = { screen = "map" },
            onOpenLanguages = { screen = "lang" },
            onOpenNpu = { screen = "npu" },
            onOpenNpuData = { screen = "npudata" },
        )
    }
}

@Composable
private fun MainScreen(
    controller: ObservaController,
    onOpenMap: () -> Unit,
    onOpenLanguages: () -> Unit,
    onOpenNpu: () -> Unit,
    onOpenNpuData: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .safeDrawingPadding() // keep content clear of status bar + gesture nav bar
            .verticalScroll(rememberScrollState()) // all controls reachable; camera stays visible
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "OBSERVA",
            color = Accent,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = "OBSERVA, offline accessibility assistant" },
        )
        Text(
            text = controller.privacyLabel,
            color = Good,
            fontSize = 16.sp,
            modifier = Modifier.semantics { contentDescription = "Privacy: ${controller.privacyLabel}" },
        )

        OperatingLayer(controller, onOpenNpu, onOpenNpuData)

        ModeButtons(controller, onOpenMap, onOpenLanguages)

        GestureHint(controller)

        BlindGestureLayer(controller) {
            CameraPanel(controller, modifier = Modifier.fillMaxWidth().height(280.dp))
        }

        AlertBanner(controller.lastAlert)
        NavigationCard(controller)
        TranslationCard(controller)
        BrailleStatus(controller.brailleStatus)
        DebugCard(controller, onOpenNpu, onOpenNpuData)
        Dashboard(controller)
        Controls(controller)
    }
}

/** High-contrast, TalkBack-labeled mode buttons — the visible blind-first hub. No icon-only controls. */
@Composable
private fun ModeButtons(controller: ObservaController, onOpenMap: () -> Unit, onOpenLanguages: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("modeButtons"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigActionButton("Awareness", if (controller.observing) "on" else "off",
                if (controller.observing) "Pause awareness detection" else "Start awareness detection",
                Modifier.weight(1f), if (controller.observing) Good else Accent,
            ) { controller.observe(!controller.observing) }
            BigActionButton("Navigate", if (controller.navigationModeActive) "on" else "off",
                "Start navigation. Compass and GPS bearing guidance to a destination.",
                Modifier.weight(1f), if (controller.navigationModeActive) Good else Accent,
            ) { if (controller.navigationModeActive) controller.stopOrientation() else controller.startOrientation() }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigActionButton("Download Map", null,
                "Open the map download screen. Install the offline map pack.",
                Modifier.weight(1f), Accent) { onOpenMap() }
            BigActionButton("Download Languages", null,
                "Open the language download screen. Install offline translation packs.",
                Modifier.weight(1f), Accent) { onOpenLanguages() }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigActionButton("Translate", null,
                "Open translation. Offline language readiness. ${controller.translationStatus}.",
                Modifier.weight(1f), Accent,
            ) { onOpenLanguages() }
            BigActionButton("Voice Commands", null,
                "Open voice commands. Speak a command after the prompt.",
                Modifier.weight(1f), Accent,
            ) { controller.openVoiceCommands() }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigActionButton("Read Signs", null,
                "Read text or street signs aloud. Point the camera and tap.",
                Modifier.weight(1f), Accent, enabled = controller.ocrAvailable,
            ) { controller.readSigns() }
            BigActionButton("Repeat Alert", null, "Repeat the last spoken alert.",
                Modifier.weight(1f), Panel) { controller.repeatLast() }
        }
    }
}

/** One large accessible button. [state] becomes stateDescription when present. */
@Composable
private fun BigActionButton(
    label: String,
    state: String?,
    description: String,
    modifier: Modifier,
    container: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(72.dp)
            .testTag("mode_$label")
            .semantics {
                role = Role.Button
                contentDescription = description
                if (state != null) stateDescription = state
            },
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = if (container == Panel) OnDark else Bg),
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** Visible Navigation panel + its accessible "Navigation status" node. Start/Stop/Repeat. */
@Composable
private fun NavigationCard(controller: ObservaController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("navigationCard")
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = "Navigation status. ${controller.navStatusLine}"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Navigation", color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(controller.navStatusLine, color = Good, fontSize = 15.sp)
        Text(controller.mapPackStatusLine, color = OnDark, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallButton(if (controller.navigationModeActive) "Stop" else "Start",
                if (controller.navigationModeActive) "Stop navigation" else "Start navigation", Modifier.weight(1f)) {
                if (controller.navigationModeActive) controller.stopOrientation() else controller.startOrientation()
            }
            SmallButton("Repeat", "Repeat navigation guidance", Modifier.weight(1f),
                enabled = controller.navigationModeActive) { controller.repeatOrientation() }
        }
    }
}

/** Visible Translation panel + its accessible "Translation status" node. Honest readiness. */
@Composable
private fun TranslationCard(controller: ObservaController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("translationCard")
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = "Translation status. ${controller.translationStatus}"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Translation", color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(controller.translationStatus, color = Good, fontSize = 15.sp)
        Text("Source auto · Target English · offline only", color = OnDark, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallButton("Start", "Start listening for translation", Modifier.weight(1f)) { controller.startTranslation() }
            SmallButton("Stop", "Stop translation", Modifier.weight(1f)) { controller.stopTranslation() }
            SmallButton("Repeat", "Repeat last translation status", Modifier.weight(1f)) { controller.repeatTranslation() }
        }
    }
}

/** Collapsible Debug/Status panel (Phase 10). Truthful backend, packs, sensors, permissions. */
@Composable
private fun DebugCard(controller: ObservaController, onOpenNpu: () -> Unit, onOpenNpuData: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("debugCard"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Button(
            onClick = onOpenNpuData,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("npuDataButton")
                .semantics { role = Role.Button; contentDescription = "Open NPU data screen. Live graph of NPU inference latency and throughput." },
            colors = ButtonDefaults.buttonColors(containerColor = Good, contentColor = Bg),
        ) { Text("NPU Data", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Button(
            onClick = onOpenNpu,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("npuDebugButton")
                .semantics { role = Role.Button; contentDescription = "Open NPU debug screen. Backend selection, QNN attempts, and the exact NPU blocker." },
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
        ) { Text("NPU Debug", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Button(
            onClick = { open = !open; controller.announceDebugStatus() },
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("debugStatusButton")
                .semantics { role = Role.Button; contentDescription = "Debug status. Backend, packs, sensors, permissions." },
            colors = ButtonDefaults.buttonColors(containerColor = Bg, contentColor = OnDark),
        ) { Text(if (open) "Debug Status ▲" else "Debug Status ▼", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        if (open) {
            val lines = listOf(
                controller.appVersionLine,
                controller.backendStatusLine,
                controller.qnnStageLine,
                controller.qnnErrorLine,
                controller.mapPackLine,
                controller.languagePackLine,
                controller.gpsStatusLine,
                controller.compassStatusLine,
                controller.ocrStatusLine,
                controller.voiceStatusLine,
                controller.internetStatusLine,
                controller.readinessSummaryLine,
            )
            lines.forEach { Text(it, color = OnDark, fontSize = 13.sp,
                modifier = Modifier.semantics { contentDescription = it }) }
        }
    }
}

@Composable
private fun SmallButton(label: String, description: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp).semantics { role = Role.Button; contentDescription = description },
        colors = ButtonDefaults.buttonColors(containerColor = Bg, contentColor = OnDark),
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

/**
 * Keep the controller's TalkBack/touch-exploration flag fresh. Raw single-finger gestures are only a
 * reliable channel when this is OFF; when ON, screen-reader users get the native custom actions.
 */
@Composable
private fun TrackTalkBackState(controller: ObservaController) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val am = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE)
            as android.view.accessibility.AccessibilityManager
        controller.updateTalkBackOn(am.isTouchExplorationEnabled)
        val listener = android.view.accessibility.AccessibilityManager
            .TouchExplorationStateChangeListener { enabled -> controller.updateTalkBackOn(enabled) }
        am.addTouchExplorationStateChangeListener(listener)
        onDispose { am.removeTouchExplorationStateChangeListener(listener) }
    }
}

/**
 * Honest gesture hint line. With TalkBack off it advertises the raw gestures; with TalkBack on it
 * points users to the guaranteed native actions instead of unreliable one-finger gestures.
 */
@Composable
private fun GestureHint(controller: ObservaController) {
    Text(
        text = controller.gestureStatusLine,
        color = OnDark,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gestureHint")
            .semantics { contentDescription = controller.gestureStatusLine },
    )
}

/**
 * Layer B: raw screen gestures, wired **only when TalkBack is off** (otherwise touch-exploration owns
 * one-finger gestures, so we don't fight it). Triple tap → voice commands, double tap → repeat last,
 * swipe up → translation, swipe down → orientation, long press → push-to-talk. All resolved by the
 * pure [com.observa.app.input.BlindGestureController]; hazards still interrupt via the output router.
 */
@Composable
private fun BlindGestureLayer(controller: ObservaController, content: @Composable () -> Unit) {
    val active = controller.rawGesturesActive
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    var tapCount by remember { mutableIntStateOf(0) }
    var flush by remember { mutableStateOf<Job?>(null) }
    var dragDy by remember { mutableStateOf(0f) }

    val gestureModifier = if (active) {
        Modifier
            .pointerInput(active) {
                detectTapGestures(
                    onLongPress = { controller.onGesture(BlindGesture.LONG_PRESS) },
                    onTap = {
                        tapCount++
                        flush?.cancel()
                        flush = scope.launch {
                            delay(320) // window to disambiguate single/double/triple tap
                            controller.onTaps(tapCount)
                            tapCount = 0
                        }
                    },
                )
            }
            .pointerInput(active) {
                detectVerticalDragGestures(
                    onDragStart = { dragDy = 0f },
                    onVerticalDrag = { _, dy -> dragDy += dy },
                    onDragEnd = { controller.onVerticalSwipe(dragDy, threshold) },
                )
            }
    } else Modifier

    Box(modifier = gestureModifier) { content() }
}

/** A labeled, TalkBack-friendly on/off toggle row with a stable test tag. */
@Composable
private fun ToggleRow(label: String, checked: Boolean, testTag: String, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(testTag)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                stateDescription = if (checked) "on" else "off"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = OnDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Braille-friendly status as a polite TalkBack live region (drives a connected braille display). */
@Composable
private fun BrailleStatus(status: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("brailleStatus")
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Status: $status"
            },
    ) {
        Text(text = status, color = Good, fontSize = 16.sp)
    }
}

/**
 * Native accessibility operating layer. Three stable, focusable nodes (Current status, Last alert,
 * Available actions) plus TalkBack/braille **custom actions** so every core flow is operable without
 * relying on the visual buttons below. These nodes are NOT live regions — they update silently and
 * are read on demand, so a refreshable braille display is not flooded. The hazard banner (assertive
 * live region) and braille status (polite live region) remain the push channels.
 */
@Composable
private fun OperatingLayer(controller: ObservaController, onOpenNpu: () -> Unit, onOpenNpuData: () -> Unit) {
    val actions = listOf(
        CustomAccessibilityAction("Start awareness") { controller.observe(true); true },
        CustomAccessibilityAction("Pause awareness") { controller.observe(false); true },
        CustomAccessibilityAction(
            if (controller.navigationModeActive) "Stop navigation" else "Navigate",
        ) { if (controller.navigationModeActive) controller.stopOrientation() else controller.startOrientation(); true },
        CustomAccessibilityAction("Repeat navigation") { controller.repeatOrientation(); true },
        CustomAccessibilityAction("Download map") { controller.downloadMapPack(); true },
        CustomAccessibilityAction("Translate") { controller.startTranslation(); true },
        CustomAccessibilityAction("Download languages") { controller.downloadLanguages(); true },
        CustomAccessibilityAction("Repeat translation") { controller.repeatTranslation(); true },
        CustomAccessibilityAction("Voice commands") { controller.openVoiceCommands(); true },
        CustomAccessibilityAction("Read signs") { controller.readSigns(); true },
        CustomAccessibilityAction("Repeat last alert") { controller.repeatLast(); true },
        CustomAccessibilityAction("Silence alerts") { controller.silenceAlerts(); true },
        CustomAccessibilityAction("Open debug status") { controller.announceDebugStatus(); true },
        CustomAccessibilityAction("Open NPU debug") { onOpenNpu(); true },
        CustomAccessibilityAction("Open NPU data") { onOpenNpuData(); true },
        CustomAccessibilityAction("Speak NPU usage") { controller.announceNpuUsage(); true },
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("operatingLayer"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Current status — focusable heading; carries semantic awareness state.
        Text(
            text = controller.a11yCurrentStatus,
            color = Good,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("statusNode")
                .semantics(mergeDescendants = true) {
                    heading()
                    contentDescription = "Current status"
                    stateDescription = controller.a11yCurrentStatus
                },
        )
        // Last alert — stable, read on demand (the assertive banner handles push).
        Text(
            text = controller.a11yLastAlertNode,
            color = Accent,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("lastAlertNode")
                .semantics(mergeDescendants = true) {
                    contentDescription = controller.a11yLastAlertNode
                },
        )
        // Available actions — the operating hub. Carries the custom accessibility actions so a
        // TalkBack user (or braille display) can run any core flow from the actions menu.
        Text(
            text = controller.a11yActionsNode,
            color = OnDark,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("actionsNode")
                .semantics(mergeDescendants = true) {
                    contentDescription = controller.a11yActionsNode
                    customActions = actions
                },
        )
    }
}

@Composable
private fun CameraPanel(controller: ObservaController, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) { onDispose { analysisExecutor.shutdown() } }

    Box(
        modifier = modifier
            .background(Panel, RoundedCornerShape(12.dp))
            .semantics { contentDescription = "Live camera preview. Analyzer running on device." },
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    try {
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build()
                            .also { it.surfaceProvider = previewView.surfaceProvider }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .build()
                            .also { a ->
                                a.setAnalyzer(analysisExecutor) { proxy ->
                                    try {
                                        controller.submitFrame(proxy.toFrameInput(controller.modelNeedsPixels))
                                        if (controller.wantsOcrCapture) {
                                            controller.submitOcrFrame(proxy.toBitmap())
                                        }
                                    } finally {
                                        proxy.close()
                                    }
                                }
                            }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                        controller.updateCameraActive(true)
                    } catch (e: Exception) {
                        Log.e("OBSERVA_CAMERA", "Camera binding failed", e)
                        controller.updateCameraActive(false)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )
    }
}

@Composable
private fun AlertBanner(lastAlert: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .semantics {
                liveRegion = LiveRegionMode.Assertive
                contentDescription = "Latest alert: $lastAlert"
            },
    ) {
        Text(text = lastAlert, color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Dashboard(controller: ObservaController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatRow("Camera", if (controller.cameraActive) "Active" else "Inactive",
            if (controller.cameraActive) Good else Accent)
        StatRow("Frames", controller.frameCount.toString(), OnDark)
        StatRow("FPS", String.format("%.1f", controller.fps), OnDark)
        StatRow("Backend", "${controller.backendName} (${controller.backendStatus})", OnDark)
        StatRow(
            "AI model",
            controller.aiModelStatus.removePrefix("AI model: "),
            if (controller.modelNeedsPixels) Good else Accent,
        )
        StatRow("AI detail", controller.aiDiagnostics, OnDark)
        StatRow("Alert cooldown", controller.cooldownNote, OnDark)
        StatRow("Observing", if (controller.observing) "On" else "Off", if (controller.observing) Good else Accent)
        StatRow("Demo Mode", if (controller.demoMode) "On" else "Off", if (controller.demoMode) Good else OnDark)
        StatRow("Speech", if (controller.ttsReady) (if (controller.muted) "Muted" else "Ready") else "Initializing", OnDark)
        StatRow("Haptics", if (controller.hapticsAvailable) "Available" else "Unavailable", OnDark)
        StatRow(
            "Voice",
            when {
                !controller.voiceAvailable -> "Unavailable"
                controller.listening -> "Listening"
                controller.voiceOnDevice -> "On-device ready"
                else -> "Ready"
            },
            if (controller.voiceAvailable) OnDark else Accent,
        )
        StatRow("Service", controller.serviceStatus, OnDark)
        StatRow("Privacy", controller.privacyLabel, Good)
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label: $value" },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = OnDark, fontSize = 16.sp)
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Controls(controller: ObservaController) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = {
                if (controller.demoMode) controller.stopDemo()
                else scope.launch { controller.runDemo() }
            },
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .semantics {
                    contentDescription = if (controller.demoMode) "Stop demo mode" else "Start demo mode"
                },
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
        ) {
            Text(if (controller.demoMode) "Stop Demo" else "Start Demo", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { controller.toggleMute() },
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .semantics { contentDescription = if (controller.muted) "Unmute speech" else "Mute speech" },
            colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = OnDark),
        ) {
            Text(if (controller.muted) "Unmute" else "Mute", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("observingToggle")
            .semantics(mergeDescendants = true) {
                contentDescription = "Observing, awareness"
                stateDescription = if (controller.observing) "on" else "off"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Observing", color = OnDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Switch(checked = controller.observing, onCheckedChange = { controller.observe(it) })
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("brailleToggle")
            .semantics(mergeDescendants = true) {
                contentDescription = "Braille status"
                stateDescription = if (controller.brailleEnabled) "on" else "off"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Braille status", color = OnDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Switch(checked = controller.brailleEnabled, onCheckedChange = { controller.setBraille(it) })
    }
    Spacer(Modifier.height(8.dp))
    ToggleRow(
        label = "Audio cues",
        checked = controller.audioCuesEnabled,
        testTag = "audioToggle",
        onChange = { controller.setAudioCues(it) },
    )
    Spacer(Modifier.height(8.dp))
    ToggleRow(
        label = "Haptics",
        checked = controller.hapticCuesEnabled,
        testTag = "hapticsToggle",
        onChange = { controller.setHapticCues(it) },
    )
    Spacer(Modifier.height(8.dp))
    ToggleRow(
        label = "Button shortcuts",
        checked = controller.hotkeysEnabled,
        testTag = "hotkeysToggle",
        onChange = { controller.setHotkeys(it) },
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { controller.readText() },
        enabled = controller.ocrAvailable,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .testTag("readTextButton")
            .semantics {
                contentDescription = if (controller.ocrAvailable)
                    "Read text. Point the camera at text and double tap to read it aloud."
                else "Read text unavailable."
            },
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
    ) {
        Text("Read Text", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { controller.onMicPressed() },
        enabled = controller.voiceAvailable,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics {
                contentDescription =
                    if (controller.voiceAvailable) "Voice command. Tap, then speak a command. ${controller.voiceState}"
                    else "Voice command unavailable. Use the on-screen buttons."
            },
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
    ) {
        Text(
            if (controller.listening) "Listening…" else "Voice Command",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
    Text(
        text = controller.voiceState,
        color = OnDark,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
    Spacer(Modifier.height(8.dp))
    NavPanel(controller)
    Spacer(Modifier.height(4.dp))
}

/** Accessible, guidance-first navigation controls: saved destinations + stop + where am I. */
@Composable
private fun NavPanel(controller: ObservaController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("navPanel"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Navigation (offline)", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        controller.savedDestinations.forEach { name ->
            Button(
                onClick = { controller.startNavigation(name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = "Navigate to $name" },
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
            ) { Text("Go: $name", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { controller.whereAmI() },
                modifier = Modifier.weight(1f).height(56.dp)
                    .semantics { contentDescription = "Where am I" },
                colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = OnDark),
            ) { Text("Where am I", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Button(
                onClick = { controller.stopNavigation() },
                enabled = controller.navigating,
                modifier = Modifier.weight(1f).height(56.dp)
                    .semantics { contentDescription = "Stop navigation" },
                colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = OnDark),
            ) { Text("Stop nav", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
        Button(
            onClick = { controller.cycleHapticMode() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("hapticModeButton")
                .semantics { contentDescription = "Haptic mode: ${controller.hapticModeLabel}. Double tap to change." },
            colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = OnDark),
        ) { Text("Haptic mode: ${controller.hapticModeLabel}", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

/**
 * Target size of the optional RGB capture handed to a real model (downscaled, in-process only).
 * Matches the bundled YOLOv8n detector's 320×320 input so no second resize/upscale is needed.
 */
private const val RGB_CAPTURE = 320

/**
 * Compute left/center/right and average luminance from the Y plane. When [capturePixels] is true
 * (only while a real model is loaded), also produce a small downscaled RGB buffer for inference.
 * No pixels are stored or transmitted — the RGB buffer lives only for the lifetime of the frame.
 */
private fun ImageProxy.toFrameInput(capturePixels: Boolean): FrameInput {
    val plane = planes[0]
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val w = width
    val h = height
    val third = (w / 3).coerceAtLeast(1)

    var leftSum = 0L; var leftN = 0L
    var midSum = 0L; var midN = 0L
    var rightSum = 0L; var rightN = 0L

    val rowStep = (h / 48).coerceAtLeast(1)
    val colStep = (w / 64).coerceAtLeast(1)
    var row = 0
    while (row < h) {
        val rowBase = row * rowStride
        var col = 0
        while (col < w) {
            val idx = rowBase + col * pixelStride
            if (idx < buffer.capacity()) {
                val luma = (buffer.get(idx).toInt() and 0xFF).toLong()
                when {
                    col < third -> { leftSum += luma; leftN++ }
                    col < third * 2 -> { midSum += luma; midN++ }
                    else -> { rightSum += luma; rightN++ }
                }
            }
            col += colStep
        }
        row += rowStep
    }

    val left = if (leftN > 0) leftSum.toFloat() / leftN else 0f
    val mid = if (midN > 0) midSum.toFloat() / midN else 0f
    val right = if (rightN > 0) rightSum.toFloat() / rightN else 0f
    val total = leftN + midN + rightN
    val avg = if (total > 0) (leftSum + midSum + rightSum).toFloat() / total else 0f

    val rgb = if (capturePixels) toRgbFloats(RGB_CAPTURE, RGB_CAPTURE) else null
    return FrameInput(w, h, left, mid, right, avg, rgb, if (rgb != null) RGB_CAPTURE else 0, if (rgb != null) RGB_CAPTURE else 0)
}

/**
 * Convert this YUV_420_888 frame to an interleaved RGB float buffer (0..1) downscaled to
 * [outW]x[outH] via nearest-neighbor sampling, using the standard BT.601 YUV→RGB transform.
 * Returns null on any plane/format surprise (caller falls back to no detections, never crashes).
 */
/**
 * Convert this YUV_420_888 frame to an upright ARGB [Bitmap] (BT.601) for on-demand OCR. Applies
 * the frame's rotation so text is upright. In-process only; the bitmap is recycled after OCR.
 */
private fun ImageProxy.toBitmap(): Bitmap {
    val w = width
    val h = height
    val yP = planes[0]; val uP = planes[1]; val vP = planes[2]
    val yBuf = yP.buffer; val uBuf = uP.buffer; val vBuf = vP.buffer
    val argb = IntArray(w * h)
    for (j in 0 until h) {
        for (i in 0 until w) {
            val yIdx = j * yP.rowStride + i * yP.pixelStride
            val uvIdx = (j / 2) * uP.rowStride + (i / 2) * uP.pixelStride
            val y = if (yIdx < yBuf.capacity()) yBuf.get(yIdx).toInt() and 0xFF else 0
            val u = if (uvIdx < uBuf.capacity()) (uBuf.get(uvIdx).toInt() and 0xFF) - 128 else 0
            val v = if (uvIdx < vBuf.capacity()) (vBuf.get(uvIdx).toInt() and 0xFF) - 128 else 0
            val r = (y + 1.402f * v).toInt().coerceIn(0, 255)
            val g = (y - 0.344f * u - 0.714f * v).toInt().coerceIn(0, 255)
            val b = (y + 1.772f * u).toInt().coerceIn(0, 255)
            argb[j * w + i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    val bmp = Bitmap.createBitmap(argb, w, h, Bitmap.Config.ARGB_8888)
    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return bmp
    val m = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bmp, 0, 0, w, h, m, true)
}

private fun ImageProxy.toRgbFloats(outW: Int, outH: Int): FloatArray? = try {
    val yP = planes[0]; val uP = planes[1]; val vP = planes[2]
    val yBuf = yP.buffer; val uBuf = uP.buffer; val vBuf = vP.buffer
    val out = FloatArray(outW * outH * 3)
    for (oy in 0 until outH) {
        val sy = oy * height / outH
        for (ox in 0 until outW) {
            val sx = ox * width / outW
            val yIdx = sy * yP.rowStride + sx * yP.pixelStride
            val uvRow = (sy / 2) * uP.rowStride
            val uvCol = (sx / 2) * uP.pixelStride
            val y = if (yIdx < yBuf.capacity()) (yBuf.get(yIdx).toInt() and 0xFF) else 0
            val u = if (uvRow + uvCol < uBuf.capacity()) (uBuf.get(uvRow + uvCol).toInt() and 0xFF) - 128 else 0
            val v = if (uvRow + uvCol < vBuf.capacity()) (vBuf.get(uvRow + uvCol).toInt() and 0xFF) - 128 else 0
            val r = (y + 1.402f * v).coerceIn(0f, 255f)
            val g = (y - 0.344f * u - 0.714f * v).coerceIn(0f, 255f)
            val b = (y + 1.772f * u).coerceIn(0f, 255f)
            val o = (oy * outW + ox) * 3
            out[o] = r / 255f; out[o + 1] = g / 255f; out[o + 2] = b / 255f
        }
    }
    out
} catch (e: Exception) {
    Log.e("OBSERVA_CAMERA", "RGB capture failed", e)
    null
}
