---
status: current
confidence: high
last_updated: 2026-06-29
owner: jose2505207-eng
---

# Offline Translation

> Real on-device translation with a visible **Translate** / **Download Languages** button and
> TalkBack/braille/voice actions. Download a language pack once, then translate **fully offline** —
> including **real-time voice-to-voice** translation. Text translation shipped in **v2.5.0**;
> voice-to-voice and ~45-language download by voice in **v3.1.0**.

## Current Reality
- **Real ML Kit on-device Translation** (`com.google.mlkit:translate`). `translation/`:
  `MlKitOnDeviceTranslator` (download / translate / delete), `LanguageDownloadController` (state +
  readiness + `setTarget`), `TranslationReadiness`, `LanguageCatalog` (~45 languages, name→code), plus
  `ui/LanguageDownloadScreen` (source/target, download, delete, a text field, Translate, result).
- **Voice-to-voice (v3.1.0):** `LiveVoiceTranslator` loops listen → ML Kit translate offline →
  `Speaker.speakIn(target)` → repeat (`swap` for two-way). Any target language via `setTarget` + the
  catalog. Triggered by **"start translation"** / **"stop translation"** ([[voice-command-layer]]).
  Honest about a missing language pack or a missing target-language TTS voice.
- **Flow:** in the **provisioning** build (has INTERNET) tap **Download Languages** or say
  *"download <language>"* → download the model(s) once. Switch to the **demoOffline** build (no
  INTERNET) or Airplane Mode — same `applicationId`, so the models persist — and **Translate** /
  voice-to-voice run fully offline. Test phrase *"¿Dónde está la entrada?"* → *"Where is the entrance?"*.
- **Readiness is honest:** "Translation ready offline" appears only when ML Kit reports the language
  models actually downloaded; the result is ML Kit's real output, **never fabricated**. Status:
  ready offline / downloading / language pack missing / failed.

## Honesty / not built
- **No cloud translation, ever.** Downloads only happen in the `provisioning` flavor
  (`BuildConfig.SETUP_MODE`); the `demoOffline` flavor (no INTERNET) translates installed models and
  says so when a download is requested.
- **Voice-to-voice** depends on the on-device recognizer and a target-language TTS voice; when either is
  unavailable the app says so and **text translation still works regardless of speech**. Spoken output
  uses the existing on-device TTS (`Speaker.speakIn`).

## Related
- [[offline-first-design]] · [[privacy-model]] · [[voice-command-layer]] · [[offline-navigation]]
- Detail: `docs/implementation/OFFLINE_TRANSLATION.md`
