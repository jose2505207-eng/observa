---
status: current
confidence: high
last_updated: 2026-06-28
owner: jose2505207-eng
---

# Ambient Awareness (Tier 1)

> Continuous, always-on, on-device scene understanding that tells the user what's around them without aiming. The heart of OBSERVA. See [[vision]] and [[two-tier-inference]].

## Current Reality
The always-on pipeline is real and device-verified (S25 Ultra, Airplane Mode). The ML *intelligence* runs through a real path but no detector model is bundled yet, so it falls back honestly to a brightness heuristic.
- CameraX `ImageAnalysis` loop on a background executor (`STRATEGY_KEEP_ONLY_LATEST`, `YUV_420_888`); ~25–32 FPS. The analyzer computes a left/center/right + average luminance `FrameInput` (no pixels stored/sent); it captures an RGB tensor only while a real model is loaded.
- `ObservaController.runProcessingLoop()` runs the active detector every cycle: the **ExecuTorch model if loaded**, else the **`HeuristicVisionRuntime`** (brightness — a real, on-device proxy, labeled as such, never emits semantic labels).
- `HazardEngine` maps detections → hazards with cooldown + scene memory ("Still …" / "Path clear"; no spam).
- Output goes through one unified `AccessibilityOutputRouter`: TTS + Braille/live-region + directional audio + directional haptics, with `HAZARD > NAVIGATION > OCR > MODE > INFO` priority (hazards interrupt). See [[spatial-guidance]].
- Real ML detection path: `ExecuTorchDetector` + `YoloDetectionParser` (COCO→person/vehicle/obstacle, NMS, left/center/right). Implemented + unit-tested; **model artifact not bundled** (see `docs/real-detector.md`), so live ML detection is not yet demonstrated on device. See [[executorch-qnn]].

## Future Vision
- Bundle a verified ExecuTorch detector (`assets/models/observa_detector.pte`) so the model path produces real detections; measure latency and lower to QNN where possible.
- Expand the cue vocabulary and prioritization; tune debounce for calm, change-driven announcements.
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
