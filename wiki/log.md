# Project Log

Newest entries first. Append an entry whenever you make a meaningful change to the code or wiki. Keep entries short and factual.

---

## 2026-06-27 — v2.1.0 QNN host AOT unblocked (real root cause + fix); YOLOv8n graph still blocks

Disciplined QNN/NPU unblock loop. App build + unit tests still green; no `INTERNET`; XNNPACK remains
the shipped detector. No device attached this session, so nothing QNN was device-claimed.

- **v2.0.0 host blocker root-caused and FIXED.** `QnnManager.InitBackend()` failed with
  `Failed to initialize QNN backend for kHtpBackend`. Native QNN logs (`QNN_LOG_LEVEL=DEBUG`) showed
  the real cause: `unknown custom config socModel 0` / device error 14001 — reproducing on SM8650 and
  SM8550 too, so not chipset-specific. It was an **ABI mismatch** between the prebuilt ExecuTorch
  **1.3.1 wheel's** QNN host pybind and **QNN SDK 2.47's** HTP device-config struct. Rebuilt the QNN
  host pybind from the local `executorch/` source tree against SDK 2.47 (`make PyQnnManagerAdaptor` in
  `build-x86`). Result: `InitBackend()` returns **0**, and a real Conv2d→ReLU model lowers fully to
  QNN HTP (valid `QnnBackend` `.pte`).
- **New blocker (model graph):** full YOLOv8n export fails QNN's `I64toI32` pass at the detection
  head's anchor decode (`expand: dimension 3 -> 1`). Model-graph issue, not QNN init. No QNN `.pte`
  shipped; no QNN device `.so`s packaged (would be dead weight without a model).
- `scripts/export_detector.py --qnn` switched to the correct `to_edge_transform_and_lower_to_qnn`
  helper (carries `soc_model` into the host device config; the bare partitioner path lost it → socModel 0).
- Docs: `docs/implementation/MODEL_RUNTIME.md` and `executorch-qnn.md` rewritten with the exact cause,
  fix, and remaining blockers. App runtime backend reporting unchanged (already truthful:
  `LOADED_QNN` only when `forward` backends include QNN).

---

## 2026-06-27 — v2.0.0 REAL on-device ExecuTorch inference (Agent 1 gate met)

Build + unit tests pass; no `INTERNET`; **device-verified on S25 Ultra (SM8750) in Airplane Mode**.

- **Real local ML detector shipped.** Bundled `assets/models/observa_detector.pte`: YOLOv8n COCO-80
  exported to ExecuTorch at 320×320 with the **XNNPACK** CPU delegate, using the local `executorch/`
  1.4.0a0 tree + torch 2.12.1 (`executorch/.venv`) so the `.pte` schema matches the bundled AAR.
- **Fixed the silent load blocker:** declared `com.facebook.fbjni:fbjni:0.7.0` +
  `com.facebook.soloader:nativeloader:0.10.5` — ExecuTorch's `Module` loads its `.so` via SoLoader;
  without these, `Module.load` threw `ClassNotFoundException: NativeLoader` → heuristic fallback.
- **Measured on device:** load ~11 ms; inference **median ~32 ms, p90 49 ms, p95 58 ms** (54-sample
  run); `forward backends=[XnnpackBackend]`. Under the 100 ms danger-recognition target. Portable
  kernels were ~4490 ms — XNNPACK is ~140× faster.
- Detector input 640→320, RGB capture 256→320 (match model input; favor latency/battery).
  `YoloDetectionParser` now selects the `[1,84,N]` tensor by shape among the 6 raw head outputs
  (regression test added). `objects=0` reported honestly when nothing recognizable is in frame.
- **QNN attempted, not shipped:** QNN SDK present (`qairt/2.47.0.260601`), `--qnn` export path added,
  but a QNN `.pte` needs the Qualcomm HTP runtime libs in the APK; XNNPACK already meets the target.
- Docs: `docs/implementation/MODEL_RUNTIME.md`, `AUDIT_CURRENT_STATE.md`; updated `executorch-qnn`
  (status → **current**), `PERFORMANCE_METRICS`, `current-status`, README, `assets/models/README`.

## 2026-06-28 — v1.10.0 physical-button hotkeys + honest find-exit (v2 Part D)

Branch `feature/v2-braille-hotkeys-accessibility`. Build + 122 unit tests pass; no `INTERNET`;
device-verified on S25 Ultra (Volume Up ×3 → OCR fired end-to-end, no crash).

- Foreground volume-rocker shortcuts (Layer 1), **off by default** so volume isn't hijacked.
  Pure `HotkeySequencer` (counted presses, 1200 ms window, debounce, button-reset) +
  `MainActivity.onKeyDown` (ACTION_DOWN only). Grammar: VolUp×1 repeat, ×2 status, ×3 read text,
  ×4 find exit; VolDown×1 mute, ×2 stop nav, ×3 emergency pause (hazards still fire). Every command
  confirmed by speech + Braille.
- **Honest find-exit:** runs one real OCR pass; guides only if the camera actually reads "exit",
  else "Exit not found. Try scanning slowly or use navigation." Never hallucinates an exit.
- Accessible "Button shortcuts" toggle. Global Accessibility-Service hotkeys (Layer 2) **documented
  as deferred** (privilege + unreliable Samsung volume capture) in `docs/HOTKEYS.md`.
- Tests +6.

**v2.0.0 gate still NOT met:** real detector model + offline map packs remain the blockers (see
the v1.9.0 entry). Not tagging v2.0.0.

## 2026-06-28 — v1.9.0 progressive lock-on haptic navigation (v2 Part C)

Branch `feature/v2-sensor-haptic-navigation`. Build + 116 unit tests pass; no `INTERNET`;
device-verified launch on S25 Ultra in Airplane Mode (no crash).

- `DirectionalLockHaptics` (pure): bearing error → progressive lock-on cue (searching / off-course /
  approaching / near / aligned / arrived / hazard) with tick interval 900→500→250 ms and rising
  amplitude; **hazard overrides** all nav haptics; poor/unreliable compass suppresses fine ticks and
  is announced; low accuracy widens cadence + caps amplitude. `HapticGuidanceMode` (off / safety only
  / navigation lock-on / full).
- `HapticCuePlayer` gains `lockTick(amplitude)` (hardware amplitude via `hasAmplitudeControl`, on/off
  fallback), `lockOn`, `arrived`; exposed through `SpatialCueEngine`.
- `ObservaController.navigationTick` drives lock-on from the real device compass + speaks alignment
  transitions ("Aligned. Continue forward.", "Compass accuracy poor. Use caution."); hazard recency
  preempts nav ticks. Accessible "Haptic mode" cycle control in the nav panel.
- `scripts/validate_production_apk.sh` (honest gate: build/tests/no-INTERNET hard-fail; model + map
  pack are WARN until they land). `docs/HAPTIC_LANGUAGE.md`. Tests +10.

**v2.0.0 gate NOT met (honest):** real detector model still unbundled (export toolchain IS
pip-installable — `executorch` 1.3.1 — but a successful heavy `torch`/`torchvision` export AND a
`.pte` that loads on our committed `executorch.aar` (version unstamped) AND an on-device detection is
an unverified chain not closed this turn); real OSM offline map packs not built (demo location only);
QNN not active; physical Braille not verified; lock-on haptic *feel* needs a human pass. **Not
tagging v2.0.0.**

## 2026-06-28 — v1.8.0 production demo & release readiness

Branch `feature/v1.8-final-production-demo`. Docs-only; build + 106 unit tests pass; no `INTERNET`.

- Rewrote README "Current status" to honest v1.7 reality (was stale at the v0 baseline).
- Added release/demo docs: `docs/FINAL_DEMO.md` (14-step offline script, real-vs-fallback called
  out), `docs/RELEASE_CHECKLIST.md`, `docs/KNOWN_LIMITATIONS.md`, `docs/PRIVACY_MODEL.md`,
  `docs/PERFORMANCE_METRICS.md` (measured vs not-measured-with-reason),
  `docs/ACCESSIBILITY_VALIDATION.md` + validation matrix, `docs/RELEASE_NOTES.md` (v1.4→v1.8).
- Honesty pass: no fake object recognition, no "QNN active" without `getBackends()` proof, no
  physical-Braille claim, no GPS precision claim — all gaps explicit.

**Repo state:** v1.6 (foreground service) and v1.7 (offline navigation) merged + tagged; v1.5
detector code merged but model artifact still unbundled (blocked); QNN never active.

## 2026-06-28 — v1.7.0 offline navigation (guidance-first)

Branch `feature/v1.7-offline-navigation`. Build + 106 unit tests pass; no `INTERNET` and no
location permission; device-verified on S25 Ultra in Airplane Mode.

- Guidance-first offline navigation (not a sighted map): `Geo` (haversine + bearing),
  `GuidanceEngine` (reuses `RelativeDirectionTranslator` for clock-face), `SavedDestination`/
  `DestinationStore` (offline, demo set), `NavigationSession` (throttled, arrival).
- Heading from the **real device compass** (`SensorNavFixProvider`, rotation-vector, offline);
  **location is a documented demo fix** (no live GPS / no `ACCESS_FINE_LOCATION` in this build),
  announced honestly via `sourceLabel`. GPS reported LOW so guidance always cautions.
- Guidance routes at NAVIGATION priority → TTS + Braille + repeat; **hazards override** (router).
- Voice: "navigate to <place>", "stop navigation", "where am I", "repeat"; accessible nav UI panel
  (Go/Where am I/Stop, TalkBack-labeled, testTag). Device: spoke "Destination ahead. home."
- Honest GPS/compass uncertainty: "GPS accuracy low. Use caution." / "Compass accuracy low.
  Calibrate phone." Docs: `docs/offline-navigation.md`, `docs/manual-test-offline-navigation.md`.

**Still not done:** live GPS provider + movement validation; map packs/road graph (straight-line
distances); v1.5 model artifact still unbundled; v1.8 release polish.

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
