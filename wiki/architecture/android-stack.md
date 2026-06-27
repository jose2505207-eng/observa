---
status: current
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Android Stack

> The Android-native foundation. Most of this section is Current Reality, verified from the repository.

## Current Reality
- **Language:** Kotlin 2.2.10.
- **Build:** Gradle (Kotlin DSL) with AGP 9.2.1; dependencies via the version catalog `gradle/libs.versions.toml`. See [[build-and-run]].
- **SDK:** `minSdk = 26`, `targetSdk = 36`, `compileSdk = 36`. Java 11 source/target.
- **UI:** Jetpack Compose (Compose BOM `2026.02.01`), Material 3. Single `MainActivity` (`com.observa.app`), all-Compose, no XML layouts/fragments. Theme in `ui/theme/` as `OBSERVATheme`.
- **Camera:** CameraX 1.6.1 (`camera-core`, `camera2`, `lifecycle`, `view`). Preview shown via Compose `AndroidView(PreviewView)`; `ImageAnalysis` runs on a single-thread executor.
- **Permissions:** Runtime camera permission via `rememberLauncherForActivityResult`; fallback screen until granted. Manifest also declares `RECORD_AUDIO`, `VIBRATE`, `WAKE_LOCK`, `POST_NOTIFICATIONS`, and foreground-service (camera/microphone) permissions.
- **On-device runtime:** ExecuTorch as a local AAR (`app/libs/executorch.aar`) — the load/inference path is called for real, with honest fallback when no `.pte` is bundled. See [[executorch-qnn]].
- **Foreground service:** `AmbientAwarenessService` (type `camera`) keeps OBSERVA foregrounded with a persistent honest notification + accessible Stop/Mute/Repeat actions; adaptive duty cycle from real battery/thermal. Device-verified. Screen-off background camera capture is a documented limitation (`docs/manual-test-foreground-service.md`).
- **R8 keep rules:** live in `app/src/main/keepRules/` (AGP merges that directory).

## Future Vision
- Screen-off background camera capture in the service for true always-on operation.
- TTS/haptic output integration ([[accessibility-principles]]).
- Possible modularization (capture / inference / output / skills) as the codebase grows.

## Related
- [[system-overview]] · [[build-and-run]] · [[performance-targets]]
