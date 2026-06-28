# OBSERVA release notes

Offline-first, privacy-first AI vision assistant for blind and low-vision users. All releases keep
`main` shippable: each builds, passes unit tests, has **no `INTERNET` permission**, and launches in
airplane mode with camera preview intact.

## Unreleased — GPS Orientation Lite + Offline Translation readiness

- **GPS Orientation Lite.** Real device GPS (`LocationManager`, no Play Services, **no INTERNET**) +
  compass → heading/bearing/distance, clock-face guidance ("slight right, 1 o'clock, 40 meters"),
  honest confidence (good / GPS low / compass calibrate). New `navigation/LocationProvider` +
  `OrientationController`, reusing the existing `nav/` geometry. Surfaced as TalkBack/braille actions
  (Start/Repeat/Stop orientation) + a live status line. Hazards always interrupt (NAVIGATION priority,
  rate-limited). Added `ACCESS_FINE/COARSE_LOCATION` only (location ≠ network). Not turn-by-turn maps.
- **Offline Translation Mode (honest readiness).** New `translation/TranslationModeController` +
  `OfflineLanguagePackManager`: a readiness gate that never fakes a translation and never uses the
  network — reports "ready offline" / "language pack missing" / "local speech unavailable". Engine +
  packs deferred to offline-after-install provisioning (bundling ML Kit Translate would add INTERNET).
- Operating layer extended with the new actions; `AccessibilityStatusReducer` carries orientation.
  +8 unit tests (orientation 5, translation 3) and +2 reducer cases. Build/tests green; **no INTERNET**.

## v2.2.0 — QNN/NPU detector pipeline (export→load) + native accessibility layer (not yet tagged)

### QNN/NPU (real pipeline; not active on the production handset — honest fallback)
- **YOLOv8n now lowers to QNN/HTP.** New `scripts/export_detector.py --qnn-raw-head` exports the raw
  multi-scale head (`[1,144,40/20/10²]`, pre-anchor-decode), avoiding the `make_anchors`/`I64toI32`
  `expand 3→1` failure. Produces a real **6.9 MB `QnnBackend` `.pte`** (`detector_yolov8n_qnn_sm8750.pte`).
- **On-device decode** in `YoloRawHeadParser` (DFL + dist2bbox + sigmoid + class-aware NMS; 5 unit
  tests) — detections identical to the XNNPACK path.
- **Packaged** the v79 device libs in `jniLibs/arm64-v8a` + `model_manifest.json`. `ExecuTorchDetector`
  tries QNN first and accepts it **only** after a successful warm-up `forward`, else XNNPACK.
- **Blocker on this device (OS-level, confirmed two ways):** the `.pte` loads but the production
  S25 Ultra cDSP rejects the unsigned HTP skel (`QnnDsp Failed to load skel, error 4000`), even with
  `ADSP_LIBRARY_PATH` set. Independently, the official **Qualcomm LiteRT QNN delegate** (`qnn-litert-
  delegate:2.28.0`, matched to the device's 2.27.5 firmware) fails **identically**, while signed
  system processes load cDSP skels in the same logcat. So a third-party app cannot use the NPU on this
  retail unit (needs signed PD / engineering build / root). **NPU is not claimed active**; detector
  runs XNNPACK CPU (~32 ms), status reads `XNNPACK CPU fallback. QNN attempted: <skel 4000>`. No
  `INTERNET`. (The LiteRT delegate was trialled then reverted — 40 MB, non-functional here.)

### Native TalkBack + braille operating layer
- **Operable without visual buttons.** New native operating layer: stable **Current status**, **Last
  alert**, **Available actions** accessible nodes, with TalkBack/braille **custom actions** for every
  core flow (start/pause awareness, repeat last alert, OCR, scene question, translation mode, silence
  alerts, open debug status). Four redundant non-visual paths: custom actions, voice, volume hotkeys,
  labeled controls.
- **Semantic state + no braille flooding.** `stateDescription` on toggles; "Awareness active" /
  "Detector backend: XNNPACK" semantics; operating-layer nodes are not live regions (read on demand).
  Hazard alerts stay highest priority (assertive). `AccessibilityStatusReducer.stripDebug` keeps
  confidence/bbox/latency out of user output.
- **Honest.** Translation = "not installed" (no model, never cloud); scene question = labeled
  brightness summary; backend from real `InferenceStatus`. Pure `AccessibilityStatusReducer` with 10
  unit tests. No `INTERNET`. XNNPACK detector path unchanged. Docs:
  `implementation/BRAILLE_ACCESSIBILITY.md`, `implementation/UX_INPUT_MAP.md`,
  `demo/ACCESSIBILITY_DEMO.md`.

## v2.1.0 — QNN host AOT unblocked (real fix); YOLOv8n graph still blocks
- **Solved the v2.0.0 QNN host blocker.** `QnnManager.InitBackend()` (`Failed to initialize QNN
  backend for kHtpBackend`) was an **ABI mismatch** between the prebuilt ExecuTorch 1.3.1 wheel's QNN
  host pybind and QNN SDK 2.47's HTP device-config (native logs: `unknown custom config socModel 0`,
  error 14001, reproducing on SM8650/SM8550). Rebuilding the QNN host pybind from the local
  `executorch/` source against SDK 2.47 fixes it: `InitBackend` returns 0 and a real model lowers to
  QNN HTP.
- **Not shipped:** the full YOLOv8n graph still fails QNN's `I64toI32` pass at the detection-head
  anchor decode (`expand: dimension 3 -> 1`) — a model-graph issue, not QNN init. No device this
  session to verify either. **XNNPACK CPU remains the shipped detector (32 ms, under target).** No
  fake QNN claims; the app still reports `LOADED_QNN` only when a model's `forward` backends include QNN.
- `scripts/export_detector.py --qnn` now uses `to_edge_transform_and_lower_to_qnn`. Docs:
  `implementation/MODEL_RUNTIME.md`, wiki `executorch-qnn` updated with the exact cause/fix/blocker.

## v2.0.0 — real on-device ExecuTorch inference
- **Ships a real local ML object detector.** YOLOv8n (COCO-80) → ExecuTorch, 320×320, **XNNPACK** CPU
  delegate, bundled as `assets/models/observa_detector.pte`. Verified on the Galaxy S25 Ultra
  (Snapdragon 8 Elite) in Airplane Mode: load ~11 ms, **inference median ~32 ms (p95 ~58 ms)**,
  `forward backends=[XnnpackBackend]` — under the 100 ms danger-recognition target.
- Fixed the silent load blocker (fbjni + soloader nativeloader runtime deps for ExecuTorch's
  `Module`). Detector 640→320 + RGB capture 256→320; parser selects the detection tensor by shape.
- QNN/NPU attempted (SDK present, `--qnn` export path) but not shipped — XNNPACK already meets target.
- Docs: `implementation/MODEL_RUNTIME.md`, `implementation/AUDIT_CURRENT_STATE.md`,
  `demo/AIRPLANE_MODE_DEMO.md`; wiki `executorch-qnn` → **current**; `PERFORMANCE_METRICS` updated.

## v1.8.0 — production demo & release readiness
- Final demo script, release checklist, validation matrix, and honest docs: `FINAL_DEMO.md`,
  `RELEASE_CHECKLIST.md`, `KNOWN_LIMITATIONS.md`, `PRIVACY_MODEL.md`, `PERFORMANCE_METRICS.md`,
  `ACCESSIBILITY_VALIDATION.md`, this file. README "Current status" rewritten to v1.7 reality.
- No runtime code changes; docs-only. Unit tests: 106 passing.

## v1.7.0 — offline navigation (guidance-first)
- Clock-face, heading-relative guidance to saved destinations using the **real device compass**;
  honest GPS/compass uncertainty; hazards override navigation. Voice + accessible nav UI.
- Documented demo location (no live GPS); straight-line bearing (no map packs). +14 tests.

## v1.6.0 — foreground service & reliability
- `AmbientAwarenessService` (foreground, persistent honest "running offline" notification with
  accessible Stop/Mute/Repeat); adaptive battery/thermal duty cycle; degraded-mode honesty. +12 tests.
- Documented limitation: screen-off background camera capture not yet wired.

## v1.5.0 — real detector path
- ExecuTorch `Module.load/forward` integration + `YoloDetectionParser` (COCO→person/vehicle/obstacle,
  NMS, direction), diagnostics, honest fallback. Reproducible export script.
- **No model artifact bundled** (blocked); QNN detected, not active.

## v1.4.0 — unified output + offline OCR
- One output router (TTS + Braille + audio + haptics + repeat; HAZARD>NAVIGATION>OCR>MODE>INFO).
  On-demand OCR via ML Kit bundled model; `INTERNET` stripped from the manifest.

## Earlier
- v1.3.0 — ExecuTorch loading path + Braille/TalkBack readiness; QNN checker fix.
- v1.2.0 — pure-module unit tests; offline device validation; "unmute" parse fix.
- v0.0.1 — camera/heuristic/hazard/TTS/haptics baseline + demo mode.

## APK
`app/build/outputs/apk/debug/app-debug.apk` (debug). Build: `./gradlew assembleDebug`.
