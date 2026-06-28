package com.observa.app.translation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.observa.app.BuildConfig

/**
 * Drives the Language Download screen and feeds the translation readiness gate. Real on-device ML Kit
 * translation: a language model downloads **once** (only in the provisioning build, which has INTERNET)
 * and then translates **fully offline**. Never fakes "ready" — readiness comes from ML Kit's actual set
 * of downloaded models. Compose-observable; main-thread.
 */
class LanguageDownloadController(private val engine: MlKitOnDeviceTranslator = MlKitOnDeviceTranslator()) {

    var sourceLang by mutableStateOf("es"); private set
    var targetLang by mutableStateOf("en"); private set
    var installed by mutableStateOf<Set<String>>(emptySet()); private set
    var downloading by mutableStateOf(false); private set
    var failed by mutableStateOf(false); private set
    var detail by mutableStateOf(""); private set
    var lastTranslation by mutableStateOf(""); private set

    /** True in the provisioning/setup build (INTERNET present); false in the demoOffline build. */
    val setupMode: Boolean get() = BuildConfig.SETUP_MODE

    fun setPair(source: String, target: String) { sourceLang = source; targetLang = target; failed = false }
    /** Set the target language (any ML Kit code); keeps the current source. */
    fun setTarget(target: String) { targetLang = target; failed = false; refresh() }
    /** Set the source language (any ML Kit code). */
    fun setSource(source: String) { sourceLang = source; failed = false; refresh() }
    /** Swap source/target (for two-way / reverse translation). */
    fun swap() { val t = sourceLang; sourceLang = targetLang; targetLang = t }

    fun pairReady(): Boolean = installed.contains(sourceLang) && installed.contains(targetLang)

    fun readiness(): TranslationReadiness =
        TranslationReadinessRules.of(downloading, failed, pairReady())

    fun statusLine(): String = readiness().label

    /** Short braille line, e.g. "Spanish English ready" / "Language missing". */
    fun brailleLine(): String =
        if (pairReady()) "${name(sourceLang)} ${name(targetLang)} ready" else "Language missing"

    /** Refresh the installed-model set from ML Kit (works offline). */
    fun refresh() = engine.downloadedLanguages { installed = it }

    /** Download both languages of the current pair. Only the provisioning build can reach the network. */
    fun downloadSelected() {
        if (!setupMode) { failed = true; detail = "Use the Setup / Download build (provisioning) to download. Runtime is offline after."; return }
        downloading = true; failed = false; detail = "Downloading ${name(sourceLang)} and ${name(targetLang)}…"
        var remaining = 2; var anyFail = false
        val done = { ok: Boolean, msg: String ->
            if (!ok) { anyFail = true; detail = msg }
            remaining--
            if (remaining == 0) { downloading = false; failed = anyFail; refresh() }
        }
        engine.downloadLanguage(sourceLang) { ok, msg -> done(ok, msg) }
        engine.downloadLanguage(targetLang) { ok, msg -> done(ok, msg) }
    }

    fun deleteSelected() {
        engine.deleteLanguage(sourceLang) { refresh() }
        engine.deleteLanguage(targetLang) { refresh() }
        detail = "Deleting ${name(sourceLang)}/${name(targetLang)}…"
    }

    /** Translate a phrase offline (no-op message if the pack isn't installed — never fakes). */
    fun translate(text: String, onResult: (String) -> Unit = {}) {
        if (!pairReady()) { detail = "Language pack missing. Download it first."; onResult(detail); return }
        engine.translate(text, sourceLang, targetLang) { ok, out ->
            lastTranslation = if (ok) out else ""
            detail = if (ok) "“$text” → “$out”" else out
            onResult(if (ok) out else detail)
        }
    }

    private fun name(code: String): String = when (code) {
        "en" -> "English"; "es" -> "Spanish"; "fr" -> "French"; "de" -> "German"; else -> code
    }

    companion object {
        /** Demo pair + test phrase. */
        const val DEMO_PHRASE = "Where is the entrance?"
    }
}
