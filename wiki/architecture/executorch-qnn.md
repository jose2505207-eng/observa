---
status: research
confidence: low
last_updated: 2026-06-27
owner: jose2505207-eng
---

# ExecuTorch & Qualcomm QNN

> On-device inference runtime and hardware acceleration. **Most claims here are Research** until the repo demonstrates them.

## Current Reality
- ExecuTorch is bundled as a **local AAR** at `app/libs/executorch.aar` and linked via `implementation(files("libs/executorch.aar"))`.
- **No ExecuTorch API is called anywhere in the code.** No model is loaded, no tensor is produced, no inference runs. The frame seam (`handleFrame`) is empty of inference logic.
- Therefore: ExecuTorch integration status is **Planned**, and QNN acceleration status is **Research** — nothing about hardware acceleration is proven in this repo.

## Future Vision (to be validated)
- Use ExecuTorch to run Tier 1 and Tier 2 models on-device ([[two-tier-inference]]).
- Investigate the **Qualcomm AI Engine Direct (QNN)** backend for NPU/DSP acceleration on Snapdragon devices to hit [[performance-targets]].
- Fallbacks to CPU/GPU (XNNPACK / Vulkan) where QNN is unavailable.

## Things we must verify before claiming "Current"
- Which ExecuTorch version the AAR provides and its supported backends.
- Whether the target devices have QNN-capable hardware and required runtime libs.
- Model export path (PyTorch → ExecuTorch `.pte`) and operator coverage for chosen models ([[model-selection]]).
- Real measured latency/power, not datasheet numbers ([[profiling-plan]]).

## Action
Record findings as processed notes under `sources/` ([[sources/README]]) and update this page's status only when backed by code + measurement.

## Related
- [[model-selection]] · [[risks-and-mitigations]]
