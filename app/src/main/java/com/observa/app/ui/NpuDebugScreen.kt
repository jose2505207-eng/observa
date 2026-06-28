package com.observa.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.observa.app.ObservaController

private val Bg = Color(0xFF000000)
private val Panel = Color(0xFF101418)
private val Accent = Color(0xFFFFEB3B)
private val OnDark = Color(0xFFFFFFFF)
private val Good = Color(0xFF69F0AE)

/**
 * NPU Debug screen — the full, accessible backend-diagnostics view. Shows build/device identity,
 * backend selection + fallback chain, the structured per-stage attempts (OBSERVA_NPU), and the exact
 * QNN blocker. Controls: force a QNN attempt / warm-up, copy the report, toggle GPU-fallback policy.
 * Never claims NPU active unless a real warm-up forward succeeded.
 */
@Composable
fun NpuDebugScreen(controller: ObservaController, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("NPU Debug", onBack)

        // Accessible one-line summary (polite live region so a status change is announced once).
        Text(controller.npuSummaryLine, color = if (controller.npuActive) Good else Accent,
            fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().testTag("npuSummary")
                .semantics { liveRegion = LiveRegionMode.Polite; contentDescription = controller.npuSummaryLine })

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WideButton("Force QNN Attempt", "Force a QNN NPU attempt and detector warm-up once.", Accent) { controller.forceQnnAttempt() }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WideButton("Copy Debug Report", "Copy the full NPU debug report to the clipboard.", Panel) {
                copyToClipboard(context, "OBSERVA NPU report", controller.npuDebugReport())
                controller.announceCopied()
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WideButton(if (controller.gpuFallbackDisabled) "GPU Fallback: Disabled" else "GPU Fallback: Allowed",
                "Toggle GPU fallback for NPU investigation. OBSERVA has no GPU path; fallback is XNNPACK CPU.", Panel) {
                controller.setDisableGpuFallback(!controller.gpuFallbackDisabled)
            }
        }

        Text("Diagnostics", color = Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() })
        controller.npuDebugLines().forEach { line ->
            Text(line, color = OnDark, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(8.dp)).padding(8.dp)
                    .semantics { contentDescription = line })
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, text))
}
