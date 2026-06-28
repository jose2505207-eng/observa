package com.observa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.observa.app.ObservaController
import com.observa.app.translation.LanguageDownloadController

private val Bg = Color(0xFF000000)
private val Panel = Color(0xFF101418)
private val Accent = Color(0xFFFFEB3B)
private val OnDark = Color(0xFFFFFFFF)
private val Good = Color(0xFF69F0AE)

/**
 * Language Download + Translation screen (Phases 6–7). Real ML Kit on-device translation: download the
 * Spanish/English models once (Setup build), then translate fully offline. Never fakes a translation —
 * the result is whatever ML Kit produced, or an honest "language pack missing" / error.
 */
@Composable
fun LanguageDownloadScreen(controller: ObservaController, onBack: () -> Unit) {
    val lang = controller.languageDownloads
    var input by remember { mutableStateOf(LanguageDownloadController.DEMO_PHRASE) }
    LaunchedEffect(Unit) { controller.refreshLanguagePacks() }

    Column(
        modifier = Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Translate & Languages", onBack)

        Text("Pair: ${lang.sourceLang} to ${lang.targetLang}  (Spanish ↔ English)", color = OnDark, fontSize = 15.sp)
        Text("Status: ${lang.statusLine()}", color = Good, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().testTag("langStatus")
                .semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "Translation status: ${lang.statusLine()}" })
        Text(if (lang.setupMode) "Setup / Download build (internet on for one-time download)"
             else "Offline build — translation works once packs are installed; downloads need the Setup build.",
            color = OnDark, fontSize = 13.sp)
        if (lang.detail.isNotBlank()) Text(lang.detail, color = OnDark, fontSize = 13.sp,
            modifier = Modifier.semantics { contentDescription = lang.detail })

        Text("Installed: ${if (lang.installed.isEmpty()) "none" else lang.installed.sorted().joinToString()}",
            color = OnDark, fontSize = 13.sp)

        WideButton("Download Spanish + English", "Download the Spanish and English language packs. Needs the Setup build with internet.", Accent) { lang.downloadSelected() }
        WideButton("Delete Language Pack", "Delete the Spanish and English language packs.", Panel) { lang.deleteSelected() }

        Text("Test offline translation", color = Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() })
        OutlinedTextField(
            value = input, onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth().testTag("translateInput")
                .semantics { contentDescription = "Text to translate from Spanish to English" },
            label = { Text("Spanish text") },
        )
        WideButton("Translate", "Translate the text offline.", Accent) { controller.translateText(input) }
        if (lang.lastTranslation.isNotBlank()) {
            Text("Result: ${lang.lastTranslation}", color = Good, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().testTag("translateResult")
                    .semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "Translation result: ${lang.lastTranslation}" })
            WideButton("Speak Translation", "Speak the translated text aloud.", Panel) { controller.repeatTranslation() }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Voice input: ${if (controller.voiceAvailable) "available" else "unavailable offline"}",
                color = OnDark, fontSize = 13.sp)
        }
    }
}
