# MODEL RUNTIME — real on-device inference (Agent 1)

**Status: REAL local ML inference verified on device.** OBSERVA runs a YOLOv8n object detector
through ExecuTorch, fully offline, on the Samsung Galaxy S25 Ultra (Snapdragon 8 Elite, SM8750).

## What runs

| Field | Value |
|---|---|
| Model | YOLOv8n (Ultralytics), COCO-80 |
| Format | ExecuTorch `.pte`, `assets/models/observa_detector.pte` (~12.7 MB) |
| Input | `float32 [1,3,320,320]`, RGB, 0..1, NCHW |
| Output | `[1,84,2100]` (cx,cy,w,h px + 80 class probs) + aux head tensors |
| Delegate | **XNNPACK** CPU delegate (`forward backends=[XnnpackBackend]`) |
| Toolchain | executorch 1.4.0a0 tree + torch 2.12.1 (`executorch/.venv`), matches `app/libs/executorch.aar` |
| License | YOLOv8 is AGPL-3.0 — fine for the hackathon demo; swap to SSD-MobileNet/NanoDet for permissive distribution |

## Measured latency (on-device, SM8750, Airplane Mode)

Read live from `OBSERVA_MODEL` logcat (`inference Nms (avg Mms)`):

| Stage | Value |
|---|---|
| Model load | ~11 ms (one-time) |
| Inference (model `forward`) | **median ~32 ms**, range ~26–43 ms |
| Pipeline target | < 100 ms danger recognition — **met** |

Compare: the same model exported with **portable reference kernels (no delegate) ran ~4490 ms**.
XNNPACK is a ~140× speedup and the reason the latency target is met on CPU.

## Pipeline (code map)

1. `ObservaScreen.kt` — CameraX `ImageAnalysis` (KEEP_ONLY_LATEST, YUV_420_888) →
   `toFrameInput(modelNeedsPixels)` converts YUV→RGB (BT.601), nearest-neighbor downscale to
   `RGB_CAPTURE=320`, packs `FrameInput.rgb` (0..1) **only while a real model is loaded**.
2. `ObservaController.runProcessingLoop` — initializes the model once off the UI thread; per duty
   cycle runs `ExecuTorchDetector.analyzeFrame` on `Dispatchers.Default` (never the UI thread).
3. `ExecuTorchDetector.kt` — `Module.load(pte)`; `preprocess` → `Tensor.fromBlob [1,3,320,320]` →
   `m.forward` → `TensorOutput`s → `YoloDetectionParser`. Times load/inference; logs everything to
   `OBSERVA_MODEL` / `OBSERVA_EXECUTORCH`.
4. `DetectionParser.kt` (`YoloDetectionParser`) — selects the `[1,C,N]` detection tensor by shape
   (the head emits 6 tensors), threshold 0.40, class-aware NMS @ IoU 0.45, COCO→safety category,
   box centerX → LEFT/CENTER/RIGHT. Pure JVM, unit-tested.
5. `HazardEngine` — cooldown + scene memory → `Hazard` events → `AccessibilityOutputRouter`
   (TTS + Braille + spatial audio/haptic cues). Person/vehicle/large-central obstacle only.

## Honesty / failure behavior

- Model absent → `UNAVAILABLE`, heuristic fallback, no detections, no crash.
- Load fails → `FAILED`, exact error logged, heuristic fallback, no crash.
- Unknown output shape → parser returns empty (never fabricates labels).
- `objects=0` when nothing recognizable is in frame (e.g. phone on a desk) — reported truthfully.
- QNN active **only** if `MethodMetadata.forward.backends` contains a QNN backend; otherwise the UI
  says CPU. Detecting the packaged `libqnn_executorch_backend.so` alone is **not** acceleration.

## Runtime native libs (from `executorch.aar`)

`libexecutorch.so` (incl. XNNPACK) + `libqnn_executorch_backend.so`, arm64-v8a. The Java `Module`
loads them via Facebook SoLoader — so the app declares `com.facebook.fbjni:fbjni:0.7.0` and
`com.facebook.soloader:nativeloader:0.10.5` (pinned to the executorch_android build). **Without these
the model load throws `ClassNotFoundException: NativeLoader` and silently falls back to heuristic** —
this was the original blocker and is now fixed.

## QNN / NPU path (attempted, not shipped)

The Qualcomm QNN SDK is present (`qairt/2.47.0.260601`) and the ExecuTorch QNN python backend imports.
`scripts/export_detector.py --qnn` lowers the model to the QNN partitioner. **Not shipped because**
a QNN-delegated `.pte` also needs the Qualcomm HTP runtime `.so`s (`libQnnHtp.so`, `libQnnSystem.so`,
HTP v79 stub/skel for SM8750) packaged in the APK `jniLibs` — the bundled AAR ships only the ET↔QNN
bridge lib, not the QNN runtime. Since XNNPACK CPU already meets the <100 ms target at 32 ms, QNN is a
battery/headroom optimization, tracked as the next stretch. The app reports QNN status truthfully and
never claims NPU acceleration it isn't using.

## Reproduce the artifact

```bash
cd /home/ivancito/observa-hackathon/executorch && source .venv/bin/activate
cd ../observa-android
python scripts/export_detector.py --imgsz 320   # XNNPACK default → assets/models/observa_detector.pte
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s OBSERVA_EXECUTORCH OBSERVA_MODEL   # see load + per-frame latency + output shapes
```
