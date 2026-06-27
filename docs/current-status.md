# OBSERVA — Current Status (honesty ledger)

Last verified: 2026-06-27, on a physical device (Samsung, `R3CXC08009D`) in **Airplane Mode**, debug build.

## What works now (verified on device)
- **Camera loop:** CameraX preview + `ImageAnalysis`; live on device. Frames **~1400+**, **FPS ~30**.
- **Frame analysis:** per-frame luminance of left/center/right thirds computed on a background thread (no pixels stored or sent).
- **Brightness heuristic (real, on-device):** `HeuristicVisionRuntime` flags a markedly darker third as a possible obstacle. Deterministic proxy — **not** ML.
- **Hazard engine:** maps detections → alerts with **cooldown + scene memory** (announce → "Still …" → "Path clear"; no spam). Verified label "announced OBSTACLE_AHEAD".
- **Speech:** `TextToSpeech` alerts. Dashboard shows **Speech: Ready**.
- **Haptics:** severity-scaled vibration. Dashboard shows **Haptics: Available**.
- **Demo Mode:** deterministic scripted sequence; alert banner showed **"[Demo] Obstacle ahead, two steps."**, Backend **Demo (simulated)**, Demo Mode **On**.
- **Accessible dashboard:** Camera / Frames / FPS / Backend / Inference / Alert cooldown / Demo Mode / Speech / Haptics / Privacy. High-contrast, large text, TalkBack content descriptions, assertive live-region on the alert banner.
- **Offline / privacy:** no networking code anywhere; verified running in Airplane Mode. UI states **"Local only · No network required"**.
- **Build:** `./gradlew assembleDebug` succeeds; APK installs and launches without crashing; survives permission denial (fallback screen).

## What is simulated (labeled as such in the UI)
- **Demo Mode events** are scripted (every alert prefixed `[Demo]`, backend reads "Demo (simulated)").
- The live **brightness heuristic** is a proxy for obstacles, not object recognition. Backend reads "Heuristic (brightness)".

## What is NOT done (and the UI says so)
- **Real ExecuTorch inference:** the AAR is bundled but no model is loaded/run. UI reads **"ExecuTorch: bundled, not invoked"**.
- **QNN / Hexagon NPU acceleration:** not implemented. No NPU claim is made anywhere.
- **OCR:** not implemented (text alerts exist only as demo/heuristic labels, no real text recognition).
- **Voice input:** not implemented (`RECORD_AUDIO` declared, unused).
- Real person/doorway/stairs detection: only as demo events; no detector yet.

## Architecture readiness
The `VisionRuntime` interface lets a real `ExecuTorchVisionRuntime` drop in behind the same engine/output with no UI changes. See [docs/implementation-plan.md](implementation-plan.md) and `docs/wiki-ready/Technical-Architecture.md`.

## How to reproduce
See [docs/demo-script.md](demo-script.md) and `docs/wiki-ready/Validation-Gates.md`.
