---
status: current
confidence: high
last_updated: 2026-06-28
owner: jose2505207-eng
---

# OCR Mode (Tier 2)

> On-demand text reading — labels, signs, mail, documents — optionally translated. A Tier 2 capability ([[two-tier-inference]]).

## Current Reality
On-demand offline OCR works (read path), device-verified in Airplane Mode.
- Engine: ML Kit **bundled** Latin text recognition (`com.google.mlkit:text-recognition`) — the model ships inside the APK and runs fully on device. `OcrEngine`/`MlKitOcrEngine` + pure `OcrFormatter`.
- Trigger on demand only (never per-frame): the **Read Text** button, the voice command "read text", and the TalkBack-actionable button. A one-shot bitmap is captured from the analyzer (YUV→ARGB, rotated upright), recognized off the UI thread.
- Result is spoken (TTS), shown in the Braille/live-region status, and stored for "repeat"; empty result says **"No readable text found."**
- **No network:** `INTERNET` and `ACCESS_NETWORK_STATE` are stripped from the merged manifest, so ML Kit cannot use the network (verified `aapt2 dump permissions`). Translation is **not** implemented.

## Future Vision
- **Translation:** optionally translate recognized text on-device into the user's language (a core OBSERVA direction).
- Document mode: handle multi-line/structured text (paragraphs, lists) for longer reading.
- [[spatial-guidance]] to help aim at text before capture.

## Design notes
- On-demand and heavier — fine to use a larger model since it runs briefly and must not block Tier 1.
- Must work offline for the core read path ([[offline-first-design]]); translation models bundled on-device where feasible.

## Open questions
- OCR engine/model choice and offline translation model ([[model-selection]]).
- Layout handling for dense documents.

## Related
- [[conversational-vision]] · [[ambient-awareness]]
