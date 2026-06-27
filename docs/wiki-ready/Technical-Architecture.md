# Technical Architecture

OBSERVA is a layered, on-device pipeline. Each layer can be developed in parallel against a mocked interface (see [[Parallel Work Plan]]).

## Layers

### Layer 1: Input
- CameraX frame stream (**implemented**: `Preview` + `ImageAnalysis`).
- Microphone / voice trigger (**planned**; `RECORD_AUDIO` declared).
- User controls (minimal touch; **partial**).

### Layer 2: Frame Processing
- Frame throttling (**partial**: `STRATEGY_KEEP_ONLY_LATEST` drops stale frames).
- YUV/RGB conversion (**planned**; frames arrive as `YUV_420_888`).
- Resize / crop (**planned**).
- Model input tensor preparation (**planned**; this is the `handleFrame` seam).

### Layer 3: Inference
- ExecuTorch where available (**Needs verification**: AAR bundled at `app/libs/executorch.aar`, not yet invoked).
- Qualcomm QNN backend where available (**Needs verification / not implemented**).
- CPU fallback if needed (**planned**).
- Mock/demo inference only when clearly labeled (**planned**, for [[Demo Plan]] Demo Mode).

### Layer 4: Reasoning
- Detection parsing, confidence filtering, tracking (**planned**).
- Spatial reasoning, hazard priority scoring (**planned** — see REQ-003).
- Duplicate suppression (**planned** — see REQ-005).

### Layer 5: Output
- Text-to-speech (**planned**).
- Directional / spatial audio (**planned**).
- Haptics (**planned**; `VIBRATE` declared).
- Visual debug overlay (**partial**: frame counter on screen).
- Performance dashboard (**partial** — see REQ-006).

### Layer 6: Demo / Observability
- Pipeline visualization, backend status, FPS/latency (**planned/partial**).
- Airplane Mode proof (**ready to demonstrate** for the parts that exist).
- Logs (**partial**: `Log.e` on camera binding failure).

## Pipeline

```
CameraX
↓
Frame Processor
↓
ExecuTorch Runtime
↓
Qualcomm QNN Backend / CPU Fallback
↓
Detection Parser
↓
Hazard Engine
↓
Alert Policy
↓
Speech + Spatial Audio + Haptics
↓
Accessible User Experience
```

## Truthful Backend Policy

Honesty is a scoring asset (see [[Hackathon Scoring Strategy]]). Rules:

- **Never claim NPU acceleration unless the implementation verifies it** at runtime.
- If QNN is partially integrated, **document the exact status** here and in the UI.
- If CPU fallback is used, **label it clearly** in the UI and demo.
- Judges value honesty and instrumentation more than vague claims. A truthful "CPU fallback, 18ms/frame" beats an unverified "NPU accelerated."
- The backend-status string in the UI (REQ-007) must reflect the actual code path taken.

## Repo facts (verified)

- Kotlin 2.2.10, AGP 9.2.1, Gradle Kotlin DSL, version catalog `gradle/libs.versions.toml`.
- `minSdk 26`, `targetSdk/compileSdk 36`, Java 11.
- CameraX 1.6.1; Compose BOM 2026.02.01; Material 3.
- Entry point: `MainActivity` (`com.observa.app`); R8 keep rules in `app/src/main/keepRules/`.

See [[Development Loops]] (AI Runtime Loop) for how to bring Layer 3 online truthfully, and [[Validation Gates]] (Gate F) for performance visibility criteria.
