---
status: current
confidence: high
last_updated: 2026-06-29
owner: jose2505207-eng
---

# ExecuTorch & Qualcomm QNN

> **2026-06-29 re-audit (fresh, after a laptop crash-during-build scare):** the whole host/export
> pipeline was rebuilt and re-run from scratch and the on-device test repeated. **Result: not a
> laptop-build artifact.** Host pybind rebuilt clean (`make PyQnnManagerAdaptor` → `[100%] Built
> target`); QNN raw-head **re-export passed** (all ops `| True`, valid 6.9 MB `qnn-htp SM8750/HTPv79`
> `.pte` — byte-differs from the bundled one only due to QNN-compiler non-determinism, same 6,887,296
> bytes); the bundled `.pte` **loads/deserializes on device**; HTP init still fails at the skel load
> with a **fresh** `Failed to load skel, error: 4000` / `device_handle … error=14001`. Decisive new
> proof: in the *same* logcat the signed Samsung camera process opens an **unsigned PD** on the cDSP
> (`/vendor/dsp/cdsp/fastrpc_shell_unsigned_3`, `Created user PD on domain 7 … Unsigned:Y`) and loads
> an HTP skel (`libbitml_nsp_79na_skel.so`) — so the DSP works; it refuses the **third-party app's**
> protection domain. Conclusion unchanged: **retail OS/DSP protection-domain block, not our bug.**

> On-device inference runtime and hardware acceleration. **Real end-to-end ExecuTorch inference is now demonstrated on device** (YOLOv8n, XNNPACK CPU delegate, ~32 ms median, Airplane Mode). QNN/NPU acceleration is documented and attemptable but **not yet shipped** (XNNPACK already meets the latency target).

## Current Reality (verified on Galaxy S25 Ultra · SM8750)
- ExecuTorch is bundled as a **local AAR** (`app/libs/executorch.aar`, `org.pytorch.executorch`, built from the local `executorch/` tree at **1.4.0a0**) exposing the real API: `Module.load/forward`, `EValue`, `Tensor`, `MethodMetadata.getBackends()`. The AAR ships `libexecutorch.so` (incl. XNNPACK) **and** `libqnn_executorch_backend.so` (arm64-v8a).
- **A real model is bundled and runs:** `assets/models/observa_detector.pte` — YOLOv8n COCO-80 exported at 320×320 with the **XNNPACK** delegate (`scripts/export_detector.py`, executorch 1.4.0a0 toolchain so the `.pte` schema matches the AAR runtime). `ExecuTorchDetector` `Module.load`s it (~11 ms), runs `forward` off the UI thread, and parses `[1,84,2100]` via `YoloDetectionParser`.
- **Measured latency:** inference **median ~32 ms (p95 ~58 ms)**, in Airplane Mode — under the 100 ms target. `forward backends=[XnnpackBackend]`. Baseline portable kernels were ~4490 ms (~140× slower).
- **The load blocker is fixed:** ExecuTorch's `Module` loads native libs via Facebook SoLoader, so the app now declares `com.facebook.fbjni:fbjni:0.7.0` + `com.facebook.soloader:nativeloader:0.10.5`. Without them `Module.load` threw `ClassNotFoundException: NativeLoader` and fell back to heuristic.
- **QNN is truthful and NOT active.** `QnnRuntimeChecker` detects the delegate library *inside the APK* and reports "present (not active)". `LOADED_QNN` is set **only** when a loaded model's `forward` backends actually include a QNN backend — never from library presence. The shipped model is XNNPACK CPU, so the app reports QNN off.

## QNN/NPU path — export+load WORK; device DSP skel blocks HTP (v2.2.0; NPU not claimed active)
- **Host init SOLVED (v2.1.0):** `QnnManager.InitBackend()` failed (`socModel 0` / error 14001) due to an ABI mismatch between the ExecuTorch 1.3.1 wheel's QNN pybind and QNN SDK 2.47. Fix: rebuilt the pybind from the local `executorch/` source against SDK 2.47 → `InitBackend` returns 0.
- **YOLOv8n graph SOLVED (v2.2.0):** the decoded head broke QNN's `I64toI32` pass at `make_anchors`. Fix: `--qnn-raw-head` exports the raw multi-scale head (`[1,144,40,40/20/10]`, pre-decode); the graph lowers fully to QNN/HTP → real 6.9 MB `QnnBackend` `.pte`. Decode/DFL/NMS moved to `YoloRawHeadParser` on Android (unit-tested).
- **Packaged + selected:** `.pte` + v79 device libs in `jniLibs/arm64-v8a`; `ExecuTorchDetector` tries QNN first, accepts it only on a successful warm-up `forward`, else XNNPACK. `model_manifest.json` records both.
- **Remaining blocker (device security):** on the production S25 Ultra the `.pte` loads but HTP init fails at warm-up — `QnnDsp Failed to load skel, error 4000` / `device_handle ... error=14001`, even with `ADSP_LIBRARY_PATH` set. The 8-Elite cDSP refuses an **unsigned** HTP skel from an app `/data` path (needs signed PD or `/vendor` skel via root). So **QNN/NPU is not active on this handset**; detector runs XNNPACK CPU (~32 ms) and the UI says so.
- **Cross-checked & confirmed OS-level:** the official Qualcomm **LiteRT QNN delegate** (`com.qualcomm.qti:qnn-litert-delegate:2.28.0`, matched to the device's 2.27.5 firmware, Qualcomm-signed skel, no INTERNET) fails with the **identical** `skel 4000` / `device_handle 14001` while signed system processes (camera) load cDSP skels in the same logcat. Two independent QNN stacks fail the same way → third-party apps cannot use the NPU on this retail unit. The LiteRT experiment was reverted (40 MB, non-functional). Detail: `docs/implementation/MODEL_RUNTIME.md`.

## Verified (the bar this page held)
- ExecuTorch version matched (local 1.4.0a0 tree built the AAR and exported the `.pte`). ✓
- On-device `Module.load` success + real `forward` producing correct output tensors. ✓
- Real measured latency on the target SoC, not datasheet numbers. ✓
- QNN claimed only if `forward` backends include QNN — currently honestly off. ✓

## Related
- [[model-selection]] · [[risks-and-mitigations]]
