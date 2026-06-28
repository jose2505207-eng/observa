# Offline Translation Mode

## v2.5.0 — real on-device translation + visible language download

Translation is now a **real, usable** feature via **ML Kit on-device Translation**, with a visible
**Download Languages** button and screen.

**How to use (Spanish ↔ English default):**
1. Install the **provisioning** build (has INTERNET): `./gradlew assembleProvisioningDebug` →
   `adb install -r app/build/outputs/apk/provisioning/debug/app-provisioning-debug.apk`.
2. Open **Download Languages** → **Download Spanish + English** (one-time network download via ML Kit).
3. Switch to the **demoOffline** build (no INTERNET) or enable Airplane Mode:
   `adb install -r app/build/outputs/apk/demoOffline/debug/app-demoOffline-debug.apk` (same
   applicationId → the downloaded models persist).
4. Open **Translate** / the Language screen → type Spanish (e.g. "¿Dónde está la entrada?") → **Translate**
   → real English output, **fully offline**.

**Honesty:** "Translation ready offline" appears only when ML Kit reports both language models actually
downloaded. The result is ML Kit's real output — never fabricated. Downloads only run in the
provisioning build (`BuildConfig.SETUP_MODE`); the demoOffline build translates installed models but
cannot download (no INTERNET) and says so. Voice input availability is shown separately; **text
translation works regardless of speech availability**. No cloud translation, ever.

**Code:** `translation/MlKitOnDeviceTranslator.kt` (download/translate/delete via ML Kit),
`translation/LanguageDownloadController.kt` (state + readiness), `translation/TranslationReadiness.kt`,
`ui/LanguageDownloadScreen.kt`. Build flavors in `app/build.gradle.kts`; INTERNET split in
`src/demoOffline/AndroidManifest.xml` (strips) vs `src/provisioning/AndroidManifest.xml` (adds).

---

# Offline Translation Mode (original readiness-gate design)

**Status: honest readiness gate + full offline pipeline scaffold implemented — no fake translation, no
network.** Real two-way translation is gated on an offline language pack + local speech + a local
translation engine; none of the three is bundled in the no-INTERNET runtime build, so the mode reports
an honest unavailable state today.

## Design rule
OBSERVA must never fabricate a translation and never use the cloud at runtime. Therefore Translation
Mode is a **readiness gate** over a real offline pipeline, not a stub that pretends to work. It turns on
only when **all three** are true:
1. an **offline language pack** is installed locally,
2. **local (offline) speech recognition** is available, AND
3. a **local translation engine** is present.

## Honest status (always truthful)
`TranslationModeController.statusLine()` returns exactly one of:
- `Translation ready offline` (all three present) → `Translation active (offline)` once started,
- `Translation language pack missing`,
- `Local speech recognition unavailable`,
- `Translation engine not installed` (the runtime default — no offline engine is bundled).

`start()` never activates unless ready; otherwise it explains what is missing and states that OBSERVA
never translates over the network.

## Offline pipeline (the named modules — honest boundaries)
A complete turn is **recognized speech → identify source language → translate locally → speak**. Each
stage is a boundary that fails *honestly* (returns Unavailable, never a guess), so the whole turn can
never fabricate text:

| Stage | Module | Honest default (no engine bundled) |
|---|---|---|
| Readiness + status + start/stop/onUtterance | `translation/TranslationModeController.kt` | reports `NO_ENGINE` |
| Offline pack detection | `translation/OfflineLanguagePackManager.kt` (scans `filesDir/translation_packs/`) | empty → no pack |
| On-device speech gate | `translation/LocalSpeechRecognizer.kt` (probes `voice/OfflineSpeechRecognizer`) | available if device has on-device ASR |
| Source-language ID | `translation/LanguageIdentifier.kt` | no model → `Identification.Unavailable` (never guesses) |
| **Translation engine** | `translation/LocalTranslator.kt` | no engine → `TranslationResult.Unavailable` (the honesty anchor) |
| Turn orchestration | `translation/TranslationTurnManager.kt` | fails honestly, speaks the reason |
| Spoken output | `translation/TranslationSpeechOutput.kt` (reuses `output/Speaker`) | only ever voices a real translation |

A real engine/model is injected via `LocalTranslator.Engine` / `LanguageIdentifier.Model` — both
interfaces are documented to never touch the network. None is wired in the runtime build.

## Provisioning (offline-after-install)
A language pack / engine may require a **one-time** connection to download, but **runtime is offline**.
To keep the demo/runtime app strictly no-INTERNET, assets are provisioned out of band into
`filesDir/translation_packs/<lang-pair>/` (e.g. via a separate provisioning/debug flavor or `adb push`)
and the engine is supplied by that flavor. Adding an in-app engine such as ML Kit Translate would pull
an `INTERNET`-declaring dependency, which violates the no-INTERNET runtime guarantee, so it is
intentionally deferred to that flavor. Once provisioned, no runtime code or permission change is needed.

## Accessibility
Operating-layer action **Start translation mode** speaks the honest status; the status node shows
"translation (not installed)" until a pack is provisioned. `Stop translation` / `Repeat translation`
are available on the controller.

## Tests
- `TranslationModeControllerTest` (4): no-pack / no-speech / **no-engine** honesty (never activates),
  ready→active→stop.
- `TranslationPipelineTest` (4): translator-without-engine is Unavailable, identifier-without-model
  never guesses, a turn with no engine fails honestly and speaks the reason, and a turn with a real
  (fake) engine translates and speaks — proving the pipeline is real, only the assets are deferred.
