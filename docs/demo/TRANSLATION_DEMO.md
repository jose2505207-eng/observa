# Offline Translation Mode — demo

Honest by design: OBSERVA never fakes a translation and never uses the network at runtime. This demo
shows the **readiness gate** and truthful status.

## Demo (current build — no pack provisioned)
1. Launch OBSERVA. TalkBack on → **Available actions** node shows "translation (not installed)".
2. Actions menu → **Start translation mode**.
3. Hear an honest reason, e.g.: *"Translation needs an offline language pack, which is not installed.
   OBSERVA never translates over the network."* (or, once a pack is provisioned but no engine is:
   *"Translation needs an offline translation engine, which is not installed..."*).
4. Open debug / status confirms one of:
   - `Translation language pack missing`
   - `Local speech recognition unavailable`
   - `Translation engine not installed`
   - `Translation ready offline` (only once pack + speech + engine are all provisioned)

This proves the mode is wired and honest — not a fake screen.

## Provisioning a pack (offline-after-install)
A pack may need a one-time download, but **runtime stays offline**. Drop a non-empty pack dir:
```
adb shell mkdir -p /data/data/com.observa.app/files/translation_packs/es-en
adb push <local_model_files> /data/data/com.observa.app/files/translation_packs/es-en/
```
(or use a dedicated provisioning/debug flavor). Restart the app → status flips to "Translation ready
offline". No INTERNET permission is added to the runtime app.

## Pipeline is real — only the assets are deferred (honest)
The full offline turn pipeline is implemented as honest boundaries:
`LocalSpeechRecognizer → LanguageIdentifier → LocalTranslator → TranslationTurnManager →
TranslationSpeechOutput`. Each fails honestly (returns Unavailable, never a guess), and
`TranslationPipelineTest` proves a turn translates and speaks when a real engine is injected. What is
deferred is the **engine/model assets**: bundling an in-app engine like ML Kit Translate would add an
`INTERNET`-declaring dependency, which breaks the no-INTERNET guarantee, so the engine is supplied by a
provisioning/debug flavor instead. See `docs/implementation/OFFLINE_TRANSLATION.md`.
