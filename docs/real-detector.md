# Real detector integration

How OBSERVA goes from heuristic fallback to a real offline object detector, and how to
reproduce the model artifact. **No model is bundled in this branch** (see "Status"); the full
inference + parser path is implemented and tested, and this doc + `scripts/export_detector.py`
produce `observa_detector.pte` reproducibly.

## Chosen model

| Field | Value |
|---|---|
| Model | YOLOv8n (nano) object detector |
| Source | Ultralytics (`yolov8n.pt`) |
| License | **AGPL-3.0** (Ultralytics). Confirm compatibility before distributing bundled weights. A permissive alternative is SSD-MobileNetV2 / NanoDet (add a matching parser). |
| Classes | COCO-80 (see `CocoClassMapper.COCO`) |
| Why | Small, mobile-friendly, single output tensor, widely exported to ExecuTorch, and lowerable to Qualcomm QNN. |

## Input contract (implemented in `ExecuTorchDetector.preprocess`)

- dtype `float32`, shape `[1, 3, 640, 640]` (NCHW), channel order **RGB**, normalization **0..1**.
- Source: camera `YUV_420_888` → RGB (BT.601) → nearest-neighbor resize to 640×640.
- Rotation: the on-demand OCR path rotates; the detector path consumes the analyzer RGB buffer
  (upright preview orientation). If detections appear rotated, apply rotation in preprocessing.

## Output contract (implemented in `YoloDetectionParser`)

- `[1, 84, 8400]` channels-first: `cx, cy, w, h` (pixels 0..640) + 80 class probs (no objectness).
- Parser also accepts `[1, N, 84]` and the `+1` objectness variant (`85`).
- Postprocessing: score = (objectness×)best-class-prob; threshold 0.40; class-aware NMS @ IoU 0.45;
  boxes normalized to 0..1; center-x → LEFT/CENTER/RIGHT; COCO → safety category.

## Class → safety mapping (`CocoClassMapper`)

- `person` → PERSON; `bicycle/car/motorcycle/bus/truck/train/boat/airplane` → VEHICLE.
- A documented furniture set → OBSTACLE **only** when `includeGenericObstacles=true` (off by default).
- Everything else → dropped (never spoken). Unknown classes are never voiced as hazards.

## Hazard interpretation

- DetectedObject → engine `Detection(label, confidence, direction)`: PERSON→PERSON, VEHICLE/OBSTACLE
  →OBSTACLE. Fed into the existing `HazardEngine` (cooldown, scene memory, "Path clear") and the
  unified `AccessibilityOutputRouter` (TTS + Braille + audio/haptic cues). Person left/right/center
  → directional cue; large central object → urgent hazard; repeats throttled; hazards interrupt OCR.

## Reproduce the artifact

```bash
python3 -m venv .venv && source .venv/bin/activate
pip install "executorch==<match app/libs/executorch.aar>" ultralytics torch
python scripts/export_detector.py --out app/src/main/assets/models/observa_detector.pte
# optional QNN lowering (needs Qualcomm SDK + ExecuTorch QNN backend):
python scripts/export_detector.py --qnn --out app/src/main/assets/models/observa_detector.pte
```

Then rebuild, install, and read `OBSERVA_MODEL` logcat for the **actual** output shapes; if they
differ from `[1,84,8400]`, the parser still auto-detects layout, but verify on device.

## QNN (truthful)

`QnnRuntimeChecker` reports the delegate library is packaged but **not active** unless the loaded
model's `forward` actually uses a QNN backend. `ExecuTorchDetector` sets `LOADED_QNN` only when
`MethodMetadata.getBackends()` for `forward` contains a QNN backend after a successful load. A plain
CPU `.pte` → `LOADED_CPU`. Library presence is never reported as acceleration.

## Limitations

- YOLOv8 is AGPL — resolve licensing before shipping bundled weights.
- `.pte` schema must match the bundled ExecuTorch runtime; a mismatch → `FAILED` + honest fallback.
- Heuristic fallback emits only generic OBSTACLE (brightness), never semantic labels.
- Distance/size is approximated from box area; not a true depth estimate.

## Status (this branch)

- Implemented & unit-tested: preprocessing, YOLO parser, NMS, class mapping, direction, hazard
  routing, diagnostics, honest fallback.
- **Not bundled:** `observa_detector.pte` (requires the export toolchain + license decision; this
  environment has no torch/executorch installed and must not blind-download weights).
- **Blocked pending artifact:** on-device "model loads" + "real detection from camera" — run the
  script above, bundle the `.pte`, and validate per `docs/manual-test-real-detector.md`.
