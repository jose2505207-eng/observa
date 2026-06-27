---
status: planned
confidence: low
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Conversational Vision (Tier 2)

> Ask questions about the scene and get answers — "what's in front of me?", "what color is this?", "is the stove on?". An on-demand VLM capability.

## Current Reality
Not implemented. No VLM, no question answering.

## Future Vision
- User asks a question by voice ([[voice-command-layer]]).
- A vision-language model reasons over the current frame on-device and answers concisely via speech.
- Builds on [[ambient-awareness]] context and [[spatial-guidance]] for framing.

## Design notes
- VLMs are large; on-device feasibility, latency, and memory are open questions ([[performance-targets]], [[executorch-qnn]]). Status is **Research** for on-device VLM.
- Must run as Tier 2 and never block Tier 1 ([[two-tier-inference]]).
- Communicate uncertainty in answers ([[safety-principles]]); avoid confident hallucination, especially for safety-relevant questions.

## Open questions
- Which VLM is small/fast enough to run on-device, or whether this stays a cloud-optional enhancement (never part of the safety core).

## Related
- [[ocr-mode]] · [[model-selection]]
