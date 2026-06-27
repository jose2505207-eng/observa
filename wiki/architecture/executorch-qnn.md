---
status: planned
confidence: medium
last_updated: 2026-06-28
owner: jose2505207-eng
---

# ExecuTorch & Qualcomm QNN

> On-device inference runtime and hardware acceleration. The **integration code is real**; end-to-end inference and QNN acceleration are **not yet demonstrated** (no model bundled).

## Current Reality
- ExecuTorch is bundled as a **local AAR** (`app/libs/executorch.aar`, `org.pytorch.executorch`) exposing the real API: `Module.load/forward`, `EValue`, `Tensor`, `MethodMetadata.getBackends()`. The AAR also ships `libexecutorch.so` **and** `libqnn_executorch_backend.so` (arm64-v8a) inside the APK.
- **The integration code calls these APIs for real:** `ExecuTorchDetector` copies `assets/models/observa_detector.pte` to internal storage, `Module.load`s it, introspects `forward` backends, runs `forward` off the UI thread, and parses output via `YoloDetectionParser`. This builds and is unit-tested.
- **No model artifact is bundled**, so on device the detector reports **`UNAVAILABLE → heuristic fallback`** (device-verified). End-to-end ExecuTorch inference is therefore implemented but **not yet demonstrated**.
- **QNN is truthful and NOT active.** `QnnRuntimeChecker` detects the delegate library *inside the APK* (fixing a `extractNativeLibs=false` false-negative) and reports "present (not active)". `LOADED_QNN` is set **only** when a loaded model's `forward` backends actually include a QNN backend — never from library presence.

## Future Vision (to be validated)
- Bundle a verified `.pte` (see `scripts/export_detector.py`, `docs/real-detector.md`) so the model path produces real detections, then measure latency/power ([[profiling-plan]], [[performance-targets]]).
- Lower the detector to the **Qualcomm QNN** backend for NPU/DSP acceleration and confirm via `getBackends()`. CPU fallback (XNNPACK) otherwise.

## Things we must verify before claiming "Current"
- Which ExecuTorch version the AAR provides (not stamped in the binary) and matching it during `.pte` export so the model loads.
- A successful on-device `Module.load` + at least one real detection.
- Real measured latency/power, not datasheet numbers ([[profiling-plan]]).
- QNN actually used by `forward` (backends include QNN) before claiming acceleration.

## Action
Record findings as processed notes under `sources/` ([[sources/README]]) and update this page's status only when backed by code + measurement.

## Related
- [[model-selection]] · [[risks-and-mitigations]]
