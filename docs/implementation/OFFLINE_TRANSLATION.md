# Offline Translation Mode

**Status: honest readiness gate implemented — no fake translation, no network.** Real two-way
translation is gated on an offline language pack + local speech, neither of which is bundled in the
no-INTERNET runtime build, so the mode reports an honest unavailable state today.

## Design rule
OBSERVA must never fabricate a translation and never use the cloud at runtime. Therefore Translation
Mode is a **readiness gate**, not a stub that pretends to work. It turns on only when both are true:
1. an **offline language pack** is installed locally, AND
2. **local (offline) speech recognition** is available.

## Honest status (always truthful)
`TranslationModeController.statusLine()` returns exactly one of:
- `Translation ready offline` (pack + speech present) → `Translation active (offline)` once started,
- `Translation language pack missing`,
- `Local speech recognition unavailable`.

`start()` never activates unless ready; otherwise it explains what is missing and states that OBSERVA
never translates over the network.

## Code map
| Concern | Where |
|---|---|
| Readiness + status + start/stop (new, pure, tested) | `translation/TranslationModeController.kt` |
| Offline pack detection (new) | `translation/OfflineLanguagePackManager.kt` (scans `filesDir/translation_packs/`) |
| Local speech availability | reuses `voice/OfflineSpeechRecognizer` (`recognizer.available`) |

## Provisioning (offline-after-install)
A language pack may require a **one-time** connection to download, but **runtime is offline**. To keep
the demo/runtime app strictly no-INTERNET, packs are provisioned out of band into
`filesDir/translation_packs/<lang-pair>/` (e.g. via a separate provisioning/debug flavor or `adb push`).
Once a non-empty pack dir is present, `OfflineLanguagePackManager.hasAnyPack` flips true and the status
reports "ready offline". No runtime code or permission change is needed.

## Not yet built (honest)
A bundled local translation engine (`LocalTranslator`), language identifier, and turn manager are not
implemented — adding an engine such as ML Kit Translate would pull an `INTERNET`-declaring dependency,
which violates the no-INTERNET runtime guarantee, so it is intentionally deferred to a provisioning
flavor. The current build ships the readiness gate + honest status + actions only.

## Accessibility
Operating-layer action **Start translation mode** speaks the honest status; the status node shows
"translation (not installed)" until a pack is provisioned. `Stop translation` / `Repeat translation`
are available on the controller.

## Tests
`TranslationModeControllerTest` (3): no-pack honesty (never activates), no-speech honesty, ready→active→stop.
