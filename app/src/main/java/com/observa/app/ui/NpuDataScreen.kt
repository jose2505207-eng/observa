package com.observa.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.observa.app.ObservaController
import com.observa.app.inference.NpuUsageSnapshot
import kotlinx.coroutines.delay

private val Bg = Color(0xFF000000)
private val Panel = Color(0xFF101418)
private val Accent = Color(0xFFFFEB3B)
private val OnDark = Color(0xFFFFFFFF)
private val Good = Color(0xFF69F0AE)
private val Grid = Color(0xFF2A2F36)

/**
 * NPU Data — a live graph of detector inference latency (ms) with throughput + backend. Android does
 * not expose a DSP/HTP utilization percentage to a third-party app, so this charts the real, measurable
 * signals (per-inference latency + inferences/sec), never a fabricated "%". Updates live by polling the
 * tracker snapshot. The graph itself isn't readable by TalkBack, so a focusable summary node + a
 * "Speak NPU usage" action carry the same numbers accessibly.
 */
@Composable
fun NpuDataScreen(controller: ObservaController, onBack: () -> Unit) {
    var snap by remember { mutableStateOf(controller.npuUsageSnapshot()) }
    LaunchedEffect(Unit) {
        while (true) { snap = controller.npuUsageSnapshot(); delay(250) }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("NPU Data", onBack)

        // Accessible, on-demand summary (NOT a live region — would spam TalkBack at the live rate).
        Text(
            snap.summaryLine(),
            color = if (snap.npuActive) Good else Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().testTag("npuUsageSummary")
                .semantics { contentDescription = "NPU usage: ${snap.summaryLine()}" },
        )

        Text("Inference latency (ms), live — lower is faster. Backend: ${snap.backendLabel}.",
            color = OnDark, fontSize = 13.sp)

        LatencyGraph(snap, modifier = Modifier.fillMaxWidth().height(200.dp).testTag("npuGraph"))

        // Numeric stat rows (each TalkBack-readable).
        StatLine("Backend", "${snap.backendLabel}${if (snap.npuActive) " (NPU active)" else ""}")
        StatLine("Now", "${snap.currentMs.toInt()} ms")
        StatLine("Average", "${snap.avgMs.toInt()} ms")
        StatLine("Min / Max", "${snap.minMs.toInt()} / ${snap.maxMs.toInt()} ms")
        StatLine("Throughput", "${snap.ratePerSec} inferences/sec")
        StatLine("Total inferences", snap.total.toString())

        WideButton("Speak NPU Usage", "Speak the current NPU usage aloud.", Accent) { controller.announceNpuUsage() }

        Text("Note: Android exposes no NPU utilization percentage to apps; this charts real measured " +
            "latency and throughput, never a fabricated percentage.", color = Grid.copy(alpha = 1f),
            fontSize = 11.sp, modifier = Modifier.semantics {
                contentDescription = "Note: NPU utilization percentage is not exposed by Android; this shows measured latency and throughput."
            })
    }
}

/** Canvas line graph of recent latencies, scaled to the window's own max (min 5 ms ceiling). */
@Composable
private fun LatencyGraph(snap: NpuUsageSnapshot, modifier: Modifier) {
    val lineColor = if (snap.npuActive) Good else Accent
    Canvas(modifier = modifier.background(Panel, RoundedCornerShape(12.dp)).padding(8.dp)
        .semantics { contentDescription = "Latency graph. Current ${snap.currentMs.toInt()} milliseconds, max ${snap.maxMs.toInt()}." }) {
        val w = size.width
        val h = size.height
        // Horizontal gridlines (quarters).
        for (i in 0..4) {
            val y = h * i / 4f
            drawLine(Grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        val s = snap.samples
        if (s.size < 2) return@Canvas
        val ceil = maxOf(snap.maxMs, 5f) * 1.1f // headroom; never divide by ~0
        val stepX = w / (s.size - 1).toFloat()
        for (i in 0 until s.size - 1) {
            val x1 = stepX * i
            val x2 = stepX * (i + 1)
            val y1 = h - (s[i] / ceil) * h
            val y2 = h - (s[i + 1] / ceil) * h
            drawLine(lineColor, Offset(x1, y1), Offset(x2, y2), strokeWidth = 4f)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(8.dp)).padding(12.dp)
            .semantics { contentDescription = "$label: $value" },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = OnDark, fontSize = 15.sp)
        Text(value, color = Good, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
