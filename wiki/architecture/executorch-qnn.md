---
status: current
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# ExecuTorch & Qualcomm QNN

> On-device inference runtime and hardware acceleration. **Real end-to-end ExecuTorch inference is now demonstrated on device** (YOLOv8n, XNNPACK CPU delegate, ~32 ms median, Airplane Mode). QNN/NPU acceleration is documented and attemptable but **not yet shipped** (XNNPACK already meets the latency target).

## Current Reality (verified on Galaxy S25 Ultra · SM8750)
- ExecuTorch is bundled as a **local AAR** (`app/libs/executorch.aar`, `org.pytorch.executorch`, built from the local `executorch/` tree at **1.4.0a0**) exposing the real API: `Module.load/forward`, `EValue`, `Tensor`, `MethodMetadata.getBackends()`. The AAR ships `libexecutorch.so` (incl. XNNPACK) **and** `libqnn_executorch_backend.so` (arm64-v8a).
- **A real model is bundled and runs:** `assets/models/observa_detector.pte` — YOLOv8n COCO-80 exported at 320×320 with the **XNNPACK** delegate (`scripts/export_detector.py`, executorch 1.4.0a0 toolchain so the `.pte` schema matches the AAR runtime). `ExecuTorchDetector` `Module.load`s it (~11 ms), runs `forward` off the UI thread, and parses `[1,84,2100]` via `YoloDetectionParser`.
- **Measured latency:** inference **median ~32 ms (p95 ~58 ms)**, in Airplane Mode — under the 100 ms target. `forward backends=[XnnpackBackend]`. Baseline portable kernels were ~4490 ms (~140× slower).
- **The load blocker is fixed:** ExecuTorch's `Module` loads native libs via Facebook SoLoader, so the app now declares `com.facebook.fbjni:fbjni:0.7.0` + `com.facebook.soloader:nativeloader:0.10.5`. Without them `Module.load` threw `ClassNotFoundException: NativeLoader` and fell back to heuristic.
- **QNN is truthful and NOT active.** `QnnRuntimeChecker` detects the delegate library *inside the APK* and reports "present (not active)". `LOADED_QNN` is set **only** when a loaded model's `forward` backends actually include a QNN backend — never from library presence. The shipped model is XNNPACK CPU, so the app reports QNN off.

## QNN/NPU path (attempted, not shipped)
- The Qualcomm QNN SDK is present (`qairt/2.47.0.260601`) and the ExecuTorch QNN python backend imports; `scripts/export_detector.py --qnn` lowers to the QNN partitioner.
- **Blocker to ship:** a QNN-delegated `.pte` also needs the Qualcomm HTP runtime `.so`s (`libQnnHtp.so`, `libQnnSystem.so`, SM8750 HTP v79 stub/skel) in the APK `jniLibs`; the AAR ships only the ET↔QNN bridge lib. Since XNNPACK already meets the target at 32 ms, QNN is the next battery/headroom stretch. Detail: `docs/implementation/MODEL_RUNTIME.md`.

## Verified (the bar this page held)
- ExecuTorch version matched (local 1.4.0a0 tree built the AAR and exported the `.pte`). ✓
- On-device `Module.load` success + real `forward` producing correct output tensors. ✓
- Real measured latency on the target SoC, not datasheet numbers. ✓
- QNN claimed only if `forward` backends include QNN — currently honestly off. ✓

## Related
- [[model-selection]] · [[risks-and-mitigations]]
