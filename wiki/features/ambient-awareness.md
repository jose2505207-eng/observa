---
status: planned
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Ambient Awareness (Tier 1)

> Continuous, always-on, on-device scene understanding that tells the user what's around them without aiming. The heart of OBSERVA. See [[vision]] and [[two-tier-inference]].

## Current Reality
The scaffolding exists; the intelligence does not.
- A CameraX `ImageAnalysis` loop runs on a background single-thread executor with `STRATEGY_KEEP_ONLY_LATEST` and `OUTPUT_IMAGE_FORMAT_YUV_420_888`.
- `handleFrame()` currently **only increments a frame counter** and closes the `ImageProxy`. A live counter is shown on screen, proving the always-on loop is alive.
- No model runs on the frames; there is no scene understanding or output yet.

## Future Vision
- Convert the YUV frame at the `handleFrame` seam into a model input tensor.
- Run a lightweight on-device model ([[model-selection]], [[executorch-qnn]]) to detect salient elements (people, obstacles, steps, doorways, text presence, hazards).
- Debounce/prioritize results and emit concise audio + haptic cues ([[accessibility-principles]]), most safety-relevant first ([[safety-principles]]).
- Stay within Tier 1 [[performance-targets]] and never block on Tier 2.

## Design notes
- Sample frames (don't process every frame) to save power.
- Announce *changes*, not a constant stream, to avoid overwhelming the user.
- Feeds [[spatial-guidance]] once something salient is detected.

## Open questions
- Which model and label set for the first version.
- Cue vocabulary and prioritization rules.

## Related
- [[ocr-mode]] · [[conversational-vision]] · [[airplane-mode-demo]]
