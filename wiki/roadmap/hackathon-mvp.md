---
status: planned
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Hackathon MVP

> The smallest build that proves OBSERVA's thesis: offline, always-on awareness for blind users. Hold the line on scope ([[risks-and-mitigations]] #7).

## Done (Current Reality)
- [x] Android Compose app scaffold ([[android-stack]]).
- [x] CameraX preview + always-on `ImageAnalysis` loop with frame counter ([[ambient-awareness]]).
- [x] Runtime camera permission flow + fallback.
- [x] ExecuTorch AAR bundled (not yet called).

## MVP target (in priority order)
1. [ ] Frame → tensor conversion at the `handleFrame` seam.
2. [ ] Run a lightweight Tier 1 model via ExecuTorch on-device ([[executorch-qnn]], [[model-selection]]).
3. [ ] Emit spoken/haptic awareness cues for a few salient classes ([[accessibility-principles]]).
4. [ ] Verify the whole Tier 1 path works in **airplane mode** ([[airplane-mode-demo]]).
5. [ ] (Stretch) On-demand [[ocr-mode]] reading one sign/label.

## Explicitly out of scope for MVP
- Tier 2 VLM ([[conversational-vision]]), all skills ([[mcp-skill-system]]), navigation, full voice layer, translation beyond a stretch goal.

## Definition of success
Airplane mode on → camera live → at least one real, useful spoken awareness cue, fully offline.

## Related
- [[post-hackathon]] · [[demo-checklist]]
