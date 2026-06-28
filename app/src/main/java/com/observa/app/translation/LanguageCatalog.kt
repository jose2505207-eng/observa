package com.observa.app.translation

/**
 * Maps spoken/typed language names to ML Kit BCP-47 codes (and back), so the user can download and
 * translate **any** supported language by name ("download French", "translate to Japanese"). Codes are
 * the ones ML Kit Translate accepts; the actual on-device availability is still verified by
 * [LanguageDownloadController] against ML Kit's downloaded-model set (never faked).
 */
object LanguageCatalog {

    /** name → code for the common languages, plus the code itself is always accepted. */
    private val byName: Map<String, String> = mapOf(
        "english" to "en", "spanish" to "es", "french" to "fr", "german" to "de",
        "italian" to "it", "portuguese" to "pt", "dutch" to "nl", "russian" to "ru",
        "chinese" to "zh", "mandarin" to "zh", "japanese" to "ja", "korean" to "ko",
        "arabic" to "ar", "hindi" to "hi", "bengali" to "bn", "turkish" to "tr",
        "vietnamese" to "vi", "thai" to "th", "polish" to "pl", "ukrainian" to "uk",
        "greek" to "el", "hebrew" to "he", "swedish" to "sv", "norwegian" to "no",
        "danish" to "da", "finnish" to "fi", "czech" to "cs", "romanian" to "ro",
        "hungarian" to "hu", "indonesian" to "id", "malay" to "ms", "tagalog" to "tl",
        "filipino" to "tl", "persian" to "fa", "farsi" to "fa", "urdu" to "ur",
        "tamil" to "ta", "telugu" to "te", "swahili" to "sw", "afrikaans" to "af",
        "catalan" to "ca", "croatian" to "hr", "slovak" to "sk", "bulgarian" to "bg",
    )

    private val byCode: Map<String, String> =
        byName.entries.groupBy { it.value }.mapValues { (_, e) -> e.first().key.replaceFirstChar { it.uppercase() } }

    /** Resolve a spoken name OR a code to a code, or null if unsupported. */
    fun codeFor(input: String): String? {
        val s = input.trim().lowercase()
        byName[s]?.let { return it }
        // Already a code we know about?
        if (byCode.containsKey(s)) return s
        return null
    }

    /** Human-readable name for a code (for spoken feedback). */
    fun nameFor(code: String): String = byCode[code.lowercase()] ?: code

    val supportedNames: List<String> get() = byCode.values.sorted()
}
