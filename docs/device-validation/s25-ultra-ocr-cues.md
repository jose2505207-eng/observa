# Device Validation — S25 Ultra: OCR, cues, output layer (Airplane Mode)

Offline validation of the production output layer (unified router, audio/haptic
cues, settings) and on-demand OCR.

## Environment

| Field | Value |
|---|---|
| Device | Samsung Galaxy S25 Ultra (`SM-S938U1`), Snapdragon 8 Elite, arm64 |
| Android | 16 (SDK 36) |
| ADB serial | `R3CXC08009D` |
| Date | 2026-06-28, ~07:58 KST |
| Branch | `feature/ocr-audio-haptics-production` |
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Airplane Mode | **ON** (`airplane_mode_on=1`) |
| Permissions in APK | **No INTERNET, no ACCESS_NETWORK_STATE** (stripped via manifest merger; verified with `aapt2 dump permissions`) |
| `.pte` detector | absent (heuristic fallback, by design) |

## Pass/Fail

| Check | Result | Evidence |
|---|---|---|
| Launches in Airplane Mode, no crash | **PASS** | pid alive; camera Active; Frames 372→ at ~30 FPS |
| Camera preview visible | **PASS** | live preview at top (fixed-height, scrollable layout) |
| No INTERNET permission | **PASS** | `aapt2 dump permissions` shows none; ML Kit network stripped |
| Model fallback honest | **PASS** | `OBSERVA_EXECUTORCH: model absent … → heuristic fallback`; UI "AI model: unavailable — heuristic fallback" |
| QNN honest (detected ≠ active) | **PASS** | `OBSERVA_QNN: … present (onDisk=false, inApk=true) but NOT active` |
| OCR on demand (offline) | **PASS** | Tapped **Read Text** → `OBSERVA_OCR: recognize: found=false, chars=0` → UI/spoken "No readable text found." (no text in view); ran fully offline, no crash, single call (not continuous) |
| OCR positive recognition | **NOT VERIFIED** | needs text physically in front of the camera — manual pass (see manual-test doc). Positive formatting is unit-tested |
| Unified controls present & reachable | **PASS** | Observing, Braille status, Audio cues, Haptics toggles + Read Text + Voice Command, all visible/scrollable |
| Audio cues / haptics audible-tactile | **PARTIAL** | engines fire (Demo path); audible/tactile confirmation deferred to human pass |
| Braille/TalkBack status | **PARTIAL** | live-region composite shown on device; physical Braille + TalkBack-read deferred |

## Log excerpts

```
I OBSERVA_QNN: QNN delegate 'libqnn_executorch_backend.so' present (onDisk=false, inApk=true) but NOT active: no QNN-delegated model is loaded.
I OBSERVA_EXECUTORCH: model absent at assets/models/observa_detector.pte → heuristic fallback. QNN delegate present (not active)
I OBSERVA_OCR: recognize: found=false, chars=0
```

## Known issues / not verified
1. **OCR positive read** not verified on device (no text presented to camera). No-text path verified; positive path unit-tested.
2. **Audio/haptic audibility, TalkBack speech, physical Braille** require a human sensory pass (see `docs/manual-test-sensory-accessibility.md`).
3. No ML detector model bundled → no object recognition (honest fallback).

## Next highest-impact
Run the human sensory pass (OCR on real signage, audio L/R panning, haptic patterns, TalkBack + Braille display), then bundle a real ExecuTorch detector + parser.
