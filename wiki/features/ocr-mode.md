---
status: planned
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# OCR Mode (Tier 2)

> On-demand text reading — labels, signs, mail, documents — optionally translated. A Tier 2 capability ([[two-tier-inference]]).

## Current Reality
Not implemented. No OCR, no text extraction.

## Future Vision
- User triggers OCR (voice/gesture); [[spatial-guidance]] helps aim if needed.
- Run on-device text detection + recognition, read results aloud, prioritized and de-cluttered.
- **Translation:** optionally translate recognized text on-device into the user's language (a core OBSERVA direction).
- Document mode: handle multi-line/structured text (paragraphs, lists) for longer reading.

## Design notes
- On-demand and heavier — fine to use a larger model since it runs briefly and must not block Tier 1.
- Must work offline for the core read path ([[offline-first-design]]); translation models bundled on-device where feasible.

## Open questions
- OCR engine/model choice and offline translation model ([[model-selection]]).
- Layout handling for dense documents.

## Related
- [[conversational-vision]] · [[ambient-awareness]]
