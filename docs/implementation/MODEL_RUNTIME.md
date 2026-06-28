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

Concrete attempt made for **SM8750 (Snapdragon 8 Elite, HTP v79)**:

1. **Chipset supported:** `QcomChipset.SM8750` exists; the QNN SDK (`qairt/2.47.0.260601`) ships the
   v79 runtime/skel libs (`libQnnHtpV79Stub.so`, `hexagon-v79/.../libQnnHtpV79Skel.so`, `libQnnHtp.so`,
   `libQnnSystem.so`). `scripts/export_detector.py --qnn` uses the correct API:
   `generate_qnn_executorch_compiler_spec(soc_model=QcomChipset.SM8750, backend_options=generate_htp_compiler_spec(use_fp16=True))`
   then `to_edge_transform_and_lower(..., partitioner=[QnnPartitioner(spec)])`.
2. **Blocker (host AOT lowering):** the partitioner's host op-support check constructs a host
   `QnnManager` which fails with `RuntimeError: Failed to initialize QNN backend for kHtpBackend`,
   even with `QNN_SDK_ROOT` set and `$QNN_SDK_ROOT/lib/x86_64-linux-clang` on `LD_LIBRARY_PATH`
   (the x86 `libQnnHtp.so` deps all resolve via `ldd`). Root cause: the ExecuTorch QNN host pybind
   extension in `executorch/.venv` is not built with working HTP host support against QNN SDK 2.47 —
   fixing it means rebuilding ExecuTorch's QNN AOT extension, out of scope for this release.
3. **Therefore not shipped.** A QNN `.pte` would also need the v79 device `.so`s in the APK
   `jniLibs/arm64-v8a` (the AAR ships only the ET-to-QNN bridge lib). Since **XNNPACK CPU already meets
   the <100 ms target at 32 ms**, QNN is a battery/thermal-headroom optimization, not a latency need.

**Next step to unblock:** rebuild ExecuTorch's QNN host extension against QNN SDK 2.47, re-run `--qnn`,
add the v79 runtime + skel `.so`s to `jniLibs`. The app reports QNN truthfully (`LOADED_QNN` only when
`forward` backends include a QNN backend), so it lights up automatically once a QNN `.pte` loads.

## Reproduce the artifact

```bash
cd /home/ivancito/observa-hackathon/executorch && source .venv/bin/activate
cd ../observa-android
python scripts/export_detector.py --imgsz 320   # XNNPACK default → assets/models/observa_detector.pte
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s OBSERVA_EXECUTORCH OBSERVA_MODEL   # see load + per-frame latency + output shapes
```
