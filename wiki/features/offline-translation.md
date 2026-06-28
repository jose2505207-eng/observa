---
status: current
confidence: high
last_updated: 2026-06-29
owner: jose2505207-eng
---

# Offline Translation

> Real on-device, two-language translation with a visible **Translate** / **Download Languages** button
> and TalkBack/braille actions. Download a language pack once, then translate **fully offline**. Shipped
> in **v2.5.0**.

## Current Reality
- **Real ML Kit on-device Translation** (`com.google.mlkit:translate`). `translation/`:
  `MlKitOnDeviceTranslator` (download / translate / delete), `LanguageDownloadController` (state +
  readiness), `TranslationReadiness`, plus `ui/LanguageDownloadScreen` (source/target, download, delete,
  a text field, Translate, result, honest voice-input line).
- **Flow:** in the **provisioning** build (has INTERNET) tap **Download Languages** → download
  Spanish + English models once. Switch to the **demoOffline** build (no INTERNET) or Airplane Mode —
  same `applicationId`, so the models persist — and **Translate** runs fully offline. Test phrase
  *"¿Dónde está la entrada?"* → *"Where is the entrance?"*.
- **Readiness is honest:** "Translation ready offline" appears only when ML Kit reports both language
  models actually downloaded; the result is ML Kit's real output, **never fabricated**. Status:
  ready offline / downloading / language pack missing / failed.

## Honesty / not built
- **No cloud translation, ever.** Downloads only happen in the `provisioning` flavor
  (`BuildConfig.SETUP_MODE`); the `demoOffline` flavor (no INTERNET) translates installed models and
  says so when a download is requested.
- **Voice input** is shown separately ("available" / "unavailable offline"); **text translation works
  regardless of speech**. Spoken output uses the existing on-device TTS.

## Related
- [[offline-first-design]] · [[privacy-model]] · [[voice-command-layer]] · [[offline-navigation]]
- Detail: `docs/implementation/OFFLINE_TRANSLATION.md`
