# Project Log

Newest entries first. Append an entry whenever you make a meaningful change to the code or wiki. Keep entries short and factual.

---

## 2026-06-28 — v1.6.0 foreground service & reliability

Branch `feature/v1.6-foreground-service`. Build + 92 unit tests pass; no `INTERNET`
permission; device-verified on S25 Ultra in Airplane Mode.

- Real **`AmbientAwarenessService`** (foreground, type `camera`) with a persistent honest
  notification ("OBSERVA running offline. … Observing. No network used.") and accessible
  **Stop / Mute / Repeat** actions routed via `ServiceBridge` → `ObservaController`. Device-verified:
  `isForeground=true types=0x40`, notification visible in the shade under airplane mode.
- Pure, unit-tested service layer: `ServiceStateReducer` (lifecycle/degrade transitions; camera
  loss/permission/thermal/low-battery → DEGRADED, not silently stopped), `NotificationContent`,
  `PermissionEducation`, `BatteryThermalPolicy`.
- **Adaptive duty cycle** from real battery + thermal (`PowerManager`/`BatteryManager`): the
  analysis loop interval scales (400 → 800 → 1500 ms) under heat/low-battery and reduces cue spam;
  shown in the new dashboard **Service** row.
- **Honest limitation:** true screen-off background camera capture is not yet wired (camera +
  inference still run with the Activity foregrounded); documented in
  `docs/manual-test-foreground-service.md`. No data leaves the device; service has no network.

**Still not done:** v1.5 model artifact (`observa_detector.pte`) not bundled (blocked: no
toolchain/AGPL); QNN never demonstrated active; v1.7 offline navigation; v1.8 release polish;
screen-off background capture.

## 2026-06-28 — Wiki sync: model loading, OCR, cues, real detector (v1.3.0–v1.5.0)

Brought the wiki in line with three shipped milestones (all device-verified on a
Galaxy S25 Ultra, Android 16, in Airplane Mode; build + unit tests green; no
`INTERNET` permission). Updated status pages: [[ambient-awareness]] → **current**,
[[ocr-mode]] → **current**, [[voice-command-layer]] → **current**,
[[privacy-model]] → **current**; [[executorch-qnn]] research→**planned** (integration
code real, model not bundled); [[spatial-guidance]] stays **planned** (directional cue
output shipped, aiming loop not).

- **v1.3.0 — real ExecuTorch loading path + Braille/TalkBack readiness.**
  `ExecuTorchDetector` really `Module.load`s a `.pte`, introspects `forward` backends,
  runs off-thread; honest `UNAVAILABLE/FAILED/LOADED_CPU/LOADED_QNN`. `QnnRuntimeChecker`
  fixed for `extractNativeLibs=false` (detects the delegate lib *in the APK*; never claims
  active). App-level Braille channel: `BrailleStatusPresenter` + polite live region +
  on/off toggle + voice "braille on/off/status"; direct `BrailleDisplayController` API
  intentionally deferred (`docs/accessibility/braille-support.md`). On-screen Observing
  toggle synced with voice; camera preview kept visible (scrollable layout + safe insets).
- **v1.4.0 — unified output + offline OCR.** One `AccessibilityOutputRouter` fans every
  event to TTS + Braille + audio cues + haptics + repeat store with
  `HAZARD>NAVIGATION>OCR>MODE>INFO` priority (hazards interrupt; cues survive mute).
  `SpatialCueEngine`/`AudioCuePlayer`/`HapticCuePlayer` (left/right/forward/urgent/confirm/
  error) + audio/haptic settings. On-demand OCR via ML Kit **bundled** Latin model
  (offline); `INTERNET`/`ACCESS_NETWORK_STATE` stripped via manifest merger.
- **v1.5.0 — real detector path.** `YoloDetectionParser` (COCO→person/vehicle/obstacle,
  threshold + class-aware NMS, left/center/right, hazard relevance, timestamp) +
  `DetectedObject` + diagnostics (load ms, last/avg latency, in/out shapes, backends, QNN
  active). Reproducible `scripts/export_detector.py` + `docs/real-detector.md`.

**Tests:** 80 unit tests passing. **Still not done / blocked:** `observa_detector.pte`
not bundled (no toolchain here + AGPL/version decisions) → on-device model load + real
ML detection are blocked but reproducible; QNN never demonstrated active; OCR translation,
conversational vision, navigation, and skills remain planned; physical Braille display +
human sensory pass not yet performed.

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
