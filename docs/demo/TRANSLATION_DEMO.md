# Offline Translation Mode — demo

Honest by design: OBSERVA never fakes a translation and never uses the network at runtime. This demo
shows the **readiness gate** and truthful status.

## Demo (current build — no pack provisioned)
1. Launch OBSERVA. TalkBack on → **Available actions** node shows "translation (not installed)".
2. Actions menu → **Start translation mode**.
3. Hear: *"Translation needs an offline language pack, which is not installed. OBSERVA never translates
   over the network."*
4. Open debug / status confirms one of:
   - `Translation language pack missing`
   - `Local speech recognition unavailable`
   - `Translation ready offline` (only once a pack is provisioned)

This proves the mode is wired and honest — not a fake screen.

## Provisioning a pack (offline-after-install)
A pack may need a one-time download, but **runtime stays offline**. Drop a non-empty pack dir:
```
adb shell mkdir -p /data/data/com.observa.app/files/translation_packs/es-en
adb push <local_model_files> /data/data/com.observa.app/files/translation_packs/es-en/
```
(or use a dedicated provisioning/debug flavor). Restart the app → status flips to "Translation ready
offline". No INTERNET permission is added to the runtime app.

## Not yet built (honest)
The local translation engine, language identifier, and turn manager are deferred — bundling an engine
like ML Kit Translate would add an `INTERNET`-declaring dependency, which breaks the no-INTERNET
guarantee. See `docs/implementation/OFFLINE_TRANSLATION.md`.
