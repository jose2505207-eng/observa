---
status: planned
confidence: low
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Profiling Plan

> How we will measure latency, battery, memory, and thermals to validate [[performance-targets]]. Nothing measured yet.

## Current Reality
No profiling has been done yet. The hooks exist: `ExecuTorchDetector` records load ms and last/avg inference latency and surfaces them in the dashboard "AI detail" row and `OBSERVA_MODEL` logcat. But with no `.pte` bundled there is no model inference to profile; FPS (~25–32) is observable today.

## Planned measurement harness
- **Latency:** timestamp frame-in → result-out at the detector (`ExecuTorchDetector.analyzeFrame`, already instrumented) ([[ambient-awareness]]); log percentiles, not just averages.
- **Throughput:** sustained Tier 1 FPS under realistic conditions.
- **Battery/thermal:** Android Battery Historian / `dumpsys batterystats`; watch for thermal throttling over long sessions.
- **Memory:** Android Studio profiler; resident footprint for Tier 1 vs transient Tier 2 ([[two-tier-inference]]).
- **Acceleration A/B:** CPU vs GPU vs QNN backends once inference exists ([[executorch-qnn]]).

## Method
- Measure on real target hardware, not just emulator.
- Record results as processed notes in `sources/` and update [[performance-targets]] with real numbers, replacing placeholders.

## Related
- [[model-selection]] · [[risks-and-mitigations]]
