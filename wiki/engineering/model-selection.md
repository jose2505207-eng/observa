---
status: research
confidence: low
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Model Selection

> Which models run in each tier, and the tradeoffs. **All Research** — no model is integrated yet.

## Current Reality
No model is loaded or run anywhere in the repo. ExecuTorch is bundled but unused ([[executorch-qnn]]).

## Selection criteria
- On-device feasibility: size, latency, memory within [[performance-targets]].
- ExecuTorch export path and operator coverage (PyTorch → `.pte`).
- Acceleration compatibility (QNN/GPU/CPU fallbacks).
- Accuracy adequate for safety-relevant use ([[safety-principles]]).
- Offline operation for any core model ([[offline-first-design]]).

## Candidates by tier (to evaluate)
- **Tier 1 (always-on):** a small, fast detector/segmenter or efficient scene classifier. Must be the lightest viable option.
- **Tier 2 OCR:** an on-device text detection + recognition model; plus an offline translation model ([[ocr-mode]]).
- **Tier 2 VLM:** small vision-language model — feasibility on-device is genuinely uncertain ([[conversational-vision]]); may remain cloud-optional and never part of the core.

## Process
- Prototype export → run via ExecuTorch → measure ([[profiling-plan]]) → record source notes ([[sources/README]]) → only then update status here.

## Related
- [[two-tier-inference]] · [[risks-and-mitigations]]
