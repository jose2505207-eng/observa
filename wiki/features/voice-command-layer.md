---
status: planned
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Voice Command Layer

> Hands-free control of the whole app. Voice is a first-class input for non-visual use ([[accessibility-principles]]).

## Current Reality
Not implemented. `RECORD_AUDIO` is declared in the manifest, but there is no audio capture, speech recognition, or TTS in the code.

## Future Vision
- On-device speech recognition for commands ("read this," "what's around me," "set an alarm").
- On-device text-to-speech for all output (the primary output channel).
- Intent routing to Tier 2 features ([[ocr-mode]], [[conversational-vision]]) and to permissioned skills ([[mcp-skill-system]]).
- Barge-in / interruptible speech so the user is never stuck waiting.

## Design notes
- Core command set must work offline ([[offline-first-design]]).
- Keep the command vocabulary small, consistent, and forgiving.

## Open questions
- On-device ASR/TTS engine choice and language coverage.
- Wake-word vs push-to-talk vs gesture activation.

## Related
- [[spatial-guidance]] · [[mcp-skill-system]]
