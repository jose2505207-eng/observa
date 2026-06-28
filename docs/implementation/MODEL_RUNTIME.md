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

### Cross-checked with a second, independent stack (Google LiteRT + Qualcomm QNN delegate)
To rule out an ExecuTorch-specific issue, the **official Qualcomm AI Engine Direct LiteRT delegate**
(`com.qualcomm.qti:qnn-litert-delegate:2.28.0` + `qnn-runtime:2.28.0`, the version closest to the
device's own vendor QNN firmware `2.27.5`) was wired into a TFLite `Interpreter` with
`QnnDelegate.Options(BackendType.HTP_BACKEND, HtpPdSession.HTP_PD_SESSION_UNSIGNED)` and a tiny probe
model. These AARs ship Qualcomm's own matched HTP host libs + v79 skel and declare **no INTERNET**.
Result on the same retail device — **identical failure**:

```
[Qnn ExecuTorch] Failed to load skel, error: 4000
Failed to create device_handle for Backend ID 6, error=14001
OBSERVA_NPU: QNN HTP delegate NOT available: Failed to apply delegate
```

Decisive detail: in the **same** logcat, signed system processes (the camera HAL) load cDSP skels
fine (`libbitml_nsp_79na_skel.so`, `libois_channel_skel.so`). So the cDSP works — it just refuses
HTP skel loads from a **third-party app's protection domain**. This is an **OS-level retail security
restriction** (user build, verified-boot green), not a bug in our export/packaging: **two independent
QNN stacks fail identically**, so on this handset the NPU is unreachable by any sideloaded app
(Priorities 1–4 all blocked). The LiteRT experiment was reverted (it adds ~40 MB and does not work);
the reproducible evidence lives here. NPU would require a signed/privileged build or an OEM allowlist.

### 2026-06-29 fresh re-validation (laptop-crash hypothesis ruled out)

The user suspected the NPU might be broken because the laptop crashed mid-build. Re-audited from
scratch — **rebuilt/re-ran every host+export stage and repeated the device test**:

| Stage | Command | Fresh result |
|---|---|---|
| Env intact | `echo $QNN_SDK_ROOT` | `/home/ivancito/Qualcomm/qairt/2.47.0.260601` (SDK, `executorch/` tree, `.venv`, device all present) |
| Host pybind rebuild | `cd executorch/build-x86 && make PyQnnManagerAdaptor` | exit 0, `[100%] Built target PyQnnManagerAdaptor` |
| QNN raw-head re-export | `python scripts/export_detector.py --qnn-raw-head --imgsz 320 --out <tmp>` | exit 0, all ops `\| True`, `backend=qnn-htp (SM8750 / HTP v79)`, valid 6.9 MB `.pte` (6,887,296 bytes; sha differs from bundled only due to QNN-compiler non-determinism — functionally identical) |
| `.pte` loads on device | install + run, logcat | `Deserializing … QnnContextCustomProtocol`, `Creating new backend bundle` — **loads** |
| HTP skel load | same run | **fails fresh**: `QnnDsp <E> Failed to load skel, error: 4000` → `Failed to create device_handle for Backend ID 6, error=14001` → `Init failed for backend QnnBackend: 0x1` |
| App behavior | same run | falls back honestly: `XNNPACK CPU. QNN attempted: [ExecuTorch Error 0x1] …`; per-frame `OBSERVA_MODEL inference ~25 ms (avg)` |
| Counter-proof (DSP works) | same logcat | signed camera HAL opens an **unsigned PD** on cDSP (`Successfully opened /vendor/dsp/cdsp/fastrpc_shell_unsigned_3`, `Created user PD on domain 7 … Unsigned:Y`) and loads `libbitml_nsp_79na_skel.so` |
| Device skel inventory | `adb shell find /vendor /system … -iname "*Htp*Skel*"` | HTP v79 skels exist **only** in vendor-signed paths (`/vendor/lib64/rfs/dsp/snap/libQnnHtpV79Skel.so`, `/vendor/lib64/hw/audio/…`) |

**Verdict:** every host/export stage passes freshly after a clean rebuild, and the only failure is the
device-side HTP skel load — a security gate that the same chip grants to a signed system process in
the same instant. The block is the **retail S25 Ultra's DSP/protection-domain policy**, not the laptop
crash, not the export, not the app. NPU would light up automatically on a signed/privileged/`userdebug`
build or an OEM-allowlisted device, because `LOADED_QNN` is set only on a real QNN warm-up `forward`.

### 2026-06-29 deep reverse-engineering (full matrix → Outcome B)

See [`QNN_REVERSE_ENGINEERING_LOG.md`](QNN_REVERSE_ENGINEERING_LOG.md) for the loop-by-loop log. Net new
findings that pin the root cause beyond doubt:

- **ABI verified:** `libQnnHtpV79Skel.so` is `ELF 32-bit QUALCOMM DSP6` (correct cDSP arch); host libs
  AArch64; no mismatch, no missing NEEDED, no version mixing.
- **Experiment F (decisive):** a tiny 2-op `Conv2d→ReLU` QNN `.pte` (67 KB) fails at the **identical**
  first line as YOLO — `loadRemoteSymbols failed err 4000` → `device_handle … 14001`. The failure is at
  HTP **device-handle creation**, *before* any graph runs → **model-independent**. Not the YOLO model,
  not the export, not the parser.
- **PD mode:** ExecuTorch QNN already defaults to **`kHtpUnsignedPd`** — the only PD mode a non-OEM app
  can request — and the retail device still refuses it (err 4000). Signed PD needs an OEM signature.
- **Device/SELinux:** `SM-S938U1`, verified-boot green, bootloader locked, `ro.debuggable=0`, SELinux
  enforcing; app domain `untrusted_app`; cDSP fastRPC nodes are `vendor_qdsp_device`/`vendor_xdsp_device`
  (system-owned). No app-attributed AVC — the refusal is DSP-side (AEE err 4000), while the signed
  camera HAL uses the same cDSP. New debug surface: `QNN stage:` (… → model loaded → backend init /
  warm-up failed), never "active" without a real warm-up `forward`.
- **External dependency for Outcome A (any one):** signed PD / OEM allowlist; platform-signed privileged
  app; engineering/`userdebug` firmware; Qualcomm AI Hub deployment; or a device whose policy permits
  third-party unsigned-PD HTP.

## Reproduce the artifact

```bash
cd /home/ivancito/observa-hackathon/executorch && source .venv/bin/activate
cd ../observa-android
python scripts/export_detector.py --imgsz 320   # XNNPACK default → assets/models/observa_detector.pte
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s OBSERVA_EXECUTORCH OBSERVA_MODEL   # see load + per-frame latency + output shapes
```
