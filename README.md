# OBSERVA

**OBSERVA is an offline-first AI vision assistant for blind and low-vision users.**

Existing AI vision apps assume the user can already aim a camera at what matters. For a blind user, that *is* the problem. OBSERVA reverses the assumption: a lightweight, always-on, on-device model gives **continuous ambient awareness** of the surroundings first, then helps the user orient toward what's there.

## Promises

- **Offline-first** — the safety-critical core runs entirely on-device and works in airplane mode. Cloud is an optional enhancement, never a requirement.
- **Privacy-first** — camera imagery is processed locally and does not leave the device for any core function.
- **Accessibility-first & safety-first** — designed for non-visual use, built to never be confidently wrong about hazards.

Android-native, low-latency, battery-conscious, on-device inference (ExecuTorch).

## Current status (v1.7)

**Working today, verified on a Galaxy S25 Ultra in Airplane Mode:**
- Always-on CameraX loop (~25–32 FPS) with an on-device **brightness heuristic** detector + **hazard engine** (cooldown, scene memory, no spam).
- **Unified output**: TTS speech, **directional audio cues** (stereo-panned), **directional haptics**, and a **Braille/TalkBack live-region** status — one router, priority `HAZARD > NAVIGATION > OCR > MODE > INFO` (hazards interrupt; cues survive mute).
- **On-demand OCR** (ML Kit bundled Latin model, fully offline) — "Read Text" reads signs aloud.
- **Offline voice commands** (on-device recognizer): observing on/off, describe scene, what is ahead, read text, braille on/off/status, navigate to, stop navigation, where am I, repeat, mute/unmute, help.
- **Foreground service** with a persistent honest notification ("running offline") + accessible Stop/Mute/Repeat, and an **adaptive battery/thermal duty cycle**.
- **Offline guidance-first navigation**: clock-face, heading-relative guidance (real compass) to saved destinations, with honest GPS/compass uncertainty.
- **No `INTERNET` permission** — the app physically cannot use the network (verified `aapt2`/`dumpsys`).

**Honestly NOT done (no faking):**
- Real ML **object recognition** — the ExecuTorch + YOLO path is implemented and unit-tested, but **no `observa_detector.pte` is bundled** (toolchain/license/version blockers; reproducible export in [`scripts/export_detector.py`](scripts/export_detector.py)). The live detector is the brightness heuristic; the UI says `AI model: unavailable — heuristic fallback`.
- **QNN acceleration** — the delegate library is detected but **not active** (no QNN-lowered model); never claimed active unless `MethodMetadata.getBackends()` proves it.
- **Live GPS** navigation (uses a documented demo location + real compass) and **map packs**.
- **Physical Braille display** verified (app-level live-region exposed; no hardware tested) and a full **human sensory pass**.

Demo: [`docs/FINAL_DEMO.md`](docs/FINAL_DEMO.md) · Limitations: [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md) · Privacy: [`docs/PRIVACY_MODEL.md`](docs/PRIVACY_MODEL.md) · Performance: [`docs/PERFORMANCE_METRICS.md`](docs/PERFORMANCE_METRICS.md) · Accessibility: [`docs/ACCESSIBILITY_VALIDATION.md`](docs/ACCESSIBILITY_VALIDATION.md) · Release notes: [`docs/RELEASE_NOTES.md`](docs/RELEASE_NOTES.md).

## The wiki is the project memory

All project knowledge — vision, architecture, features, decisions (ADRs), roadmap, demo plan, and a running log — lives in [`wiki/`](wiki/README.md). It is the single source of truth.

- Start at [`wiki/README.md`](wiki/README.md) and [`wiki/index.md`](wiki/index.md).
- Architecture: [`wiki/architecture/system-overview.md`](wiki/architecture/system-overview.md), [`wiki/architecture/two-tier-inference.md`](wiki/architecture/two-tier-inference.md).

## Team Operating System

The project wiki contains requirements, roles, validation gates, development loops, demo plan, and GitHub workflow. **Team members should start there before changing code.**

The team wiki pages live in [`docs/wiki-ready/`](docs/wiki-ready/) (formatted for the GitHub Wiki). Start with [`docs/wiki-ready/Home.md`](docs/wiki-ready/Home.md). See that folder's README for how to publish them to the GitHub Wiki. Safe pushes are governed by the [`git-auto-push`](.claude/skills/git-auto-push/SKILL.md) skill.

## Build & run

```sh
./gradlew assembleDebug     # build debug APK
./gradlew installDebug      # install on a connected device/emulator
./gradlew test              # JVM unit tests
./gradlew connectedAndroidTest  # instrumented tests (needs a device)
```

Requires the Android SDK (set `sdk.dir` in `local.properties`) and a camera-equipped device/emulator. More in [`wiki/engineering/build-and-run.md`](wiki/engineering/build-and-run.md).
