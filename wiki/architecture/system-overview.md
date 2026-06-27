---
status: planned
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# System Overview

> How OBSERVA is built, end to end. Read [[two-tier-inference]] and [[offline-first-design]] alongside this.

## Current Reality
A single-module Android app (`com.observa.app`) with one `MainActivity` and a Jetpack Compose UI ([[android-stack]]). The data path that exists today:

```
Camera (CameraX)
  → Preview  → on-screen PreviewView (Compose AndroidView)
  → ImageAnalysis (background single-thread executor,
       STRATEGY_KEEP_ONLY_LATEST, YUV_420_888)
     → handleFrame(): counts frames, closes ImageProxy
```

That is the entire live pipeline. `handleFrame` is the seam where model input will be produced; it currently only increments a counter to prove the always-on loop is alive. ExecuTorch is bundled as a local AAR but **not called**. See [[executorch-qnn]].

## Future Vision
The frame seam feeds a **two-tier inference** system:

```
                 ┌──────────────── Tier 1 (always-on, on-device) ─────────────┐
Camera frames →  │ lightweight scene model → salient events → audio/haptic out │
                 └────────────────────────────────────────────────────────────┘
                                   │ user request
                                   ▼
                 ┌──────────────── Tier 2 (on-demand, on-device) ─────────────┐
                 │ OCR / VLM reasoning / document understanding                 │
                 └────────────────────────────────────────────────────────────┘
                                   │ optional, permissioned
                                   ▼
                 ┌──────────────── Skills (optional, may be online) ──────────┐
                 │ alarms, email, web search — via MCP skill system            │
                 └────────────────────────────────────────────────────────────┘
```

- Tier 2 must never block Tier 1. See [[two-tier-inference]].
- Output layer: TTS + earcons + haptics (Planned), governed by [[accessibility-principles]].
- The safety core (camera → Tier 1 → output) is isolated from skills ([[ADR-0003-skill-system-boundaries]]).

## Key modules (target)
- Capture: CameraX (Current scaffold).
- Inference runtime: ExecuTorch, optionally QNN-accelerated ([[executorch-qnn]], [[model-selection]]).
- Output: speech/haptics (Planned).
- Skills: [[mcp-skill-system]] (Planned).

## Related
- [[performance-targets]] · [[privacy-model]]
