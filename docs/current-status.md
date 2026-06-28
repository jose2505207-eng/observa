# OBSERVA — Current Status (honesty ledger)

Last verified: 2026-06-27, on the target device **Samsung Galaxy S25 Ultra (SM-S938U1, Snapdragon 8 Elite SM8750, arm64-v8a)**, serial `R3CXC08009D`, in **Airplane Mode**, debug build.

## What works now (verified on device)
- **Real on-device ML detector (NEW):** YOLOv8n (COCO-80) exported to **ExecuTorch** at 320×320 with the **XNNPACK** CPU delegate, bundled as `assets/models/observa_detector.pte` (12.7 MB) and loaded locally. Verified in logcat: `load success … forward backends=[XnnpackBackend]`, output tensor `[1, 84, 2100]` parsed by `YoloDetectionParser`. **Inference latency: ~26–43 ms, median ~32 ms** on the Snapdragon 8 Elite — under the 100 ms danger-recognition target. Runs in **Airplane Mode**. Detections (person/vehicle/large central obstacle) flow into the `HazardEngine`; the demo desk view produces `objects=0` honestly (no COCO hazard in frame).
- **QNN / NPU:** the QNN delegate library is packaged; the current `.pte` is **XNNPACK CPU**, not QNN-delegated, so the app honestly reports QNN **off** (`forward backends=[XnnpackBackend]`, not `QnnBackend`). See `docs/implementation/MODEL_RUNTIME.md` for the QNN path and what it requires.
- **Camera loop:** CameraX preview + `ImageAnalysis` (`STRATEGY_KEEP_ONLY_LATEST`, YUV_420_888); live on device. Frames **~1400+**, **FPS ~30**.
- **Frame analysis:** per-frame luminance of left/center/right thirds computed on a background thread (no pixels stored or sent).
- **Brightness heuristic (fallback only):** `HeuristicVisionRuntime` flags a markedly darker third as a possible obstacle. Deterministic proxy — **not** ML. Used **only** if the model is absent or fails to load.
- **Hazard engine:** maps detections → alerts with **cooldown + scene memory** (announce → "Still …" → "Path clear"; no spam). Verified label "announced OBSTACLE_AHEAD".
- **Speech:** `TextToSpeech` alerts. Dashboard shows **Speech: Ready**.
- **Haptics:** severity-scaled vibration. Dashboard shows **Haptics: Available**.
- **Demo Mode:** deterministic scripted sequence; alert banner showed **"[Demo] Obstacle ahead, two steps."**, Backend **Demo (simulated)**, Demo Mode **On**.
- **Accessible dashboard:** Camera / Frames / FPS / Backend / Inference / Alert cooldown / Demo Mode / Speech / Haptics / Privacy. High-contrast, large text, TalkBack content descriptions, assertive live-region on the alert banner.
- **Offline / privacy:** no networking code anywhere; verified running in Airplane Mode. UI states **"Local only · No network required"**.
- **Build:** `./gradlew assembleDebug` succeeds; APK installs and launches without crashing; survives permission denial (fallback screen).

## What is simulated (labeled as such in the UI)
- **Demo Mode events** are scripted (every alert prefixed `[Demo]`, backend reads "Demo (simulated)").
- The brightness heuristic is a fallback proxy for obstacles, not object recognition — used only when no model is loaded.

## What is NOT done (and the UI says so)
- **QNN / Hexagon NPU acceleration:** not active. The model runs on the **XNNPACK CPU delegate** (32 ms, under target). QNN lowering is documented and the SDK is available, but a QNN `.pte` additionally requires the Qualcomm HTP runtime `.so`s packaged in the APK; the app never claims NPU acceleration unless `forward backends` reports a QNN backend.
- **Doorway / stairs / curb / crosswalk detection:** not in the COCO-80 detector. Real person/vehicle/large-obstacle detection works; doors/stairs/curbs would need a model trained for them (documented future).
- **Translation mode:** mode shell only; no on-device translation model bundled yet (documented as missing, never cloud).

> Note: OCR (on-device ML Kit, on demand) and offline voice commands (`OfflineSpeechRecognizer` + `VoiceCommandParser`) are implemented in code; re-verify on device in the demo loop.

## Architecture readiness
The `VisionRuntime` interface lets a real `ExecuTorchVisionRuntime` drop in behind the same engine/output with no UI changes. See [docs/implementation-plan.md](implementation-plan.md) and `docs/wiki-ready/Technical-Architecture.md`.

## How to reproduce
See [docs/demo-script.md](demo-script.md) and `docs/wiki-ready/Validation-Gates.md`.
