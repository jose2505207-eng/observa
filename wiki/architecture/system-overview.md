---
status: planned
confidence: medium
last_updated: 2026-06-28
owner: jose2505207-eng
---

# System Overview

> How OBSERVA is built, end to end. Read [[two-tier-inference]] and [[offline-first-design]] alongside this.

## Current Reality
A single-module Android app (`com.observa.app`) with one `MainActivity` and a Jetpack Compose UI ([[android-stack]]). The data path that exists today:

```
Camera (CameraX)
  → Preview → on-screen PreviewView (Compose AndroidView)
  → ImageAnalysis (background executor, KEEP_ONLY_LATEST, YUV_420_888)
     → FrameInput (L/C/R + avg luma; RGB tensor only when a model is loaded)
       → ObservaController.runProcessingLoop():
            ExecuTorchDetector (if model loaded) else HeuristicVisionRuntime
          → HazardEngine (cooldown / scene memory)
          → AccessibilityOutputRouter → TTS + Braille/live-region
                                       + SpatialCueEngine (audio + haptics)
On demand: "Read Text" → one-shot bitmap → MlKitOcrEngine (offline) → router
Voice:     push-to-talk → OfflineSpeechRecognizer → VoiceCommandParser → CommandRouter
```

This whole pipeline is real and device-verified offline. `ExecuTorchDetector` actually calls the ExecuTorch API (`Module.load/forward`); with no `.pte` bundled it falls back honestly to the brightness heuristic. ML Kit's bundled OCR model and the platform on-device recognizer run locally; the app has **no `INTERNET` permission**. See [[executorch-qnn]], [[ocr-mode]], [[voice-command-layer]], [[privacy-model]].

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
