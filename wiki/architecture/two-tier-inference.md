---
status: planned
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Two-Tier Inference

> OBSERVA splits inference into an always-on Tier 1 and an on-demand Tier 2. This is the core architectural decision — see [[ADR-0002-two-tier-architecture]].

## Tier 1 — Always-on
- Runs continuously on every (sampled) camera frame.
- **Lightweight**, **battery efficient**, low latency.
- Produces continuous scene understanding and salient events ([[ambient-awareness]]).
- Must work fully offline ([[offline-first-design]]). This is the safety core.

## Tier 2 — On-demand
- Triggered by the user (voice/gesture), not on every frame.
- Heavier workloads: [[ocr-mode]], [[conversational-vision]], document understanding.
- May use larger models and more power because it runs briefly.

## The hard rule
**Tier 2 must never block Tier 1.** If a Tier 2 task is running or fails, Tier 1 awareness keeps flowing. Implications:
- Separate execution so a heavy Tier 2 inference can't starve the Tier 1 loop.
- Backpressure: Tier 1 already uses `STRATEGY_KEEP_ONLY_LATEST` so it drops stale frames rather than queuing.
- Resource budgeting so Tier 2 can't exhaust memory/thermal headroom Tier 1 needs ([[performance-targets]]).

## Current Reality
Only a single always-on frame loop exists (frame counting; no model). There is no Tier 2 and no real Tier 1 model yet. The current loop is the foundation Tier 1 will be built on. See [[system-overview]].

## Open questions
- Thread/process isolation strategy between tiers.
- Frame sampling rate for Tier 1 vs full-frame capture for Tier 2.
- Shared vs separate model contexts.

## Related
- [[model-selection]] · [[executorch-qnn]] · [[safety-principles]]
