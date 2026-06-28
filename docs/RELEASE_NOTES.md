# OBSERVA release notes

Offline-first, privacy-first AI vision assistant for blind and low-vision users. All releases keep
`main` shippable: each builds, passes unit tests, has **no `INTERNET` permission**, and launches in
airplane mode with camera preview intact.

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
