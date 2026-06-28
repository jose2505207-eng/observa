# Project Log

Newest entries first. Append an entry whenever you make a meaningful change to the code or wiki. Keep entries short and factual.

---

## 2026-06-29 — v3.1.0 — voice control everywhere, voice-to-voice translation, real area maps, nav haptics

Tag **v3.1.0**. Big interaction + feature update on top of the NPU-active v3.0.0.

- **Volume-up ×3 → voice commands** (`HotkeyCommand.VOICE_COMMANDS`).
- **Every feature by voice:** start/stop navigation, start/stop translation, read signs, download map,
  download ‹language› (any of ~45 via `LanguageCatalog`). Parser checks stop-variants before
  start-variants. `CommandActions`/`CommandRouter` extended; `CommandRouterTest` updated.
- **Real-time voice-to-voice translation:** `LiveVoiceTranslator` (listen → ML Kit translate → speak in
  target language via `Speaker.speakIn` → loop; `swap` for two-way). Download any language
  (`setTarget` + catalog). Honest about missing packs/voices.
- **Real "map of where I am":** `MapDownloadController.downloadAreaMap(lat,lon)` pulls nearby named
  places from the OSM Overpass API (provisioning/INTERNET only), stores offline, loads as nav
  destinations (`OfflineMapRepository.places()`). Real place data, not rendered tiles — labeled so.
- **Navigation haptics:** `SpatialCueEngine.navDirection` + `OrientationGuidance.direction` → left/right
  turn pulses, forward buzz when aligned, arrival pattern; detection runs concurrently, hazards interrupt.
- Build (both flavors) + 190 tests green; demoOffline no INTERNET; NPU detector unchanged (~2–3 ms).

---

## 2026-06-29 — v3.0.0 — NPU-accelerated offline assistant (tagged)

Milestone release. OBSERVA runs the YOLOv8n detector on the **Hexagon NPU** of the retail Galaxy S25
Ultra at **~2–3 ms** (`backends=[QnnBackend]`, real detections), fully offline. Tag **v3.0.0**.

What v3.0.0 contains (relative to v2.5.0):
- **QNN/NPU detector ACTIVE on device** — enabled by `<uses-native-library android:name="libcdsprpc.so"
  android:required="false"/>` (Android 12+ cDSP FastRPC access). XNNPACK CPU stays as the automatic
  fallback. `LOADED_QNN`/`npuActive` set only after a real warm-up forward; never faked.
- **Structured backend diagnostics** (`inference/`: BackendKind/Stage/Attempt/Diagnostics/Selector,
  single `OBSERVA_NPU` log tag) + visible **NPU Debug** screen (build/device identity, per-stage
  attempts, exact blocker, Copy Debug Report).
- **Live NPU usage graph** — **NPU Data** button + `NpuUsageTracker` + `NpuDataScreen` (latency +
  throughput, honest: no fabricated utilization %).
- Carries v2.5.0: visible Download Map / Download Languages, real ML Kit offline translation,
  GPS+compass navigation, two flavors (demoOffline no-INTERNET / provisioning), blind-first gestures +
  TalkBack operating layer.
- **Honesty correction:** the earlier "retail device hard-blocks NPU (Outcome B)" conclusion was wrong
  about the *cause* (it was the Android 12+ native-library rule, not a signing/PD block); docs corrected.
- Build (both flavors) + 183 tests green; demoOffline has **no INTERNET**.

---

## 2026-06-29 — 🎉 QNN/NPU detector ACTIVE on the S25 Ultra (blocker fixed)

The NPU now runs the detector. Branch `fix-npu-runtime`.

- **Fix:** one manifest line — `<uses-native-library android:name="libcdsprpc.so" android:required="false"/>`
  in `app/src/main/AndroidManifest.xml`. On Android 12+ the app could not `dlopen` the vendor cDSP
  FastRPC client (`libcdsprpc.so`), so QNN HTP failed to create the FastRPC transport → `error 4000` →
  `Failed to load skel`. Declaring it lets the app's linker namespace reach the vendor library; the v79
  skel then loads on the cDSP (domain 3) and HTP init succeeds. Credit: psiddh/executorch pr-20057.
- **Correction (honesty):** my earlier "retail device hard-blocks HTP via signing/protection-domain"
  conclusion (Outcome B) was **wrong about the cause**. The symptom (`skel 4000`, model-independent,
  cross-stack) was right; the cause was the missing native-library declaration, not signing.
- **Device-verified:** `OBSERVA_NPU stage=WARMUP_FORWARD success=true` → `stage=ACTIVE backends=[QnnBackend]`;
  `Successfully opened libQnnHtpV79Skel.so` + `remote_handle64_open … qnn_skel_handle_invoke … domain 3`;
  `OBSERVA_EXECUTORCH … forward backends=[QnnBackend]; parser=yolo-raw-head; QNN/NPU ACTIVE`;
  `OBSERVA_MODEL inference 2ms (avg 2ms)`. ~10–15× faster than XNNPACK CPU. Stable, no fallback, no crash.
- XNNPACK CPU kept as automatic fallback (`required=false`). demoOffline still no INTERNET. 177 tests
  green. Docs corrected (MODEL_RUNTIME, executorch-qnn, NPU_DEBUG_REPORT, README, release notes).

---

## 2026-06-29 — NPU Debug menu + structured backend diagnostics (release-blocker probe)

Investigated the "running on GPU/CPU not NPU" release blocker. Branch `fix-npu-runtime`.

- **Key finding: the app was NOT on GPU.** No GPU/Vulkan/LiteRT path is compiled in at all
  (`grep` over `app/src/main/java` is empty). The only fallback is **XNNPACK CPU**, now labeled.
- New `inference/` package: `BackendKind`, `BackendStage`, `BackendAttempt`, `BackendDiagnostics`,
  `BackendSelector`. Per-stage attempts logged under one grep tag **OBSERVA_NPU**. Device-verified:
  `EXECUTORCH_QNN MODEL_LOAD ok backends=[QnnBackend]` → `WARMUP_FORWARD fail` → `FALLBACK` →
  `XNNPACK_CPU ACTIVE ok`. `npuActive()` only after a real QNN warm-up+active.
- New `ui/NpuDebugScreen` (NPU Debug button + "Open NPU debug" TalkBack action): build/device identity,
  backend priority, per-stage attempts, `.pte` sha, exact blocker; Force QNN Attempt / Copy Debug Report
  / GPU-fallback toggle. Accessible summary node.
- NPU still blocked: QNN `.pte` loads, warm-up fails at HTP device-handle (`skel 4000` / `14001`),
  reproduced; in-app native-log read is blocked for untrusted_app (capture via adb). Doc:
  `docs/implementation/NPU_DEBUG_REPORT.md`. Build + 177 tests green; demoOffline no INTERNET. Not tagged.

---

## 2026-06-29 — v2.5.0: offline map & language DOWNLOADS + real ML Kit translation + flavors

The user couldn't see any way to download maps or language packs → fixed. Branch
`feature/offline-maps-languages-v2.5`, tagged **v2.5.0**.

- **Two flavors:** `demoOffline` (default, **no INTERNET**, airplane-mode proof) and `provisioning`
  (`-setup`, **adds INTERNET**) for one-time downloads. `BuildConfig.SETUP_MODE` gates network. Same
  applicationId so downloaded models/packs survive an `adb install -r` swap to the offline build.
  Verified: demoOffline APK has no INTERNET; provisioning has INTERNET + ACCESS_NETWORK_STATE.
- **Real translation via ML Kit Translate** (`com.google.mlkit:translate`): `MlKitOnDeviceTranslator`
  + `LanguageDownloadController` + `TranslationReadiness`. Download es/en once → translate fully
  offline. Never fabricated. New `ui/LanguageDownloadScreen` (download/delete/translate/result).
- **Map download:** new `maps/` package (`MapPackStatus`, `MapRegion`, `MapPackVerifier`,
  `MapPackRepository`, `MapDownloadController`). "Install demo map pack" = offline waypoint bundle
  (honest "Demo offline map pack"); area map gated to provisioning. New `ui/MapDownloadScreen`.
- **Visible buttons** Download Map + Download Languages (+ existing Awareness/Navigate/Translate/Voice/
  Read Signs/Repeat). Control Strip a11y actions gain download map/languages, repeat translation.
- Detector keeps running across screens. NPU one-shot still skel 4000 → honest XNNPACK fallback (~16–28
  ms). Build (both flavors) + 172 tests green. Device-verified buttons visible + actions present.
- Docs: `OFFLINE_MAPS.md`, `OFFLINE_TRANSLATION.md`, README, release notes updated.

---

## 2026-06-29 — Maps/Navigation + Translation made visible in the UI; debug screen; sign reading

The backend controllers existed but the user couldn't *see* Maps or Translate. Fixed: branch
`feature/maps-translate-visible-ui`.

- **Visible mode buttons** (high-contrast, `role=Button`, contentDescription/stateDescription, 72 dp,
  no icon-only): Awareness · Navigate · Translate · Voice Commands · Read Signs · Repeat Alert.
- **Navigation card** (live bearing guidance + map-pack status + Start/Stop/Repeat), **Translation
  card** (readiness + Start/Stop/Repeat), collapsible **Debug Status card**.
- New navigation modules: `NavigationModeController`, `OfflineMapPackManager` (not installed/download
  required/corrupt/ready offline), `OfflineMapRepository`, `RouteGuidanceEngine` (honest bearing
  fallback, never fakes turn-by-turn), `StreetSignTracker` (stability gate so sign OCR never runs every
  frame). New `translation/OfflineReadinessChecker`. Navigation works with **no map pack** (compass
  bearing); pack only adds the rendered/route layer; hazards still interrupt.
- **Read Signs** = one real ML Kit OCR pass ("Sign text: …" / "No readable sign text"); never fabricated.
- **Debug screen**: version + `BuildConfig.GIT_SHA`/`BUILD_TIME`, backend, QNN stage + error, map/lang
  pack, GPS, compass, OCR, voice, INTERNET (not declared), offline-readiness summary.
- Control Strip a11y actions updated (Navigate/Translate/Voice commands/Read signs/…).
- Device-verified: mode buttons visible + labeled, detector ~22 ms XNNPACK, NPU one-shot still
  skel 4000 → honest fallback. Build + 170 tests green; no INTERNET. NPU not active → not tagged.
  New doc `docs/implementation/OFFLINE_MAPS.md`.

---

## 2026-06-29 — QNN/NPU deep reverse-engineering → Outcome B (retail PD block), + stage instrumentation

Branch `npu-root-cause-reverse-engineering`. Reverse-engineered the skel-4000 failure end to end
(10-loop log: `docs/implementation/QNN_REVERSE_ENGINEERING_LOG.md`). **No working NPU path on this
retail unit; proven model-independent and device-side.**

- **ABI:** `libQnnHtpV79Skel.so` = ELF 32-bit QUALCOMM DSP6 (correct). Host libs AArch64. No mismatch.
- **Experiment F (decisive):** a tiny 2-op Conv2d→ReLU QNN `.pte` (67 KB) fails at the *identical* line
  as YOLO — `loadRemoteSymbols failed err 4000` / `device_handle 14001`. Failure is at HTP device-handle
  creation, before any graph → **not the model/export/parser**.
- **PD:** ExecuTorch QNN already defaults to `kHtpUnsignedPd` (the only mode a non-OEM app can ask for);
  device still refuses it. Signed PD needs an OEM signature.
- **Device:** SM-S938U1, verified-boot green, bootloader locked, `ro.debuggable=0`, SELinux enforcing;
  app domain `untrusted_app`; cDSP nodes are vendor-owned; no app AVC (DSP-side AEE err 4000), while the
  signed camera HAL uses the same cDSP.
- **Instrumentation:** new `qnnStage` (libs → model loaded → backend init/warm-up failed → active) in
  `ExecuTorchDetector` + `qnnStageLine` in the debug status. `LOADED_QNN` only on a real warm-up forward.
- **Outcome B.** External dependency to ever reach NPU: signed PD / OEM allowlist, platform-signed
  privileged app, engineering/userdebug firmware, Qualcomm AI Hub deployment, or a permissive device.
- Detector stays XNNPACK CPU (~22–32 ms). Build + 166 tests green; no INTERNET; accessibility untouched.
  **Not tagged v2.2.0** (NPU not active). Device dropped off USB during the final cosmetic re-install;
  all decisive measurements were captured while connected.

---

## 2026-06-29 — NPU re-audit (laptop-crash ruled out) + blind-first two-layer gestures

User feared the laptop crash-during-build broke the NPU. Re-audited the whole QNN pipeline from
scratch and repeated the device test. **It is not a build artifact.**

- **Host rebuild:** `cd executorch/build-x86 && make PyQnnManagerAdaptor` → `[100%] Built target`
  (clean). Env intact (`QNN_SDK_ROOT=…/qairt/2.47.0.260601`, `.venv`, device all present).
- **Re-export:** `python scripts/export_detector.py --qnn-raw-head --imgsz 320` → exit 0, every op
  `| True`, valid 6.9 MB `qnn-htp (SM8750/HTPv79)` `.pte` (6,887,296 bytes; sha differs from bundled
  only due to QNN-compiler non-determinism — functionally identical).
- **Device:** reinstalled, ran. The `.pte` **loads** (`Deserializing … QnnContextCustomProtocol`),
  HTP init fails **fresh**: `QnnDsp <E> Failed to load skel, error: 4000` → `device_handle … 14001` →
  `Init failed for backend QnnBackend`. App falls back to XNNPACK (~22 ms avg) and reports it.
- **Counter-proof:** same logcat shows the signed Samsung camera open an unsigned PD on the cDSP
  (`/vendor/dsp/cdsp/fastrpc_shell_unsigned_3`, `Created user PD … Unsigned:Y`) and load
  `libbitml_nsp_79na_skel.so`. Device HTP v79 skels live only in vendor-signed paths. → **retail
  OS/DSP protection-domain block**, needs signed/privileged/userdebug build or OEM allowlist.
- **Tag state:** `v2.2.0` already exists (→ `c4e5b4b`, CPU-only) with an *honest* annotation
  (XNNPACK fallback, retail DSP block). NOT re-tagged as NPU-complete; NPU is not active on device.

**Blind-first gestures.** New pure, unit-tested `input/BlindGestureController` (5 tests). Layer A =
native actions (+ **Open voice commands**). Layer B = raw gestures wired **only when TalkBack off**
(triple-tap voice, swipe-up translation, swipe-down orientation, double-tap repeat, long-press PTT),
tracked via `AccessibilityManager.isTouchExplorationEnabled`; TalkBack-on shows "Gestures available
through TalkBack actions." Device-verified: detector ~22 ms, gesture hint + "open voice commands"
present in the live a11y tree. Build + 166 tests green; no INTERNET. Docs/wiki updated.

---

## 2026-06-28 — Orientation + Translation: full named-module build-out

Completed the GPS Orientation Lite and Offline Translation feature set into the explicit module
structure, replacing the earlier "reuse existing classes" shortcut with real, single-responsibility,
unit-tested modules (each composes — never duplicates — the pure `nav/` math). Build + 161 unit tests
green; APK still has **no INTERNET** (only `ACCESS_FINE/COARSE_LOCATION`); detector + accessibility
core untouched.

- **`navigation/` (GPS Orientation Lite).** Added `CompassProvider` (real rotation-vector heading,
  decoupled from the demo-location `SensorNavFixProvider`), `BearingCalculator` (six-way relative
  direction: ahead / ahead-left / left / ahead-right / right / behind, delegating `nav/Geo`),
  `OrientationGuidanceEngine` (mission vocabulary + good/weak-GPS/compass-unstable confidence),
  `DestinationStore` (single configurable target, `setDestination(...)`), `NavigationSafetyArbiter`
  (suppresses guidance for a hold window after a hazard). `OrientationController` rewritten to compose
  them via `LocationSource`/`HeadingSource` seams. Tests: `OrientationControllerTest` (6, incl. hazard
  suppression) + `OrientationGuidanceTest` (7).
- **`translation/` (Offline Translation).** Added the honest pipeline `LocalSpeechRecognizer`,
  `LanguageIdentifier`, `LocalTranslator` (honesty anchor — Unavailable, never fabricates),
  `TranslationTurnManager`, `TranslationSpeechOutput`. `TranslationModeController` now gates on a third
  fact — a local engine — adding the honest `Translation engine not installed` state. Tests:
  `TranslationModeControllerTest` (4, incl. no-engine) + `TranslationPipelineTest` (4, incl. a real
  injected engine translating + speaking).
- `ObservaController` wires the new providers/pipeline; orientation guidance now passes `lastHazardMs`
  to the arbiter. Docs (`GPS_ORIENTATION.md`, `OFFLINE_TRANSLATION.md`, both demo scripts, presenter
  checklist, README, release notes) updated to the new modules/vocabulary.
- Pending unchanged: live outdoor GPS guidance via the TalkBack action on device (logic unit-tested).

---

## 2026-06-28 — GPS Orientation Lite + Offline Translation readiness

Two user-facing features added without touching the detector, accessibility core, or no-INTERNET
guarantee. Build + unit tests green (incl. 8 new tests); APK has `ACCESS_FINE/COARSE_LOCATION` but
**no INTERNET**; detector still XNNPACK ~28 ms on device; operating layer intact.

- **GPS Orientation Lite.** New `navigation/LocationProvider` (Android `LocationManager`, no Play
  Services, satellite GPS = offline) + `navigation/OrientationController` (+`LocationSource` seam for
  tests), reusing existing `nav/Geo`, `RelativeDirectionTranslator`, `GuidanceEngine`,
  `SensorNavFixProvider` compass — no duplication. Heading/bearing/distance + clock-face guidance +
  honest confidence. Emitted at NAVIGATION priority (hazards interrupt), rate-limited 4 s. TalkBack
  actions Start/Repeat/Stop orientation; current-status node appends the live orientation line.
  Manifest gained location perms only. `OrientationControllerTest` (5).
- **Offline Translation Mode (honest).** New `translation/TranslationModeController` +
  `OfflineLanguagePackManager` — a readiness gate (offline pack + local speech) that never fakes a
  translation and never uses the network; status is "ready offline" / "language pack missing" /
  "local speech unavailable". Engine/packs deferred to offline-after-install provisioning (ML Kit
  Translate would add INTERNET). `TranslationModeControllerTest` (3).
- `AccessibilityStatusReducer` carries orientation (+2 tests); operating layer + reducer updated.
- Docs: `GPS_ORIENTATION.md`, `OFFLINE_TRANSLATION.md`, `demo/ORIENTATION_DEMO.md`,
  `demo/TRANSLATION_DEMO.md`, presenter checklist, README, release notes.
- Pending: live outdoor GPS guidance via the TalkBack action on device (logic unit-tested; app
  launches with location granted, detector intact).

---

## 2026-06-28 — NPU rescue: confirmed OS-level retail block via a second stack (LiteRT QNN)

Attempted to make NPU real on the retail S25 Ultra. Found the device ships **signed vendor QNN 2.27.5**
HTP v79 skels (e.g. `/vendor/lib64/rfs/dsp/snap/libQnnHtpV79Skel.so`) but they are reserved for signed
system PDs. Pointing ExecuTorch QNN at the vendor signed skel still gave `skel 4000`. Then wired the
**official Qualcomm LiteRT QNN delegate** (`com.qualcomm.qti:qnn-litert-delegate:2.28.0` +
`qnn-runtime:2.28.0`, no INTERNET) with a TFLite probe model and `HTP_BACKEND` / unsigned PD — it
failed with the **identical** `Failed to load skel, error 4000` / `device_handle 14001`, while the
camera HAL loaded cDSP skels in the same logcat. Conclusion: **the retail unit (user build,
verified-boot green) blocks third-party apps from loading HTP skels on the cDSP** — NPU is unreachable
by any sideloaded stack (ExecuTorch QNN and Qualcomm LiteRT both blocked). The LiteRT experiment was
reverted (≈40 MB, non-functional); evidence captured in `MODEL_RUNTIME.md` / `executorch-qnn.md`.
**v2.2.0 still NOT tagged** — NPU is not active and we do not claim it. XNNPACK CPU detector +
accessibility unchanged; build/tests green; no INTERNET.

---

## 2026-06-28 — v2.2.0 QNN/NPU detector pipeline (export→load works; device DSP blocks HTP)

Real QNN/NPU path built end-to-end and validated on the connected S25 Ultra (`R3CXC08009D`). Build +
unit tests green (incl. 5 raw-head parser tests); APK has no `INTERNET`; XNNPACK detector preserved;
accessibility operating layer intact. **NPU not claimed active** (honest XNNPACK fallback). Not tagged.

- **Raw-head QNN export.** `export_detector.py --qnn-raw-head` swaps the YOLOv8 Detect head's forward
  to return the raw per-scale `cat(box-DFL, class-logits)` (`[1,144,40/20/10²]`) before anchor decode,
  dodging the `make_anchors`/`I64toI32` `expand 3→1` failure. Whole graph lowers to QNN/HTP → real
  6.9 MB `QnnBackend` `.pte` (`detector_yolov8n_qnn_sm8750.pte`).
- **On-device decode** `YoloRawHeadParser` (DFL→dist2bbox→sigmoid→class-aware NMS), unit-tested.
- **Packaged** v79 device libs in `jniLibs/arm64-v8a` (+`useLegacyPackaging` so the skel extracts);
  `model_manifest.json`; `ExecuTorchDetector` tries QNN first, accepts only on a successful warm-up
  `forward`, else XNNPACK. Status surfaces `backendStatusLine` truthfully; fixed a stale-status node
  (detectorBackend is now a Compose state set after model init).
- **Device blocker:** `.pte` loads, HTP init fails — `QnnDsp Failed to load skel, error 4000` /
  `device_handle ... error=14001`, unchanged after setting `ADSP_LIBRARY_PATH`. Production 8-Elite cDSP
  refuses an unsigned skel from `/data` (needs signed PD / `/vendor` skel / eng build). Device shows
  "Detector backend: XNNPACK", inference ~32 ms. Full detail: `docs/implementation/MODEL_RUNTIME.md`.

---

## 2026-06-27 — v2.2.0 native TalkBack + braille operating layer

App build + unit tests green (incl. 10 new reducer tests); no `INTERNET`; XNNPACK detector path and
hazard priority untouched.

- **Native operating layer.** Three stable, focusable accessible nodes — Current status, Last alert,
  Available actions — at the top of `ObservaScreen`. The Available-actions node carries Compose
  `CustomAccessibilityAction`s so every core flow is operable from TalkBack's actions menu (and via
  braille displays) without any visual button: start/pause awareness, repeat last alert, OCR, scene
  question, translation mode, silence alerts, open debug status.
- **Pure reducer** `AccessibilityStatusReducer` (+ `A11yState`, `DetectorBackend`) derives all node
  texts and `stateDescription` semantics; `stripDebug` guarantees confidence/bbox/latency never reach
  user output. Unit-tested (`AccessibilityStatusReducerTest`, 10 cases).
- **Semantic state.** Toggles now use `stateDescription` ("on"/"off"); status node carries
  "Awareness active/paused" and "Detector backend: XNNPACK". Operating-layer nodes are NOT live
  regions → no braille flooding; the assertive hazard banner + polite status line remain the push channels.
- **Honesty held.** Translation action says "not installed" (no model, never cloud); scene question is
  the labeled brightness summary; detector backend comes from real `InferenceStatus`.
- **Controller** gained `detectorBackend`, `translationInstalled`, `sceneQuestion()`,
  `startTranslation()`, `silenceAlerts()`, `announceDebugStatus()`, and reducer-backed node getters.
- Docs: `BRAILLE_ACCESSIBILITY.md` (gap closed), `UX_INPUT_MAP.md` (custom-actions + honest gesture
  table), new `docs/demo/ACCESSIBILITY_DEMO.md` (manual validation), README. Not tagged.

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
