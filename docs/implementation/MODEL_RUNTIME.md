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

## QNN / NPU path — host AOT + raw-head export WORK; device DSP skel-load blocks HTP (v2.2.0)

Concrete attempt for **SM8750 (Snapdragon 8 Elite, HTP v79)**. Two earlier blockers are **solved**
(host QnnManager init, and the YOLOv8n head graph). A real QNN `.pte` is built, packaged, and **loads
on device**, but HTP execution is gated by the device's DSP security model. Honest status, no faking —
the app falls back to XNNPACK and says so.

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

### Fixed the YOLOv8n graph: raw-head export
The decoded head failed QNN's `I64toI32` pass at `make_anchors` (`expand: dimension 3 -> 1`). Fix:
export the **raw multi-scale head** (`scripts/export_detector.py --qnn-raw-head`) — the Detect head's
`forward` is replaced so it returns `cat(box-DFL, class-logits)` per scale *before* anchor decode:
`[1,144,40,40] [1,144,20,20] [1,144,10,10]` (144 = 4·reg_max + nc = 64 + 80). The graph is then pure
conv/silu/concat/sigmoid and **lowers fully to QNN/HTP** — all ops `| True`, a real **6.9 MB
`QnnBackend` `.pte`** is produced. The anchor decode + DFL + dist2bbox + NMS now run on the Android CPU
in `YoloRawHeadParser` (unit-tested), producing detections identical to the XNNPACK path.

### Packaged + selected at runtime
`detector_yolov8n_qnn_sm8750.pte` is bundled with the v79 device libs in `jniLibs/arm64-v8a`
(`libQnnHtp.so`, `libQnnSystem.so`, `libQnnHtpV79Stub.so`, `libQnnHtpPrepare.so`,
`libQnnHtpV79Skel.so`; the AAR already ships `libqnn_executorch_backend.so`). `ExecuTorchDetector`
tries the QNN model first and accepts QNN **only** if `forward` lists a QNN backend AND a warm-up
`forward` actually executes; otherwise it falls back to the proven XNNPACK `observa_detector.pte`.
`model_manifest.json` records both. Toolchain note: export uses the **1.3.1 wheel** pybind; the QNN
`.pte` program schema loads fine on the **1.4.0a0** AAR (same as the working XNNPACK `.pte`).

### Remaining blocker: device DSP refuses the unsigned HTP skel
On the **production Galaxy S25 Ultra**, the QNN `.pte` loads but HTP device init fails at warm-up
(captured from on-device `adb logcat`):

```
[Qnn ExecuTorch] QnnDsp <E> loadRemoteSymbols failed with err 4000
[Qnn ExecuTorch] QnnDsp <E> Failed to load skel, error: 4000
[Qnn ExecuTorch] Failed to create device_handle for Backend ID 6, error=14001
ExecuTorch: Init failed for backend QnnBackend: 0x1
```

`err 4000` (AEE unable-to-load) persists even with `ADSP_LIBRARY_PATH` set to the app's extracted
native-lib dir (which contains `libQnnHtpV79Skel.so`, verified). Root cause: the Snapdragon 8 Elite
cDSP will not load an **unsigned** HTP skel from an app `/data` path via fastRPC — it requires a
signed PD or the skel staged in `/vendor/lib/rfsa/adsp` (root). Neither is achievable from a normal
sideloaded app, so **QNN/NPU cannot be activated on this production handset**. The detector therefore
runs on **XNNPACK CPU (median ~32 ms)** and the UI/debug status reports
`Detector backend: XNNPACK CPU fallback. QNN attempted: <skel load 4000>` — never a fake QNN claim.

To actually run QNN/NPU here would need one of: an engineering/`userdebug` build permitting unsigned
PD, root to stage the skel in `/vendor`, or a Qualcomm-signed skel/PD. The full pipeline (export →
package → load → honest backend selection) is in place and would light up automatically on such a
device, because `LOADED_QNN` is set only when a QNN warm-up `forward` truly succeeds.

## Reproduce the artifact

```bash
cd /home/ivancito/observa-hackathon/executorch && source .venv/bin/activate
cd ../observa-android
python scripts/export_detector.py --imgsz 320   # XNNPACK default → assets/models/observa_detector.pte
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s OBSERVA_EXECUTORCH OBSERVA_MODEL   # see load + per-frame latency + output shapes
```
