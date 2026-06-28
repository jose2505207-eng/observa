package com.observa.app.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.role
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
 * Map Download screen. Real, honest: "Install demo map pack" writes an offline waypoint bundle (works
 * with no network); "Download area map" needs the provisioning (Setup) build. "Ready offline" is only
 * shown when a verified file exists.
 */
@Composable
fun MapDownloadScreen(controller: ObservaController, onBack: () -> Unit) {
    val maps = controller.mapDownloads
    Column(
        modifier = Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Download Map", onBack)
        Text(
            "Status: ${maps.statusLine()}",
            color = Good, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().testTag("mapStatus")
                .semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "Map pack status: ${maps.statusLine()}" },
        )
        Text(if (maps.setupMode) "Setup / Download build (internet on)" else "Offline build — demo pack works offline; area maps need the Setup build.",
            color = OnDark, fontSize = 13.sp)
        if (maps.detail.isNotBlank()) Text(maps.detail, color = OnDark, fontSize = 13.sp,
            modifier = Modifier.semantics { contentDescription = maps.detail })
        if (maps.progressPercent in 1..99) Text("Progress: ${maps.progressPercent}%", color = Accent, fontSize = 14.sp)

        WideButton("Install Demo Map Pack", "Install the offline demo map pack now. Works without internet.", Accent) { maps.installDemoPack() }
        WideButton("Download Map Of My Area", "Download real nearby places for your current GPS location. Needs the Setup build with internet.", Panel) { controller.downloadCurrentAreaMap() }

        Text("Installed packs", color = Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() })
        if (maps.installed.isEmpty()) {
            Text("None installed.", color = OnDark, fontSize = 14.sp)
        } else {
            maps.installed.forEach { pack ->
                Row(modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).padding(12.dp)
                    .semantics { contentDescription = "${pack.name}, ${if (pack.valid) "ready offline" else "corrupt"}, ${pack.sizeBytes} bytes" },
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${pack.name} (${if (pack.valid) "ready" else "corrupt"})", color = OnDark, fontSize = 14.sp)
                    Button(onClick = { maps.deletePack(pack.name) }, modifier = Modifier.height(40.dp)
                        .semantics { role = Role.Button; contentDescription = "Delete ${pack.name}" },
                        colors = ButtonDefaults.buttonColors(containerColor = Bg, contentColor = Accent)) {
                        Text("Delete", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onBack, modifier = Modifier.height(56.dp)
            .semantics { role = Role.Button; contentDescription = "Back to main screen" },
            colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = OnDark)) { Text("Back", fontSize = 16.sp) }
        Text(title, color = Accent, fontSize = 26.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() })
    }
}

@Composable
internal fun WideButton(label: String, description: String, container: Color, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(64.dp).testTag("btn_$label")
        .semantics { role = Role.Button; contentDescription = description },
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = if (container == Panel) OnDark else Bg)) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
