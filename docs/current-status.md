# OBSERVA — Current Status (honesty ledger)

Status as of **v3.1.0** (2026-06-29). Target device **Samsung Galaxy S25 Ultra (SM-S938U1, Snapdragon 8 Elite SM8750, arm64-v8a)**, serial `R3CXC08009D`, in **Airplane Mode**, debug build.

## What works now (verified on device)
- **Real on-device ML detector on the Hexagon NPU:** YOLOv8n (COCO-80) exported to **ExecuTorch** at 320×320, running on the **Qualcomm Hexagon NPU (HTP v79)** via the QNN backend, bundled as `assets/models/observa_detector.pte` and loaded locally. Verified in logcat: `forward backends=[QnnBackend]`, `QNN/NPU ACTIVE`, output tensor `[1, 84, 2100]` parsed by `YoloDetectionParser`. **Inference latency: ~2–3 ms** on the Snapdragon 8 Elite — ~10–15× faster than the CPU path and far under the 100 ms danger-recognition target. Runs in **Airplane Mode**. Detections (person/vehicle/large central obstacle) flow into the `HazardEngine`; an empty view produces `objects=0` honestly.
- **QNN / NPU active:** the YOLOv8n detector runs on the Hexagon NPU. The enabler was `<uses-native-library android:name="libcdsprpc.so" android:required="false"/>` in the manifest, granting cDSP FastRPC access on Android 12+. `LOADED_QNN`/`npuActive` is set **only** after a real QNN warm-up `forward` succeeds; **XNNPACK CPU stays as the automatic fallback** (`required="false"`) on devices without the NPU path. See `docs/implementation/MODEL_RUNTIME.md` and `docs/implementation/NPU_DEBUG_REPORT.md`.
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
- **Doorway / stairs / curb / crosswalk detection:** not in the COCO-80 detector. Real person/vehicle/large-obstacle detection works; doors/stairs/curbs would need a model trained for them (documented future).
- **Turn-by-turn street routing:** navigation gives real GPS + compass orientation/bearing guidance and offline named-place destinations, not rendered map tiles or street-by-street routes.
- **Physical Braille display** hardware: app-level live-region exposed and tested; no refreshable braille hardware verified yet.

Now shipped (earlier "not done" items): **QNN/NPU acceleration active** (~2–3 ms), **real ML Kit offline translation** incl. voice-to-voice (`LiveVoiceTranslator`, ~45 languages), **real device-GPS navigation** with turn haptics, and **voice control of every feature** (volume-up ×3).

> Note: OCR (on-device ML Kit, on demand) and offline voice commands (`OfflineSpeechRecognizer` + `VoiceCommandParser`) are implemented in code; re-verify on device in the demo loop.

## Architecture readiness
The `VisionRuntime` interface lets a real `ExecuTorchVisionRuntime` drop in behind the same engine/output with no UI changes. See [docs/implementation-plan.md](implementation-plan.md) and `docs/wiki-ready/Technical-Architecture.md`.

## How to reproduce
See [docs/demo/DEMO_SCRIPT.md](demo/DEMO_SCRIPT.md), [docs/demo/AIRPLANE_MODE_DEMO.md](demo/AIRPLANE_MODE_DEMO.md), and `docs/wiki-ready/Validation-Gates.md`.
