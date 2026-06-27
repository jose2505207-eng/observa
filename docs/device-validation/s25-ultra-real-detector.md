# Device Validation — S25 Ultra: real detector path (Airplane Mode)

Validation of the real-detector branch. **No `.pte` is bundled** in this environment, so the
detector path runs its honest fallback; the full inference/parser path + reproducible export are
delivered (see `docs/real-detector.md`).

## Environment
| Field | Value |
|---|---|
| Device | Galaxy S25 Ultra (SM-S938U1), Snapdragon 8 Elite, Android 16 (SDK 36) |
| Branch | `feature/real-detector-integration` |
| Airplane Mode | ON |
| INTERNET in APK | none (verified `aapt2 dump permissions`) |
| `.pte` bundled | **No** |

## Pass/Fail
| Check | Result | Evidence |
|---|---|---|
| clean assembleDebug | **PASS** | 37 tasks |
| testDebugUnitTest | **PASS** | 80 tests, 0 failures |
| No INTERNET permission | **PASS** | aapt2: none |
| Launches in airplane mode, no crash | **PASS** | pid alive; Camera Active; Frames 355 @ ~25 FPS |
| Camera preview visible | **PASS** | live preview |
| Model asset detected | **N/A (absent)** | `OBSERVA_EXECUTORCH: model absent … → heuristic fallback` |
| Model loads on device | **BLOCKED** | no `.pte` to load (see Blocked) |
| Real detection from camera | **BLOCKED** | requires a loaded model |
| Detection→audio/haptic/TTS/Braille routing | **PASS (code+tests)** | `DetectionToHazardTest`; runtime path wired through unified router |
| Fallback honest when model absent/invalid | **PASS** | UI "unavailable — heuristic fallback"; "AI detail: model absent · QNN delegate present (not active)" |
| QNN status truthful | **PASS** | delegate present, not active (no QNN-delegated model) |
| No fake labels | **PASS** | heuristic emits only generic OBSTACLE; parser drops unmapped classes |
| OCR / cues / Braille unaffected | **PASS** | unchanged from v1.4.0; OCR offline verified previously |

## Log excerpts
```
I OBSERVA_QNN: QNN delegate 'libqnn_executorch_backend.so' present (onDisk=false, inApk=true) but NOT active: no QNN-delegated model is loaded.
I OBSERVA_EXECUTORCH: model absent at assets/models/observa_detector.pte → heuristic fallback. QNN delegate present (not active)
```
Dashboard: AI model = "unavailable — heuristic fallback"; AI detail = "model absent · QNN delegate present (not active)".

## Blocked (and how to unblock)
On-device **model load** and **real detection** require `observa_detector.pte`, which is not
bundled here because: (a) no torch/executorch toolchain is installed in this environment, (b) the
mission forbids blind-downloading weights, and (c) a random `.pte` risks a schema mismatch with the
bundled `executorch.aar` (which would be reported as `FAILED` + fallback). To unblock:

1. `python scripts/export_detector.py --out app/src/main/assets/models/observa_detector.pte`
   (pin `executorch` to the AAR's version; YOLOv8 is AGPL — resolve licensing).
2. Rebuild, install, read `OBSERVA_MODEL` output shapes, run `docs/manual-test-real-detector.md`.
3. Record latency (target <100ms inference / <250ms alert) and QNN active/inactive here.

## Latency / performance
Not measurable without a loaded model. The diagnostics surface ("AI detail": last + avg ms,
input/output shapes, QNN) is implemented and will populate once a `.pte` is bundled.
