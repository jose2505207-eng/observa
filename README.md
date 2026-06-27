# OBSERVA

**OBSERVA is an offline-first AI vision assistant for blind and low-vision users.**

Existing AI vision apps assume the user can already aim a camera at what matters. For a blind user, that *is* the problem. OBSERVA reverses the assumption: a lightweight, always-on, on-device model gives **continuous ambient awareness** of the surroundings first, then helps the user orient toward what's there.

## Promises

- **Offline-first** — the safety-critical core runs entirely on-device and works in airplane mode. Cloud is an optional enhancement, never a requirement.
- **Privacy-first** — camera imagery is processed locally and does not leave the device for any core function.
- **Accessibility-first & safety-first** — designed for non-visual use, built to never be confidently wrong about hazards.

Android-native, low-latency, battery-conscious, on-device inference (ExecuTorch).

## Current status

Early development. **Working today:** a single-activity Jetpack Compose app with an always-on CameraX analysis loop (frames processed and counted on a background thread) and a runtime camera-permission flow. ExecuTorch is bundled but **on-device model inference is not yet implemented** — that is the current build target.

This README intentionally states only what the repository proves. See the wiki for the full picture, including what is planned vs. researched.

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
