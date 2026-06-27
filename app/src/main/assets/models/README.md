# On-device models

Drop an ExecuTorch model here. It is bundled into the APK and loaded **locally** —
OBSERVA never downloads models or sends frames anywhere.

## Expected file

- **`observa_detector.pte`** — object/hazard detector exported for ExecuTorch.

`ExecuTorchDetector` looks for exactly this path (`models/observa_detector.pte`),
copies it to internal storage, and calls `Module.load(...)`.

## Assumed input contract (preprocessing)

`ExecuTorchDetector.preprocess` currently produces:

- dtype: `float32`
- shape: `[1, 3, 224, 224]` (NCHW)
- channel order: **RGB**
- normalization: **0..1** (pixel / 255)

If your model expects a different size, layout, or normalization (e.g. mean/std
or `[1, 224, 224, 3]` NHWC), update `inputWidth`/`inputHeight` and `preprocess`
and document the change here.

## Output contract (parsing)

The output is parsed **only** through a `DetectionParser`. The default
`StrictUnknownShapeParser` **refuses to emit any label** and just logs the
observed output tensor shapes (look for `OBSERVA_MODEL` in logcat). This is
deliberate: the app will **not** speak object labels (“person”, “stairs”,
“crosswalk”, …) until a model-specific parser is written and verified against the
real output shapes on device.

To enable real detections you must:

1. Bundle a valid `observa_detector.pte`.
2. Read the logged output shapes from `OBSERVA_MODEL`.
3. Implement a `DetectionParser` for that exact format (boxes/scores/labels) and a
   labels file if the model emits class indices.
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
