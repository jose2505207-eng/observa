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

## QNN / NPU path — host AOT now UNBLOCKED; YOLOv8n graph still blocks (v2.1.0)

Concrete attempt made for **SM8750 (Snapdragon 8 Elite, HTP v79)**. The previously-documented host
init blocker is **now solved**; a new, deeper model-graph blocker remains. Honest status, no faking.

### What was the blocker (v2.0.0) and what really caused it
The QNN partitioner's host op-support check builds a host `QnnManager` and called `InitBackend()`,
failing with `RuntimeError: Failed to initialize QNN backend for kHtpBackend` — even with `QNN_SDK_ROOT`
set and `$QNN_SDK_ROOT/lib/x86_64-linux-clang` on `LD_LIBRARY_PATH`. Drilling past the generic Python
message into the native QNN logger (`QNN_LOG_LEVEL=DEBUG`) revealed the real cause:

```
<E> Failed to set scatter gather parameters: unknown custom config socModel 0
<E> Failed to create device_handle for Backend ID 6 (HTP), error=14001
```

ExecuTorch serialized the SoC correctly (`SocInfo(soc_model=SM8750:69, htp_arch=V79, vtcm=8MB)`), yet
`libQnnHtp.so` (2.47) read the device custom-config socModel as **0**. The failure reproduced
identically for **SM8650 and SM8550** too — i.e. it was not chipset-specific. Root cause: an **ABI
mismatch** between the **prebuilt ExecuTorch 1.3.1 wheel's** QNN host pybind
(`PyQnnManagerAdaptor.cpython-312-*.so`) and **QNN SDK 2.47's** HTP device-config struct (the
"scatter gather" custom config is a newer SDK field). The wheel's pybind wrote the older struct layout,
so the 2.47 lib saw socModel 0.

### The fix (works)
Rebuilt the ExecuTorch QNN host pybind **from the local `executorch/` source tree against QNN SDK 2.47**:

```bash
cd /home/ivancito/observa-hackathon/executorch && source .venv/bin/activate
export QNN_SDK_ROOT=/home/ivancito/Qualcomm/qairt/2.47.0.260601
cd build-x86 && make PyQnnManagerAdaptor -j"$(nproc)"   # compiles vs .../2.47.0.260601/include/QNN
# then use that .so for export (swapped over the wheel copy in site-packages):
#   cp build-x86/backends/qualcomm/PyQnnManagerAdaptor.cpython-312-*.so \
#      .venv/.../site-packages/executorch/backends/qualcomm/python/PyQnnManagerAdaptor.cpython-312-*.so
```

Verified: `QnnManager(opt).InitBackend()` now returns **0 (success)**, and a real `Conv2d→ReLU` model
lowers fully to QNN HTP (`aten.convolution|True`, `aten.relu|True`, HTP compiler runs, valid
`QnnBackend` `.pte` produced).

### New blocker: the YOLOv8n graph itself
Exporting the **full YOLOv8n** (320×320) via `to_edge_transform_and_lower_to_qnn` now reaches QNN's
graph passes and fails in the **`I64toI32` pass** at the YOLOv8 detection head's anchor decode
(`make_anchors`): `RuntimeError: expand: attempting to expand a dimension of length 3 -> 1`. This is a
model-graph lowering incompatibility, not a QNN init/SDK problem. Resolving it needs the head reshaped
(export raw multi-scale outputs and decode in the parser, or quantize/annotate the head) — a separate,
larger task, and one that **cannot be device-verified in this session (no device attached).**

### Still required to actually ship QNN
1. Resolve the YOLOv8n head `I64toI32` issue (above) to get a QNN `.pte`.
2. Toolchain match: the on-device AAR is **1.4.0a0**; the venv import is the **1.3.1 wheel** — a QNN
   `.pte` must be schema-compatible with the AAR runtime (re-verify, or export with the 1.4.0a0 tree).
3. Package the v79 device `.so`s in APK `jniLibs/arm64-v8a` (`libQnnHtp.so`, `libQnnSystem.so`,
   `libQnnHtpV79Stub.so`, `hexagon-v79/.../libQnnHtpV79Skel.so`) — the AAR ships only the ET↔QNN bridge.

Because **XNNPACK CPU already meets the <100 ms target at 32 ms**, QNN remains a battery/thermal-headroom
optimization, not a latency need. The app reports QNN truthfully (`LOADED_QNN` only when `forward`
backends include a QNN backend), so it lights up automatically once a QNN `.pte` loads — no fake claims.

## Reproduce the artifact

```bash
cd /home/ivancito/observa-hackathon/executorch && source .venv/bin/activate
cd ../observa-android
python scripts/export_detector.py --imgsz 320   # XNNPACK default → assets/models/observa_detector.pte
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s OBSERVA_EXECUTORCH OBSERVA_MODEL   # see load + per-frame latency + output shapes
```
