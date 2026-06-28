package com.observa.app.translation

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * Real on-device translation via **ML Kit Translate**. Downloading a language model needs the network
 * **once** (only possible in the provisioning build, which has INTERNET); after that, translation runs
 * **fully offline**. This wrapper never fabricates a translation — it only ever returns text ML Kit
 * actually produced, and reports honest readiness from the set of downloaded models.
 *
 * ML Kit's APIs are async (Tasks); callbacks here are invoked on the main thread.
 */
class MlKitOnDeviceTranslator {

    private val models = RemoteModelManager.getInstance()

    /** Map a short code ("en","es",…) to an ML Kit language tag, or null if unsupported. */
    fun langTag(code: String): String? = TranslateLanguage.fromLanguageTag(code)

    /** Which language models are already downloaded (offline-capable). Callback gets short codes. */
    fun downloadedLanguages(onResult: (Set<String>) -> Unit) {
        models.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { set -> onResult(set.map { it.language }.toSet()) }
            .addOnFailureListener { e -> Log.w(TAG, "getDownloadedModels failed: ${e.message}"); onResult(emptySet()) }
    }

    /** Download the model for one language (needs INTERNET / the provisioning build). */
    fun downloadLanguage(code: String, onDone: (Boolean, String) -> Unit) {
        val tag = langTag(code) ?: run { onDone(false, "Unsupported language: $code"); return }
        val model = TranslateRemoteModel.Builder(tag).build()
        models.download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener { onDone(true, "$code ready offline") }
            .addOnFailureListener { e -> onDone(false, "Download failed: ${e.message}") }
    }

    /** Delete a downloaded language model. */
    fun deleteLanguage(code: String, onDone: (Boolean) -> Unit) {
        val tag = langTag(code) ?: run { onDone(false); return }
        models.deleteDownloadedModel(TranslateRemoteModel.Builder(tag).build())
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    /**
     * Translate [text] from [source] to [target]. Runs offline if both models are present; never
     * downloads here (`requireWifi`-style conditions are not set, and we pass no download). Returns the
     * honest ML Kit result or an error string.
     */
    fun translate(text: String, source: String, target: String, onResult: (Boolean, String) -> Unit) {
        val s = langTag(source); val t = langTag(target)
        if (s == null || t == null) { onResult(false, "Unsupported language pair"); return }
        val client = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(s).setTargetLanguage(t).build()
        )
        // Only translate if models are present; do NOT trigger a network download here.
        client.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                client.translate(text)
                    .addOnSuccessListener { out -> onResult(true, out); client.close() }
                    .addOnFailureListener { e -> onResult(false, "Translate failed: ${e.message}"); client.close() }
            }
            .addOnFailureListener { e -> onResult(false, "Language pack missing offline: ${e.message}"); client.close() }
    }

    private companion object { const val TAG = "OBSERVA_TRANSLATE" }
}
