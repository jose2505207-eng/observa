# Skills Matrix

Who can do what, where it lives in the repo, and how we prove it works. Owners map to [[Team Roles]]. Status: ✅ in place · 🟡 partial · 🔴 not started · ❓ needs verification.

| Skill | Needed For | Owner Role | Backup Role | Repo Areas | Validation Method | Status |
|-------|-----------|-----------|-------------|------------|-------------------|--------|
| Android Kotlin | Everything | Android Lead | AI Runtime Lead | `app/src/main/java/com/observa/app/` | Gate A build | ✅ |
| Jetpack Compose | UI shell, dashboard | Android Lead | Accessibility Lead | `MainActivity.kt`, `ui/theme/` | Gate A + visual | ✅ |
| CameraX | Frame input | Android Lead | Performance Lead | `MainActivity.kt` | Gate C camera | ✅ |
| Android permissions | Camera/audio/FGS | Android Lead | Accessibility Lead | `AndroidManifest.xml`, `MainActivity.kt` | Gate C | 🟡 (camera done) |
| TextToSpeech | Spoken alerts | Accessibility Lead | Perception Lead | `app/src/main/java/...` | Gate D / G | 🔴 |
| Vibrator / haptics | Directional warnings | Accessibility Lead | Android Lead | `app/src/main/java/...` | Gate D | 🔴 |
| Audio panning / spatial audio | Directional feedback | Accessibility Lead | Perception Lead | `app/src/main/java/...` | Gate D | 🔴 |
| ExecuTorch | On-device inference | AI Runtime Lead | Performance Lead | `app/libs/executorch.aar`, `app/src/main/java/...` | AI Runtime Loop + Gate F | ❓ (bundled, unused) |
| Qualcomm QNN | NPU acceleration | AI Runtime Lead | — | native libs, backend code | Runtime backend check (REQ-007) | ❓/🔴 |
| Model export / conversion | `.pte` models | AI Runtime Lead | Perception Lead | export scripts, `app/src/main/assets/` | Dummy inference | 🔴 |
| Tensor preprocessing | Model input | AI Runtime Lead | Perception Lead | frame processor module | Unit test | 🔴 |
| Detection postprocessing | Usable detections | Perception Lead | AI Runtime Lead | reasoning module | Gate D | 🔴 |
| OCR | Text reading (REQ-011) | AI Runtime Lead | Perception Lead | inference + parser | Manual read test | 🔴 |
| Accessibility testing | Eyes-free usability | Accessibility Lead | Demo Lead | UI/res | Gate G | 🟡 |
| TalkBack testing | Screen-reader UX | Accessibility Lead | Documentation Lead | UI/res | Gate G | 🟡 |
| Performance profiling | FPS/latency/memory | Performance Lead | AI Runtime Lead | instrumentation | Gate F | 🟡 (frame count) |
| Gradle build/debug | Reliable builds | Android Lead | Release Lead | `*.gradle.kts`, `libs.versions.toml` | Gate A | ✅ |
| Git branching | Parallel work | Release Lead | Documentation Lead | repo | Gate I | ✅ |
| GitHub Wiki updates | Team OS | Documentation Lead | Product Captain | `docs/wiki-ready/`, wiki repo | Gate H | 🟡 (ready, unpublished) |
| Demo scripting | Judge demo | Demo Lead | Product Captain | `docs/wiki-ready/Demo-Plan` | Demo Loop | 🟡 |
| README/documentation | Onboarding | Documentation Lead | Product Captain | `README.md`, `docs/` | Gate H | ✅ |

If a skill is 🔴 with no clear owner available, the Product Captain reassigns or descopes (see [[Hackathon Scoring Strategy]] for priority).
