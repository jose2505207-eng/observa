# Project Log

Newest entries first. Append an entry whenever you make a meaningful change to the code or wiki. Keep entries short and factual.

---

## 2026-06-27 — P0 demo app implemented & verified on device

- Merged `auto/wiki-operating-system` into `main` (app baseline + team operating system), built and pushed.
- Built the P0 demo slice and **verified on a physical device in Airplane Mode**:
  - CameraX loop ~30 FPS; per-frame left/center/right luminance computed on a background thread (no pixels stored/sent).
  - `runtime/`: `VisionRuntime` interface + `HeuristicVisionRuntime` (brightness, real, on-device) + `ExecuTorchVisionRuntime` truthful stub (status: bundled, not invoked).
  - `hazard/`: `HazardType`/`Direction`/`Severity`, `HazardEngine` with cooldown + scene memory ("Still …" / "Path clear"; no spam).
  - `output/`: `Speaker` (TextToSpeech) + `Haptics` (severity-scaled vibration).
  - `demo/DemoScript`: deterministic scripted sequence through the same engine; alerts labeled `[Demo]`.
  - `ui/ObservaScreen` + `ObservaController`: accessible high-contrast dashboard, TalkBack labels, assertive live-region alert banner, Start Demo + Mute controls.
- **Truthful by construction:** UI shows `ExecuTorch: bundled, not invoked`; no NPU/QNN/real-inference claim. No networking code (offline by construction).
- Docs: added `docs/implementation-plan.md`, `docs/current-status.md`, `docs/demo-script.md`; updated README and `docs/wiki-ready/` (Demo-Plan, Product-Requirements).
- **Still not done:** real ExecuTorch/QNN inference, OCR, voice input.

## 2026-06-27 — Wiki bootstrap

- Adopted the **LLM Wiki** philosophy: `wiki/` is now the single source of truth. Removed the placeholder `CLAUDE_PROJECT.md` and repointed `CLAUDE.md` to the wiki.
- Created the full wiki structure (product, architecture, features, engineering, demo, decisions, roadmap) plus schema templates.
- Recorded **Current Reality** baseline from the repository:
  - Single-module Jetpack Compose app (`com.observa.app`), single `MainActivity`.
  - CameraX `Preview` + `ImageAnalysis` loop is wired and running; frames are counted on a background executor (`STRATEGY_KEEP_ONLY_LATEST`, `YUV_420_888`). See [[ambient-awareness]].
  - Runtime camera-permission flow with a fallback screen.
  - ExecuTorch ships as a local AAR (`app/libs/executorch.aar`) but is **not yet invoked** — no inference code exists. See [[executorch-qnn]].
  - Manifest declares audio + foreground-service (camera/mic) permissions; no foreground service is implemented yet.
- Authored ADRs: [[ADR-0001-offline-first]], [[ADR-0002-two-tier-architecture]], [[ADR-0003-skill-system-boundaries]].
- Added repository `README.md` pointing to the wiki.

**Status:** camera/analyzer scaffold is real; everything else (inference, OCR, VLM, skills, navigation, translation, voice) is `planned` or `research`.
