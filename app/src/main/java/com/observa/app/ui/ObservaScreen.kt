package com.observa.app.ui

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.observa.app.ObservaController
import com.observa.app.hazard.FrameInput
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

private val Bg = Color(0xFF000000)
private val Panel = Color(0xFF101418)
private val Accent = Color(0xFFFFEB3B)      // high-contrast yellow
private val OnDark = Color(0xFFFFFFFF)
private val Good = Color(0xFF69F0AE)

@Composable
fun ObservaScreen(controller: ObservaController) {
    LaunchedEffect(Unit) { controller.runProcessingLoop() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .safeDrawingPadding() // keep content clear of status bar + gesture nav bar
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

        CameraPanel(controller, modifier = Modifier.weight(1f).fillMaxWidth())

        AlertBanner(controller.lastAlert)
        BrailleStatus(controller.brailleStatus)
        Dashboard(controller)
        Controls(controller)
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
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Status: $status"
            },
    ) {
        Text(text = status, color = Good, fontSize = 16.sp)
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
                contentDescription = if (controller.observing)
                    "Observing on. Double tap to stop observing."
                else
                    "Observing off. Double tap to start observing."
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Observing", color = OnDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Switch(checked = controller.observing, onCheckedChange = { controller.observe(it) })
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
    Spacer(Modifier.height(4.dp))
}

/** Target size of the optional RGB capture handed to a real model (downscaled, in-process only). */
private const val RGB_CAPTURE = 256

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
