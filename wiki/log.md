# Project Log

Newest entries first. Append an entry whenever you make a meaningful change to the code or wiki. Keep entries short and factual.

---

## 2026-06-27 — Post-crash recovery + voice/cue/model integration

Recovered uncommitted work from a laptop crash (4 untracked packages,
no tracked files modified, no conflict artifacts) and integrated it.
Built after each step (`./gradlew assembleDebug` SUCCESSFUL). Branch:
`recover/post-crash-voice-cues`.

- **Recovered & checkpointed** `accessibility/`, `cue/`, `voice/`,
  `nav/` (all compiled clean but were unwired).
- **Accessibility output** (`AccessibilityOutputRouter`): hazards now fan
  out to speech + a braille-friendly **polite live region** + a
  last-message store for "Repeat", with per-message rate limiting.
- **Spatial cues** (`SpatialCueEngine`): synthesized stereo-panned audio
  (`AudioTrack`, in-memory, offline) + **directional haptic patterns**
  (left/right/forward/urgent). Non-speech cues stay on when speech is
  muted (safety).
- **Offline voice control**: `OfflineSpeechRecognizer` (prefers on-device
  ASR, `EXTRA_PREFER_OFFLINE`) + deterministic `VoiceCommandParser` +
  `CommandRouter` (low-confidence confirmation) + `PushToTalkController`,
  bridged via `VoiceActions`. Commands: start/stop observing, describe
  scene, what is ahead, read text, repeat, mute/unmute, help. Unbuilt
  capabilities (navigate/find/where-am-I/read-text) answer **honestly**.
- **Observing toggle** gates the live heuristic; **describe scene / what
  is ahead** give an honest brightness summary (no vision model yet).
- **Model scaffolding (P5)**: `assets/models/` + `ExecuTorchDetector`
  (validates whether a `.pte` is bundled; `NOT_CONNECTED`/`BUNDLED_NOT_
  INVOKED`, never fabricates detections) + `QnnRuntimeChecker` (detects
  QNN native libs; CPU fallback when absent). Clear logcat
  (`OBSERVA_EXECUTORCH` / `OBSERVA_QNN`). Removed superseded
  `ExecuTorchVisionRuntime` stub.
- `MainActivity` requests optional `RECORD_AUDIO` and inits voice; camera
  still gates the app.

**Still not done:** real ExecuTorch/QNN inference, object-detection
model, OCR/text reading, GPS/navigation, on-device device-verified run of
this build.

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
