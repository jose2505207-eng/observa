# On-device models

Drop an ExecuTorch model here. It is bundled into the APK and loaded **locally** —
OBSERVA never downloads models or sends frames anywhere.

## Expected file

- **`observa_detector.pte`** — object/hazard detector exported for ExecuTorch.

`ExecuTorchDetector` looks for exactly this path (`models/observa_detector.pte`),
copies it to internal storage, and calls `Module.load(...)`.

## Input contract (preprocessing)

`ExecuTorchDetector.preprocess` produces (YOLOv8 default):

- dtype: `float32`
- shape: `[1, 3, 640, 640]` (NCHW)
- channel order: **RGB**
- normalization: **0..1** (pixel / 255)

For a different size/layout/normalization, change `inputWidth`/`inputHeight` (and
`preprocess`) and update this file.

## Output contract (parsing)

The default parser is now **`YoloDetectionParser`**, which understands a YOLO single
output tensor `[1, 84, 8400]` (cx,cy,w,h in 0..640 + 80 COCO class probs), and also
`[1, N, 84]` and the `+1` objectness variant. It thresholds (0.40), runs class-aware
NMS (IoU 0.45), maps COCO → safety categories, and computes left/center/right.

For any **unrecognized** output shape it returns empty — it never fabricates labels.
(`StrictUnknownShapeParser` remains available as the most conservative default.)

To enable real detections:

1. Bundle a valid `observa_detector.pte` (see `scripts/export_detector.py` and
   `docs/real-detector.md`).
2. Read the logged output shapes from `OBSERVA_MODEL`; confirm the parser matches.
3. Optionally add `observa_detector.pte.version` (a short text sidecar) — it is shown
   in diagnostics.
4. Verify on device before claiming object recognition.

## Acceleration (QNN)

The ExecuTorch QNN delegate library (`libqnn_executorch_backend.so`) is packaged
in the APK, but **QNN is only "active" if the model itself was lowered to the QNN
backend**. `ExecuTorchDetector` checks `MethodMetadata.getBackends()` for `forward`
at load time and reports `loaded — QNN active` only when that is true; otherwise it
reports `loaded — CPU`. Detecting the library alone is **not** acceleration.

## Sourcing a model (do not download blindly)

Prefer a small mobile detector compatible with ExecuTorch Android that can later be
lowered to Qualcomm QNN — e.g. from the ExecuTorch examples
(`executorch/examples`) or Qualcomm AI Hub. When you add one, record here:

- source URL,
- exact export/lowering command,
- license,
- file size.

No model is committed yet, so the app runs the heuristic fallback and claims no ML
detection or QNN acceleration.
