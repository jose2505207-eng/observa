# OBSERVA

**OBSERVA is an offline-first AI vision assistant for blind and low-vision users.**

Existing AI vision apps assume the user can already aim a camera at what matters. For a blind user, that *is* the problem. OBSERVA reverses the assumption: a lightweight, always-on, on-device model gives **continuous ambient awareness** of the surroundings first, then helps the user orient toward what's there.

## Promises

- **Offline-first** — the safety-critical core runs entirely on-device and works in airplane mode. Cloud is an optional enhancement, never a requirement.
- **Privacy-first** — camera imagery is processed locally and does not leave the device for any core function.
- **Accessibility-first & safety-first** — designed for non-visual use, built to never be confidently wrong about hazards.

Android-native, low-latency, battery-conscious, on-device inference (ExecuTorch).

## Current status (v3.1.0 — NPU-accelerated, voice control everywhere)

**Working today, verified on a Galaxy S25 Ultra (Snapdragon 8 Elite) in Airplane Mode:**
- **Real on-device ML object detection on the Hexagon NPU** — YOLOv8n (COCO-80) exported to **ExecuTorch** (320×320), running on the **Qualcomm Hexagon NPU (HTP v79)** at **~2–3 ms** (`forward backends=[QnnBackend]`, `QNN/NPU ACTIVE`) — ~10–15× faster than the XNNPACK CPU path, which stays as the automatic fallback. Bundled as `assets/models/observa_detector.pte` and run locally; person/vehicle/large-obstacle detections drive the hazard engine. (Brightness heuristic remains only as a no-model fallback.)
- **Voice control of every feature + real-time voice-to-voice translation (v3.1.0)** — Volume-up ×3 (or the Voice Commands button) opens voice control; you can start/stop navigation, start/stop translation, read signs, download a map, and download any of ~45 languages by voice. `LiveVoiceTranslator` does listen → on-device ML Kit translate → speak-in-target-language, fully offline once the language pack is installed. **Download Map** can pull the named places around you from OpenStreetMap (provisioning build only) and use them offline as navigation destinations. Navigation now also emits **turn/arrival haptics**. Honest throughout about missing packs/voices.
- Always-on CameraX loop (~25–32 FPS) + **hazard engine** (cooldown, scene memory, no spam).
- **Unified output**: TTS speech, **directional audio cues** (stereo-panned), **directional haptics**, and a **Braille/TalkBack live-region** status — one router, priority `HAZARD > NAVIGATION > OCR > MODE > INFO` (hazards interrupt; cues survive mute).
- **Native accessibility operating layer**: stable **Current status / Last alert / Available actions** accessible nodes plus TalkBack/braille **custom actions** (start/pause awareness, open voice commands, repeat, OCR, scene question, translation, orientation, silence, debug) — the app is fully operable without visual buttons, with `stateDescription` semantics and no braille flooding.
- **Two-layer blind-first input**: native accessibility actions are the guaranteed path (**Layer A**); raw screen gestures (**Layer B** — triple-tap voice, swipe-up translation, swipe-down navigation, double-tap repeat, long-press push-to-talk) are wired **only when TalkBack is off**, since a screen reader owns one-finger gestures. Pure, unit-tested `input/BlindGestureController`; when TalkBack is on the app says "Gestures available through TalkBack actions." Hazards always interrupt.
- **Visible mode UI**: high-contrast, TalkBack-labeled buttons (no icon-only) — **Awareness · Navigate · Download Map · Translate · Download Languages · Voice Commands · Read Signs · Repeat Alert** — plus **Map Download** and **Language Download / Translate** screens, a **Navigation** card (live bearing guidance + offline map-pack status), and a collapsible **Debug Status** card. Detection keeps running across screens. See [`docs/implementation/OFFLINE_MAPS.md`](docs/implementation/OFFLINE_MAPS.md).
- **Real offline translation (ML Kit) + downloads**: **Download Languages** downloads Spanish/English models once (in the `provisioning` build, which has INTERNET) and then translates **fully offline** (in the default `demoOffline` build, which has **no INTERNET**). **Download Map** installs an offline demo waypoint pack with no network. Two flavors keep the privacy proof: `demoOffline` (no INTERNET) vs `provisioning` (`-setup`, INTERNET for one-time downloads). Never cloud, never fabricated. See [`docs/implementation/OFFLINE_TRANSLATION.md`](docs/implementation/OFFLINE_TRANSLATION.md).
- **On-demand OCR** (ML Kit bundled Latin model, fully offline) — "Read Text" reads signs aloud.
- **Offline voice commands** (on-device recognizer): observing on/off, describe scene, what is ahead, read text, braille on/off/status, navigate to, stop navigation, where am I, repeat, mute/unmute, help.
- **Foreground service** with a persistent honest notification ("running offline") + accessible Stop/Mute/Repeat, and an **adaptive battery/thermal duty cycle**.
- **Offline guidance-first navigation**: clock-face, heading-relative guidance (real compass) to saved destinations, with honest GPS/compass uncertainty.
- **GPS Orientation Lite**: real device-GPS + real compass heading/bearing/distance guidance in a six-way vocabulary ("Destination ahead-left, 40 meters. Turn slightly left.") with honest confidence (good / weak GPS / compass unstable), via `LocationManager` (no Play Services, no INTERNET). Built as `navigation/` modules (LocationProvider, CompassProvider, BearingCalculator, DestinationStore, OrientationGuidanceEngine, NavigationSafetyArbiter, OrientationController). Surfaced as TalkBack/braille actions (Start/Repeat/Stop orientation); hazards always interrupt. Not turn-by-turn maps. See [`docs/implementation/GPS_ORIENTATION.md`](docs/implementation/GPS_ORIENTATION.md).
- **Offline Translation Mode (honest readiness + real pipeline scaffold)**: a real readiness gate (offline language pack + local speech + local engine) over an honest turn pipeline (`LocalSpeechRecognizer → LanguageIdentifier → LocalTranslator → TranslationTurnManager → TranslationSpeechOutput`) that never fakes a translation and never uses the network — reports "ready offline" / "language pack missing" / "speech unavailable" / "engine not installed". Engine/pack assets deferred to a provisioning flavor. See [`docs/implementation/OFFLINE_TRANSLATION.md`](docs/implementation/OFFLINE_TRANSLATION.md).
- **No `INTERNET` permission** — the app physically cannot use the network (verified `aapt2`/`dumpsys`).

**Honestly NOT done (no faking):**
- **QNN/NPU acceleration — ACTIVE on device (2026-06-29).** The YOLOv8n detector runs on the **Hexagon NPU (HTP v79)** of the retail Galaxy S25 Ultra at **~2–3 ms** (`forward backends=[QnnBackend]`, `QNN/NPU ACTIVE`) — ~10–15× faster than the XNNPACK CPU path. The enabler was a single manifest line, `<uses-native-library android:name="libcdsprpc.so" android:required="false"/>`, granting the app access to the vendor cDSP FastRPC client on Android 12+ (the earlier `skel 4000` failure was this access rule, not a signing block). `LOADED_QNN` is set only after a real QNN warm-up `forward` succeeds; **XNNPACK CPU remains the automatic fallback** on devices without the library (`required="false"`). No INTERNET. See [`docs/implementation/MODEL_RUNTIME.md`](docs/implementation/MODEL_RUNTIME.md) and [`docs/implementation/NPU_DEBUG_REPORT.md`](docs/implementation/NPU_DEBUG_REPORT.md).
- **Doors / stairs / curbs / crosswalks** — not in COCO-80; would need a model trained for them.
- **Turn-by-turn street routing** — navigation gives real GPS + compass **orientation/bearing** guidance and offline named-place destinations, not rendered map tiles or street-by-street routes.
- **Physical Braille display** verified (app-level live-region exposed; no hardware tested) and a full **human sensory pass**.

> Earlier "not done" items now shipped: real ML Kit offline translation (incl. voice-to-voice), real device-GPS navigation, and Hexagon-NPU acceleration are all working and device-verified — see the log for the v2.5.0 → v3.1.0 history.

Demo: [`docs/FINAL_DEMO.md`](docs/FINAL_DEMO.md) · Limitations: [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md) · Privacy: [`docs/PRIVACY_MODEL.md`](docs/PRIVACY_MODEL.md) · Performance: [`docs/PERFORMANCE_METRICS.md`](docs/PERFORMANCE_METRICS.md) · Accessibility: [`docs/ACCESSIBILITY_VALIDATION.md`](docs/ACCESSIBILITY_VALIDATION.md), [`docs/demo/ACCESSIBILITY_DEMO.md`](docs/demo/ACCESSIBILITY_DEMO.md) · Release notes: [`docs/RELEASE_NOTES.md`](docs/RELEASE_NOTES.md).

## The wiki is the project memory

All project knowledge — vision, architecture, features, decisions (ADRs), roadmap, demo plan, and a running log — lives in [`wiki/`](wiki/README.md). It is the single source of truth.

- Start at [`wiki/README.md`](wiki/README.md) and [`wiki/index.md`](wiki/index.md).
- Architecture: [`wiki/architecture/system-overview.md`](wiki/architecture/system-overview.md), [`wiki/architecture/two-tier-inference.md`](wiki/architecture/two-tier-inference.md).

## Team Operating System

The project wiki contains requirements, roles, validation gates, development loops, demo plan, and GitHub workflow. **Team members should start there before changing code.**

The team wiki pages live in [`docs/wiki-ready/`](docs/wiki-ready/) (formatted for the GitHub Wiki). Start with [`docs/wiki-ready/Home.md`](docs/wiki-ready/Home.md). See that folder's README for how to publish them to the GitHub Wiki. Safe pushes are governed by the [`git-auto-push`](.claude/skills/git-auto-push/SKILL.md) skill.

## Get the APK (judges)

Pre-built debug APKs are attached to the [latest GitHub release](https://github.com/jose2505207-eng/observa/releases/latest). Two flavors:

- **`app-demoOffline-debug.apk`** — the default demo build with **no `INTERNET` permission** (the privacy proof). Use this for the airplane-mode walkthrough.
- **`app-provisioning-debug.apk`** — same app with one-time `INTERNET` only, used to download language packs / area maps; the data then works offline in the demoOffline build (same `applicationId`).

```sh
adb install -r app-demoOffline-debug.apk
adb shell dumpsys package com.observa.app | grep -i INTERNET   # → nothing (no network capability)
```

## Build from source

```sh
./gradlew assembleDemoOfflineDebug    # the no-INTERNET demo APK
./gradlew assembleProvisioningDebug   # the one-time-setup APK (downloads)
./gradlew test                        # JVM unit tests (190 green)
./gradlew connectedAndroidTest        # instrumented tests (needs a device)
```

Requires the Android SDK (set `sdk.dir` in `local.properties`) and a camera-equipped device/emulator. More in [`wiki/engineering/build-and-run.md`](wiki/engineering/build-and-run.md).
