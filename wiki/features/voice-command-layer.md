---
status: current
confidence: high
last_updated: 2026-06-28
owner: jose2505207-eng
---

# Voice Command Layer

> Hands-free control of the whole app. Voice is a first-class input for non-visual use ([[accessibility-principles]]).

## Current Reality
Implemented and device-verified ("Voice: On-device ready" on the S25 Ultra).
- **ASR:** `OfflineSpeechRecognizer` wraps Android `SpeechRecognizer`, prefers the on-device recognizer (`createOnDeviceSpeechRecognizer`, `EXTRA_PREFER_OFFLINE`); push-to-talk via the **Voice Command** button (`PushToTalkController`). Falls back to on-screen controls when unavailable.
- **Parser:** deterministic offline `VoiceCommandParser` + `CommandRouter` (low-confidence confirmation; Stop/Cancel/Mute/Braille always available). Commands: start/stop observing, describe scene, what is ahead, read text, repeat, mute/unmute, braille on/off/status, **navigate to <place>, stop navigation, where am I**, help. Unbuilt features decline honestly.
- **TTS:** `Speaker` (Android TextToSpeech) is the primary output channel; hazards flush the queue (barge-in / interruptible).
- A known parser bug was fixed + regression-tested: "unmute" no longer matched "mute".

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
