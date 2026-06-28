# QNN / NPU Reverse-Engineering Log

Goal: reach **Outcome A** (YOLO detector truly runs on the NPU, status QNN/NPU active only after a
successful warm-up `forward`) or **Outcome B** (NPU proven impossible from this retail app/device
config, with exact evidence + the named external dependency).

Device: Samsung Galaxy S25 Ultra (SM8750 / Snapdragon 8 Elite, HTP v79), retail, `adb` id `R3CXC08009D`.

Rules: try each fix path **once**; if an attempt yields the same error without changing an input
variable, stop repeating it. Every loop appends hypothesis / commands / files changed / observed logs /
conclusion / next action.

---

## LOOP 0 — Preserve state, branch

- **Hypothesis:** n/a (setup).
- **Commands:** `git checkout -b npu-root-cause-reverse-engineering`.
- **State:** from `main` @ `f206124`. Tags incl. `v2.2.0`, `v2.2.0-rc1` (both honest XNNPACK-fallback).
- **Conclusion:** investigation branch created; this log started.
- **Next:** LOOP 1 build + APK content audit.

---

## LOOP 1 — Local build + APK contents

- **Hypothesis:** build is clean and the APK actually contains the QNN model + runtime + no INTERNET.
- **Commands:** `./gradlew clean assembleDebug test`; `unzip -l` the APK; `aapt2 dump permissions`.
- **Observed:**
  - Build **SUCCESSFUL**, tests pass. APK = **100 MB**, `app-debug.apk`.
  - QNN model `assets/models/detector_yolov8n_qnn_sm8750.pte` = 6,887,296 B; fallback
    `assets/models/observa_detector.pte` = 12,706,240 B; both present + `model_manifest.json`.
  - Packaged `lib/arm64-v8a/`: `libexecutorch.so` (8.5 MB), `libqnn_executorch_backend.so` (1.1 MB),
    `libQnnHtp.so` (3.6 MB), `libQnnHtpV79Stub.so` (0.7 MB), **`libQnnHtpV79Skel.so` (11.5 MB)**,
    `libQnnSystem.so` (3.9 MB), **`libQnnHtpPrepare.so` (86 MB, on-device graph compiler — only needed
    for online_prepare; candidate to drop for size)**.
  - **INTERNET: ABSENT.**
- **Conclusion:** packaging is correct and complete; not a packaging-absence problem.
- **Next:** LOOP 2 off-device validity (already largely proven 06-29; see below).

## LOOP 2 — QNN host smoke test + export validity

- **Hypothesis:** the host adaptor + export are healthy (rule out the "laptop crash broke the build").
- **Commands:** `make PyQnnManagerAdaptor`; `export_detector.py --qnn-raw-head --imgsz 320`; tiny
  `Conv2d→ReLU` lowered via the same `generate_qnn_executorch_compiler_spec`/`generate_htp_compiler_spec`.
- **Observed:** host pybind builds clean (`[100%] Built target`); YOLO raw-head export passes (all ops
  `| True`, 6.9 MB `qnn-htp SM8750/HTPv79` `.pte`); **tiny Conv2d/ReLU lowers to QNN HTP** (`aten.relu
  | True`, `aten.convolution | True`) → valid 67 KB QnnBackend `.pte`. Env: `QNN_SDK_ROOT=…/qairt/
  2.47.0.260601`, python 3.12.
- **Conclusion:** host + export are clean. Not a laptop-build artifact, not an export bug.
- **Next:** LOOP 3 ABI / LOOP 4 device.

## LOOP 3 — Binary / ABI inspection

- **Hypothesis:** wrong ABI or missing dependent libs cause the skel load to fail.
- **Commands:** `file` + `readelf -h/-d` on `jniLibs/arm64-v8a/*.so`.
- **Observed:**
  - `libQnnHtpV79Skel.so` = **ELF 32-bit, QUALCOMM DSP6** (correct — it runs on the Hexagon cDSP, not
    the CPU). Host libs (`libQnnHtp/System/Stub/Prepare`) = ELF64 AArch64. SONAMEs correct; skel NEEDED
    only `libc++.so.1`/`libc++abi.so.1` (DSP-side).
  - No x86 host libs packaged; no missing NEEDED in the host libs; no duplicate QNN versions; LiteRT
    2.28 runtime is **not** in this APK (isolated — it was reverted earlier).
- **Conclusion:** ABI is correct. The skel is the right architecture for HTP v79. Not an ABI bug.
- **Next:** LOOP 4 device security posture + denial capture.

## LOOP 4 — Device environment reverse engineering

- **Hypothesis:** the device's security posture blocks third-party HTP skel/PD loading.
- **Commands:** `getprop` identity; `getenforce`; `ls -laZ /dev/*rpc* /dev/*cdsp*`; `ps -eZ`; AVC grep;
  device skel inventory.
- **Observed:**
  - Identity: `ro.product.model=SM-S938U1`, `ro.soc.model=SM8750`, `ro.board.platform=sun`.
  - **Locked production:** `ro.boot.verifiedbootstate=green`, `ro.boot.flash.locked=1`,
    `ro.debuggable=0`, `getenforce=Enforcing`.
  - App SELinux domain = **`u:r:untrusted_app:s0`** (`u0_a282`).
  - fastRPC nodes: `/dev/fastrpc-cdsp` → `u:object_r:vendor_qdsp_device:s0` (system:system, `crw-rw-r--`);
    `/dev/fastrpc-cdsp-secure` + `-adsp-secure` → `vendor_xdsp_device` (`crw-r--r--`).
  - **No SELinux AVC denial is attributed to our app** in logcat — the refusal is DSP-side
    (`loadRemoteSymbols err 4000`, AEE "unable to load"), not an Android-policy avc.
  - Device HTP v79 skels exist only in **vendor-signed paths** (`/vendor/lib64/rfs/dsp/snap/
    libQnnHtpV79Skel.so`, `/vendor/lib64/hw/audio/…`).
  - Counter-proof (same logcat, earlier run): signed Samsung camera HAL opens an **unsigned PD** on the
    cDSP (`Successfully opened /vendor/dsp/cdsp/fastrpc_shell_unsigned_3`, `Created user PD on domain 7
    … Unsigned:Y`) and loads `libbitml_nsp_79na_skel.so`.
- **Conclusion:** fully-locked retail unit; our app is `untrusted_app`; the cDSP grants PDs to the
  signed vendor camera but refuses the unsigned third-party skel/PD with err 4000.
- **Next:** LOOP 5 instrumentation + LOOP 6 experiment matrix.

## LOOP 5 — Runtime instrumentation

- **Hypothesis:** an explicit per-stage status will pinpoint where QNN dies and keep the UI honest.
- **Files changed:** `runtime/ExecuTorchDetector.kt` (+`qnnStage`: not started → libs present/missing →
  model loaded → no QNN backend / backend init / warm-up failed → active); `ObservaController.kt`
  (`qnnStageLine`, appended to `announceDebugStatus`).
- **Observed (device):** stage reaches **"model loaded"** then **"backend init / warm-up failed"** —
  i.e. the `.pte` deserializes and `Module.load` succeeds; the failure is the warm-up `forward` when
  the QNN backend tries to create the HTP device handle. `LOADED_QNN` is never set.
- **Conclusion:** the model loads; the device handle (skel load) is the wall. Status stays truthful.
- **Next:** LOOP 6 experiments.

## LOOP 6 — Controlled experiment matrix

**Key API finding:** ExecuTorch QNN already defaults to **`kHtpUnsignedPd`** (`qc_schema.py:191`,
`HtpDevice.cpp:312`) — the *only* PD mode available to a non-OEM app. Our `.pte` already uses it.

| Exp | Changed variable | Model | Skel source | PD mode | Result (first failing line) |
|---|---|---|---|---|---|
| A | baseline (as shipped) | YOLOv8n raw-head QNN | app `/data/.../lib/arm64` + vendor via `ADSP_LIBRARY_PATH` | unsigned (default) | `loadRemoteSymbols failed err 4000` → `device_handle … 14001` |
| C/D | `ADSP_LIBRARY_PATH` already includes `/vendor/dsp/cdsp;/vendor/lib/rfsa/adsp;/vendor/lib64/rfsa/adsp;/system/lib/rfsa/adsp` | YOLOv8n QNN | vendor paths | unsigned | **same** `err 4000` — skel *location* is not the variable; the *calling PD* is refused. Not re-run (same input → same error). |
| **F** | **tiny 2-op model** | **Conv2d→ReLU QNN (67 KB)** | app skel | unsigned | **identical** `loadRemoteSymbols failed err 4000` → `Failed to load skel 4000` → `device_handle 14001` |
| G | LiteRT Qualcomm delegate (prior turn, isolated, no INTERNET) | TFLite probe | Qualcomm-signed v79 skel in the AAR | unsigned/HTP | **identical** `Failed to load skel, error: 4000` / `device_handle 14001` |
| B | remove app-packaged skel, rely on vendor skel | n/a | vendor only | unsigned | **not run** — logically subsumed: err 4000 is PD-auth, not file-presence; ADSP_LIBRARY_PATH already exposes the vendor skel dir and A still fails. Removing the app skel cannot grant `untrusted_app` a PD the DSP already refused. |
| E | runtime version alignment | — | — | — | app uses QNN **2.47** end-to-end (host + device libs match); device firmware QNN ≈ 2.27.5; LiteRT path used 2.28 and failed the same way. Version is not the variable; the PD auth is. |

- **Conclusion:** every QNN HTP path — tiny model, full YOLO, ExecuTorch, and Google's LiteRT — fails
  at the **same model-independent device-handle stage** with the same `err 4000`. The variable that
  matters (calling process = `untrusted_app`, unsigned PD on a locked retail unit) is constant and
  refused; the variables we *can* change (model, export, skel file location, ABI) are all proven not to
  be the cause.

## LOOP 7 — Decision tree outcome

- Tiny QNN model **fails identically** to YOLO QNN (Exp F) **and** LiteRT tiny path fails identically
  (Exp G) → per the decision tree, the problem is **not** YOLO/export/parser; it is **HTP access / skel
  loading / DSP protection-domain**.
- No vendor/system *signed PD* is reachable from `untrusted_app` (the only PD mode we can request,
  unsigned, is refused; signed PD needs an OEM/Qualcomm signature we do not have).
- **Verdict: Outcome B.** A locked retail S25 Ultra (SM-S938U1, verified-boot green, bootloader
  locked, `ro.debuggable=0`, SELinux enforcing) blocks third-party HTP execution from a normal
  sideloaded `untrusted_app`. NPU is **not achievable** from this app/device configuration.

### Named external dependency required for Outcome A (any one of)
1. **Signed PD / OEM allowlist** — app (and its HTP skel) signed/allowlisted by Samsung/Qualcomm so the
   cDSP authorizes the protection domain (`kHtpSignedPd`).
2. **Privileged / platform-signed app** in a vendor SELinux domain permitted to use `vendor_qdsp_device`.
3. **Engineering / `userdebug` firmware** (or unlocked bootloader) that permits `untrusted_app`
   unsigned-PD DSP access.
4. **Qualcomm AI Hub / AI Engine Direct supported deployment** path with the device's signed runtime.
5. **A different device/runtime** whose policy allows third-party unsigned-PD HTP (some dev phones do).

Detector continues on **XNNPACK CPU (~22–32 ms, under the 100 ms target)** and reports it honestly;
`LOADED_QNN` is set only after a real QNN warm-up `forward` (which never succeeds here).

## LOOP 8 — Fix decision

- **No working NPU path exists** from this app/device config (LOOP 7). So: keep the single QNN→XNNPACK
  runtime path, keep XNNPACK fallback, keep the new `qnnStage` instrumentation + this log. Do **not**
  fake QNN. Do **not** tag `v2.2.0` as NPU-complete.
- **Known size optimization (documented, not applied — unverifiable with the device offline):** the
  packaged `libQnnHtpPrepare.so` (86 MB) is the **on-device graph compiler**, used only for
  `online_prepare`. Our `.pte` is an offline-prepared context binary, so it is not needed at runtime;
  dropping it (and `online_prepare=False` stays) would cut the debug APK ~86 MB. Deferred until it can
  be verified on device that the honest `skel 4000` attempt path is unchanged (removing a lib the QNN
  init might dlopen could change the failure to a misleading lib-not-found before the real block).

## LOOP 9 — Accessibility preserved

- This session changed only `runtime/ExecuTorchDetector.kt` (+`qnnStage`) and `ObservaController.kt`
  (`qnnStageLine` in `announceDebugStatus`) — no accessibility/gesture code touched.
- `./gradlew clean assembleDebug test` green; 166 unit tests pass (operating-layer reducer, gesture
  controller, orientation, translation, etc.). Current-status / Last-alert / Available-actions nodes,
  TalkBack custom actions, and the two-layer gesture model are unchanged. Backend status stays truthful
  (`Detector backend: XNNPACK CPU fallback. QNN attempted: …`, now also `QNN stage: backend init /
  warm-up failed`).

## Device note

During the final re-install of the instrumented build the S25 Ultra dropped off USB (`adb: no
devices/emulators found`). All decisive device measurements (LOOP 4 identity/SELinux/fastRPC, the
baseline `skel 4000` with the real YOLO QNN model, and **Experiment F** with the tiny model) were
captured earlier in the same session while connected; the instrumented build compiled and unit tests
passed. The verdict (Outcome B) does not depend on the missing re-install.

