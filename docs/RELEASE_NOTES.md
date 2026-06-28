# OBSERVA release notes

Offline-first, privacy-first AI vision assistant for blind and low-vision users. All releases keep
`main` shippable: each builds, passes unit tests, has **no `INTERNET` permission**, and launches in
airplane mode with camera preview intact.

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
