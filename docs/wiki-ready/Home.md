# OBSERVA — Team Wiki (Home)

OBSERVA is an **offline, on-device AI accessibility assistant** for blind and low-vision users. Instead of asking the user to aim a camera at something, OBSERVA watches ambiently, understands the scene locally on Android, and proactively warns about hazards with speech, directional audio, and haptics — with no cloud dependency for the core experience.

## North Star

> **OBSERVA wins by proving that private, local, low-latency AI can make the physical world safer and more navigable for blind and low-vision users.**

## Current Objective

Ship a hackathon-ready build that demonstrates, in **Airplane Mode**, a continuous on-device ambient-awareness loop that produces understandable, non-spammy, directional alerts — with a visible performance/backend dashboard and an honest account of the ExecuTorch / Qualcomm QNN path.

## What "Done" Means

- App launches and runs the camera + ambient loop **fully offline** (Airplane Mode proof).
- At least one **understandable, directional, non-repeating** hazard/scene alert (speech and/or haptics).
- A **dashboard** showing FPS, frame count, and (if an inference path exists) latency + backend status.
- **Truthful** backend labeling (NPU/QNN claimed only if verified; CPU fallback labeled).
- A **clean documented build** (`./gradlew assembleDebug`) and a **judge-ready demo script**.
- Wiki reflects reality; known limitations documented honestly.

## Use the Wiki as the Source of Truth

This wiki is the team's operating system. **Read it before changing code.** It tells you what we're building, why it matters, what you own, which files to touch, how to avoid blocking others, how to validate, how to demo, and how to push safely. If behavior changes, update the relevant page in the same change.

## Map

- [[Product Requirements]] — REQ-### with acceptance criteria and an ownership map.
- [[Technical Architecture]] — the 6-layer pipeline and the Truthful Backend Policy.
- [[Team Roles]] — who owns what; inputs, outputs, files, done, handoff.
- [[Skills Matrix]] — skills → owners, backups, repo areas, validation, status.
- [[Development Loops]] — repeatable loops (feature, AI runtime, accessibility, demo, perf, docs).
- [[Validation Gates]] — gates A–I every change must pass.
- [[Demo Plan]] — judge script + backup plans.
- [[GitHub Workflow]] — branches, commit format, PR checklist, merge policy.
- [[Auto Push Skill]] — the `git-auto-push` safe-push skill.
- [[Parallel Work Plan]] — workstreams that run in parallel + integration rhythm.
- [[Troubleshooting]] — common failures and fixes.
- [[Hackathon Scoring Strategy]] — mapping each scoring axis to implementation.

## Current Reality (verified from the repo, 2026-06-27)

- Single-module Jetpack Compose app, `com.observa.app`, one `MainActivity`.
- CameraX preview + always-on `ImageAnalysis` loop; on-screen **frame counter** proves the loop runs (`STRATEGY_KEEP_ONLY_LATEST`, `YUV_420_888`, background executor).
- Runtime camera-permission flow with a fallback screen.
- ExecuTorch is bundled as a local AAR (`app/libs/executorch.aar`) but **not yet invoked** — no model loading, inference, TTS, haptics, OCR, or voice yet.
- Permissions declared: camera, audio, vibrate, wake-lock, notifications, foreground-service (camera/mic).

Everything beyond the camera/analyzer scaffold is **Planned / Needs verification**. See [[Product Requirements]] for status.
