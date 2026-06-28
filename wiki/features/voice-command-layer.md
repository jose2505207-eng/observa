---
status: current
confidence: high
last_updated: 2026-06-28
owner: jose2505207-eng
---

# Voice Command Layer

> Hands-free control of the whole app. Voice is a first-class input for non-visual use ([[accessibility-principles]]).

## Current Reality
Implemented and device-verified ("Voice: On-device ready" on the S25 Ultra). As of **v3.1.0, every
feature is reachable by voice.**
- **Activation:** **Volume-up ×3** (`HotkeyCommand.VOICE_COMMANDS`) or the **Voice Command** button opens voice control. Push-to-talk via `PushToTalkController`.
- **ASR:** `OfflineSpeechRecognizer` wraps Android `SpeechRecognizer`, prefers the on-device recognizer (`createOnDeviceSpeechRecognizer`, `EXTRA_PREFER_OFFLINE`). Falls back to on-screen controls when unavailable.
- **Parser:** deterministic offline `VoiceCommandParser` + `CommandRouter` → `CommandActions` (low-confidence confirmation; Stop/Cancel/Mute/Braille always available). Commands: start/stop observing, describe scene, what is ahead, read text / **read signs**, repeat, mute/unmute, braille on/off/status, **start/stop navigation, navigate to <place>, where am I**, **start/stop translation**, **download map**, **download <language>** (any of ~45 via `LanguageCatalog` name→code), help. **Stop-variants are parsed before start-variants** so "stop navigation" is never misread as "start". Unbuilt features and missing packs decline honestly.
- **Voice-to-voice translation:** "start translation" runs `LiveVoiceTranslator` (listen → ML Kit translate offline → `Speaker.speakIn` target language → loop); see [[offline-translation]].
- **TTS:** `Speaker` (Android TextToSpeech) is the primary output channel; hazards flush the queue (barge-in / interruptible). `Speaker.speakIn(lang, …)` speaks in a target language for translation.
- A known parser bug was fixed + regression-tested: "unmute" no longer matched "mute". Coverage in `CommandRouterTest`, `NewCommandsTest`, `LanguageCatalogTest`.

## Future Vision
- Intent routing to more Tier 2 features ([[conversational-vision]]) and to permissioned skills ([[mcp-skill-system]]).
- Swap in a fully offline ASR engine if the platform recognizer is unavailable on a target device.

## Design notes
- Core command set works offline ([[offline-first-design]]).
- Keep the command vocabulary small, consistent, and forgiving.

## Open questions
- On-device ASR/TTS engine choice and language coverage.
- Wake-word vs push-to-talk vs gesture activation.

## Related
- [[spatial-guidance]] · [[mcp-skill-system]]
