---
status: current
confidence: high
last_updated: 2026-06-28
owner: jose2505207-eng
---

# Hackathon MVP

> The smallest build that proves OBSERVA's thesis: offline, always-on awareness for blind users. Hold the line on scope ([[risks-and-mitigations]] #7).

## Done (Current Reality)
- [x] Android Compose app scaffold ([[android-stack]]).
- [x] CameraX preview + always-on `ImageAnalysis` loop with frame counter ([[ambient-awareness]]).
- [x] Runtime camera permission flow + fallback.
- [x] ExecuTorch AAR bundled **and the load/inference path is called for real** (`Module.load/forward`); honest fallback when no model ([[executorch-qnn]]).
- [x] Frame → tensor conversion (preprocessing) implemented.
- [x] Spoken/Braille/audio/haptic awareness cues via the unified router ([[accessibility-principles]]).
- [x] Whole Tier 1 path verified in **airplane mode** on S25 Ultra ([[airplane-mode-demo]]).
- [x] On-demand [[ocr-mode]] reading text offline (ML Kit bundled).
- [x] Offline [[voice-command-layer]].

## Remaining for a "real awareness cue from ML"
1. [ ] Bundle a verified `observa_detector.pte` (`scripts/export_detector.py`) so the model path emits real detections ([[model-selection]]).
2. [ ] Measure on-device latency; (stretch) lower to QNN and confirm active.
3. [ ] Physical Braille-display + human sensory pass.

## Explicitly out of scope for MVP
- Tier 2 VLM ([[conversational-vision]]), all skills ([[mcp-skill-system]]), navigation, translation.

## Definition of success
Airplane mode on → camera live → at least one real, useful spoken awareness cue, fully offline. **Met today** via the heuristic + Demo + OCR + voice; the **ML-model** cue is pending the bundled `.pte`.

## Related
- [[post-hackathon]] · [[demo-checklist]]
