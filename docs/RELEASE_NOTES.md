# OBSERVA release notes

## v3.1.0 — voice control of everything, voice-to-voice translation, real area maps, nav haptics

- **Volume‑up ×3 → voice commands.** Remapped the triple volume‑up hotkey to open hold‑to‑talk voice
  commands (`HotkeyCommand.VOICE_COMMANDS`). Needs "Button shortcuts" enabled.
- **Every feature callable by voice.** New intents in the offline grammar: start/stop navigation,
  start/stop translation ("translate"), read signs, download map, **download ‹language›** (any of ~45
  named languages → ML Kit codes via `LanguageCatalog`). Wired through `VoiceCommandParser` /
  `CommandRouter` / `CommandActions` (stop‑variants parsed before start‑variants so "stop navigation"
  isn't misread).
- **Real‑time voice‑to‑voice translation.** `LiveVoiceTranslator`: listen (on‑device speech) → translate
  offline (ML Kit) → **speak in the target language** (`Speaker.speakIn`) → listen again. Continuous;
  `swap` flips direction for two‑way. Honest: says when a language pack or target TTS voice is missing,
  never fabricates. Download **any** language (`LanguageDownloadController.setTarget` + catalog).
- **Download a real map of where you are.** `MapDownloadController.downloadAreaMap(lat,lon)` fetches
  nearby named places (shops/amenities/transit) around your GPS from the free **OSM Overpass API** (no
  key; provisioning build / INTERNET only), stores them offline as map‑pack waypoints, and loads them as
  **navigation destinations** (`OfflineMapRepository.places()`). Honest: real local place data, not
  rendered street tiles. Works offline after the one‑time download.
- **Full navigation feedback.** Directional **haptics** now fire with guidance (`navDirection`: left/right
  pulse to turn, forward buzz when aligned, arrival pattern) on top of GPS+compass bearing speech, with
  object detection running concurrently and hazards still interrupting.
- Build (both flavors) + **190 tests** green (+ NewCommandsTest, LanguageCatalog). demoOffline has **no
  INTERNET**; downloads gated to the provisioning build. NPU detector unchanged (~2–3 ms, active).



Offline-first, privacy-first AI vision assistant for blind and low-vision users. All releases keep
`main` shippable: each builds, passes unit tests, has **no `INTERNET` permission**, and launches in
airplane mode with camera preview intact.

## Unreleased — 🎉 QNN/NPU detector ACTIVE on device (the release blocker is fixed)

- **The detector now runs on the Hexagon NPU** of the retail Galaxy S25 Ultra (SM8750, HTP v79):
  `forward backends=[QnnBackend]`, warm-up forward succeeds, `OBSERVA_NPU … stage=ACTIVE success=true`,
  **~2–3 ms/inference** (vs ~22–32 ms XNNPACK CPU — ~10–15×). Device-verified, demoOffline build, no
  INTERNET. `LOADED_QNN`/`npuActive` set only after the real warm-up forward.
- **Root cause + fix:** the prior `skel 4000` / `device_handle 14001` was **not** a signing /
  protection-domain block (earlier conclusion corrected). It was the **Android 12+ vendor-native-library
  access rule** — the app could not `dlopen` the vendor `libcdsprpc.so` FastRPC client, so QNN HTP could
  not create the cDSP transport. **Fix = one manifest line:**
  `<uses-native-library android:name="libcdsprpc.so" android:required="false"/>`. With it, the v79 skel
  loads on the cDSP (domain 3) and HTP init succeeds. Credit: github.com/psiddh/executorch pr-20057
  (`examples/qualcomm/qnn-htp-test`).
- **XNNPACK CPU is retained as the automatic fallback** (`required="false"`) for devices without the
  library. Detector parser switches to `yolo-raw-head` on the QNN path (decode on CPU), identical
  detections. Docs corrected: MODEL_RUNTIME, executorch-qnn (wiki), NPU_DEBUG_REPORT, README. 177 tests
  green. **This is genuinely NPU-active and may be tagged as such.**
- **Live NPU usage graph** — new **NPU Data** button (+ "Open NPU data" / "Speak NPU usage" TalkBack
  actions) opens `ui/NpuDataScreen`: a live Canvas line graph of per-inference latency (ms) plus
  throughput (inferences/sec), min/max/avg, backend, and total. Backed by a thread-safe
  `inference/NpuUsageTracker` fed from the detector (unit-tested, 4 tests). Honest by design — Android
  exposes no DSP utilization %, so it charts measured latency/throughput, never a fabricated percentage;
  accessible summary node + spoken-usage action carry the numbers for TalkBack. 183 tests green.

## Unreleased — NPU Debug menu + structured backend diagnostics (release-blocker investigation)

- **The app was NOT falling back to GPU.** There is **no GPU/Vulkan/LiteRT path compiled in** — the
  only fallback is **XNNPACK CPU**, now clearly labeled. Fallback chain: ExecuTorch QNN → XNNPACK CPU.
- **New `inference/` diagnostics package** (`BackendKind`, `BackendStage`, `BackendAttempt`,
  `BackendDiagnostics`, `BackendSelector`). Every backend attempt is recorded per stage and logged under
  one grep tag **`OBSERVA_NPU`** as `attempt=<backend> stage=<stage> success=<bool> detail=<msg>`.
  Device-verified trace: `EXECUTORCH_QNN MODEL_LOAD success=true backends=[QnnBackend]` →
  `WARMUP_FORWARD success=false` → `FALLBACK` → `XNNPACK_CPU ACTIVE success=true`. `npuActive()` is true
  only after a real QNN `WARMUP_FORWARD`+`ACTIVE` — XNNPACK ACTIVE is reported as CPU, never NPU.
- **New visible NPU Debug screen** (`ui/NpuDebugScreen.kt`), reachable from the **NPU Debug** button and
  the **"Open NPU debug"** TalkBack action. Shows build/device identity, backend priority, per-stage
  attempts, QNN `.pte` sha, and the exact blocker; controls: **Force QNN Attempt** (re-run init +
  warm-up), **Copy Debug Report** (clipboard, for judges/Qualcomm), **GPU Fallback Disabled/Allowed**.
  Accessible summary node. `BackendSelector.disableGpuFallback` defaults true.
- **NPU still blocked** on the retail S25 Ultra (`skel 4000` / `device_handle 14001` at HTP device-handle
  creation), proven model-independent and cross-stack (see `docs/implementation/NPU_DEBUG_REPORT.md`).
  Detector runs XNNPACK CPU ~16–32 ms. Build (both flavors) + **177 tests** green; no INTERNET in
  demoOffline. Not tagged NPU-complete.

## v2.5.0 — Offline map & language downloads, real ML Kit translation, two build flavors

**The user can now see and use Download Map and Download Languages.** Navigation and translation are
finished as visible, operable features with honest offline provisioning. NPU still blocked on retail
(skel 4000); not claimed active.

- **Two build flavors** (privacy-honest provisioning split):
  - `demoOffline` (default): **NO INTERNET** — proves airplane-mode runtime. Verified: `aapt2` shows no
    INTERNET/ACCESS_NETWORK_STATE.
  - `provisioning` (`-setup`): **adds INTERNET + ACCESS_NETWORK_STATE** for one-time map/language
    downloads. `BuildConfig.SETUP_MODE` gates network actions. Same `applicationId`, so models/packs
    downloaded by the provisioning build persist for an `adb install -r` of the demoOffline build.
- **Download Languages = real on-device ML Kit Translation.** `MlKitOnDeviceTranslator` +
  `LanguageDownloadController`: download Spanish/English models once (provisioning build), then
  translate **fully offline** (works in demoOffline with no INTERNET). Never fabricated — result is
  ML Kit's actual output; readiness comes from ML Kit's downloaded-model set. New Language/Translation
  screen with source/target, download/delete, a text field, Translate, result, and an honest
  voice-input availability line. Test phrase "Where is the entrance?".
- **Download Map.** `maps/` package: `MapPackStatus`, `MapRegion`, `MapPackVerifier`,
  `MapPackRepository`, `MapDownloadController`. "Install demo map pack" writes an offline OBSERVA
  waypoint bundle (works with no network, labeled "Demo offline map pack"); "Download area map" is
  honestly gated to the provisioning build. "Ready offline" only when a verified file exists. New Map
  Download screen (install/delete/status/progress).
- **Visible buttons**: Awareness · Navigate · **Download Map** · Translate · **Download Languages** ·
  Voice Commands · Read Signs · Repeat Alert (role=Button, contentDescription, large targets). New
  Map/Language sub-screens; detector keeps running across screens.
- **Accessibility Control Strip** gains: download map, translate, download languages, repeat
  translation, repeat navigation, read signs, voice commands.
- Build (both flavors) + **172 tests** green (+ OfflineProvisioningTest: TranslationReadinessRules,
  MapPackVerifier). Device-verified: download buttons visible, detector ~16–28 ms, NPU one-shot still
  skel 4000 → honest XNNPACK fallback.

## Unreleased — Visible Maps/Navigation + Translation UI, debug screen, sign reading

- **Maps/Navigation and Translation are now visible and usable in the UI**, not just backend
  controllers. New high-contrast, TalkBack-labeled mode buttons (`role=Button`, contentDescription,
  stateDescription, 72 dp targets, no icon-only): **Awareness · Navigate · Translate · Voice Commands ·
  Read Signs · Repeat Alert**. New **Navigation** card (live guidance + map-pack status + Start/Stop/
  Repeat), **Translation** card (readiness + Start/Stop/Repeat), and a collapsible **Debug Status** card.
- **Navigation Mode** = real compass + GPS bearing guidance (works with no map pack) + honest offline
  map-pack status. New `navigation/NavigationModeController`, `OfflineMapPackManager` (not installed /
  download required / corrupt / ready offline), `OfflineMapRepository`, `RouteGuidanceEngine` (honest
  bearing fallback — never claims turn-by-turn without route data), `StreetSignTracker` (stability gate
  so sign OCR never runs every frame / blocks hazards). Hazards still interrupt navigation
  (`NavigationSafetyArbiter` + router priority).
- **Read Signs** runs one real ML Kit OCR pass and speaks "Sign text: …" or "No readable sign text" —
  never fabricated. No street-sign *detector* is bundled, so auto-trigger stays inactive (honest).
- **Debug/Status screen** shows version + git sha + build time (new `BuildConfig.GIT_SHA`/`BUILD_TIME`),
  detector backend, `QNN stage`, QNN error, map/language pack status, GPS, compass, OCR, voice, INTERNET
  (not declared), and an offline-readiness summary (`OfflineReadinessChecker`).
- Control Strip accessibility actions updated: Navigate / Repeat navigation / Translate / Voice commands
  / Read signs / Repeat last alert / Silence alerts / Debug status.
- Build + **170 tests** green (+ NavigationModulesTest: RouteGuidanceEngine, NavigationSafetyArbiter,
  StreetSignTracker). **No INTERNET.** Detector XNNPACK ~22 ms. NPU still blocked (Outcome B); not tagged.

## Unreleased — QNN/NPU reverse-engineering (Outcome B) + stage instrumentation

- **Reverse-engineered the skel-4000 failure end to end** (branch `npu-root-cause-reverse-engineering`;
  full matrix in `docs/implementation/QNN_REVERSE_ENGINEERING_LOG.md`). Proven **model-independent and
  device-side**: a tiny 2-op Conv2d→ReLU QNN `.pte` (67 KB) fails at the *identical* first line as YOLO
  (`loadRemoteSymbols err 4000` → `device_handle 14001`), i.e. at HTP device-handle creation before any
  graph runs. ABI verified correct (DSP6 skel). ExecuTorch QNN already uses the only PD mode a non-OEM
  app can request (`kHtpUnsignedPd`) and the device still refuses it. Device is locked retail
  (`SM-S938U1`, verified-boot green, bootloader locked, `ro.debuggable=0`, SELinux enforcing); app runs
  as `untrusted_app`; the signed camera HAL uses the same cDSP. **Verdict: NPU unreachable from a normal
  sideloaded app — needs signed PD/OEM allowlist, privileged/platform-signed app, engineering/userdebug
  firmware, Qualcomm AI Hub deployment, or a permissive device.**
- **Honest instrumentation:** new `QNN stage:` line in the debug status (libs → model loaded → backend
  init / warm-up failed → active). `LOADED_QNN` still set only after a real QNN warm-up `forward`.
- Detector stays **XNNPACK CPU (~22–32 ms)**. Build + 166 tests green; **no INTERNET**; accessibility
  untouched. **Not tagged v2.2.0** (NPU not active on device).

## Unreleased — NPU re-validation + blind-first two-layer gestures

- **NPU re-audited from scratch (laptop-crash hypothesis ruled out).** Rebuilt the QNN host adaptor
  (`make PyQnnManagerAdaptor` → built clean) and **re-exported** the QNN raw-head detector
  (`--qnn-raw-head --imgsz 320` → all ops lower to HTP, valid 6.9 MB `qnn-htp SM8750/HTPv79` `.pte`).
  Reinstalled and re-ran on the retail S25 Ultra: the `.pte` **loads/deserializes**, but HTP init
  fails fresh at `Failed to load skel, error: 4000` / `device_handle … error=14001`. In the **same**
  logcat the signed Samsung camera opens an unsigned PD on the cDSP and loads its HTP skel — so the
  DSP works; it refuses the third-party app's protection domain. **Verdict: retail OS/DSP
  protection-domain block, not a build artifact, not app code.** Detector runs **XNNPACK CPU
  (~22–32 ms)** and says so. No INTERNET. Not tagged NPU-complete (NPU is not active on device).
- **Blind-first two-layer input.** New pure, unit-tested `input/BlindGestureController`. **Layer A**
  (guaranteed): native `CustomAccessibilityAction`s, now including **Open voice commands**. **Layer B**
  (TalkBack OFF only): raw gestures on the camera surface — triple-tap → voice, swipe-up → translation,
  swipe-down → orientation, double-tap → repeat, long-press → push-to-talk. `TrackTalkBackState`
  watches `AccessibilityManager.isTouchExplorationEnabled`; when TalkBack is on, raw gestures are not
  wired and the hint reads "Gestures available through TalkBack actions." Hazards still interrupt.
  +5 unit tests (166-test suite). Build/tests green; **no INTERNET**.

## Unreleased — GPS Orientation Lite + Offline Translation readiness

- **GPS Orientation Lite.** Real device GPS (`LocationManager`, no Play Services, **no INTERNET**) +
  real compass → heading/bearing/distance with a six-way relative-direction vocabulary
  ("Destination ahead-left, 40 meters. Turn slightly left.") and honest confidence
  (good / weak GPS / compass unstable). Built as the named `navigation/` modules: `LocationProvider`,
  `CompassProvider`, `BearingCalculator`, `DestinationStore`, `OrientationGuidanceEngine`,
  `NavigationSafetyArbiter`, orchestrated by `OrientationController` (delegating the great-circle math
  to `nav/Geo` — no duplication). Surfaced as TalkBack/braille actions (Start/Repeat/Stop orientation)
  + a live status line. Hazards always interrupt: NAVIGATION priority **and** a `NavigationSafetyArbiter`
  hold window, plus rate-limiting. Added `ACCESS_FINE/COARSE_LOCATION` only (location ≠ network).
  Not turn-by-turn maps.
- **Offline Translation Mode (honest readiness + real pipeline scaffold).** The full offline turn
  pipeline is implemented as honest boundaries — `translation/LocalSpeechRecognizer`,
  `LanguageIdentifier`, `LocalTranslator` (the honesty anchor: returns Unavailable, never fabricates),
  `TranslationTurnManager`, `TranslationSpeechOutput` — gated by `TranslationModeController` +
  `OfflineLanguagePackManager`. Status reports "ready offline" / "language pack missing" / "local
  speech unavailable" / "engine not installed". Never fakes a translation, never uses the network;
  engine/pack assets deferred to offline-after-install provisioning (bundling ML Kit Translate would
  add INTERNET). `TranslationPipelineTest` proves a turn translates and speaks when a real engine is
  injected.
- Operating layer carries the new actions; `AccessibilityStatusReducer` carries orientation. Feature
  is covered by **21 unit tests** (orientation 13, translation 8) within a 161-test suite. Build/tests
  green; **no INTERNET**.

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
