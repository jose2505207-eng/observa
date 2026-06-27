---
status: risk
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Risks & Mitigations

> Known engineering risks. Updated as reality is learned.

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|------------|
| 1 | On-device inference too slow for real-time Tier 1 | High | High | Lightweight models, quantization, QNN/GPU acceleration; aggressive frame sampling. See [[model-selection]], [[executorch-qnn]]. |
| 2 | ExecuTorch/QNN integration harder than expected (operator gaps, backend availability) | High | High | Treat as **Research**; spike early; CPU fallback. See [[executorch-qnn]]. |
| 3 | Battery/thermal drain from always-on camera+inference | High | High | Frame sampling, duty cycling, measure early. See [[profiling-plan]], [[performance-targets]]. |
| 4 | On-device VLM not feasible | Medium | Medium | Keep [[conversational-vision]] optional/cloud-enhanced; never in the safety core. |
| 5 | Safety: confidently wrong hazard guidance | Medium | Critical | Communicate uncertainty; fail loud; don't replace primary aid. See [[safety-principles]]. |
| 6 | Always-on foreground service complexity (Android 14+/SDK 36 constraints) | Medium | Medium | Permissions pre-declared; design service carefully; test on target SDK. |
| 7 | Scope creep across many planned features for a hackathon | High | Medium | Hold the line on [[hackathon-mvp]]; everything else is post-MVP. |
| 8 | Code/wiki divergence | Medium | Medium | Enforce [[development-loop]] + [[wiki-rules]] maintenance step. |

## Related
- [[two-tier-inference]] · [[demo-checklist]]
