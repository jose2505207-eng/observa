---
status: planned
confidence: low
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Performance Targets

> Provisional budgets. These are **goals**, not measurements — nothing here has been profiled yet. See [[profiling-plan]].

## Why targets matter
For a non-visual mobility aid, latency and battery are accessibility and safety properties ([[accessibility-principles]], [[safety-principles]]). Slow or battery-hungry awareness is unusable awareness.

## Provisional budgets (to validate)
| Dimension | Tier 1 (always-on) | Tier 2 (on-demand) |
|---|---|---|
| End-to-end latency | low enough for real-time hazard cues (target: well under ~300 ms perceived) | a few seconds acceptable |
| Frame cadence | sampled, not every frame; tuned for power | single-shot / short burst |
| Sustained power | must allow long sessions without overheating/throttling | brief, bursty OK |
| Memory | small resident footprint | larger, transient |

All numbers above are **placeholders** pending real device measurement.

## Levers
- Model size/quantization ([[model-selection]]).
- Hardware acceleration via QNN/GPU ([[executorch-qnn]]).
- Frame sampling and `STRATEGY_KEEP_ONLY_LATEST` backpressure (already in the loop).
- Tier separation so Tier 2 can't starve Tier 1 ([[two-tier-inference]]).

## Current Reality
No performance work done. The loop counts frames; there is no inference to measure. Establish a measurement harness before optimizing — see [[profiling-plan]].
