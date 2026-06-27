# Device Validation — S25 Ultra: model loading + Braille (Airplane Mode)

On-device validation of the real ExecuTorch model-loading path and the app-level
Braille status channel, performed fully offline.

## Environment

| Field | Value |
|---|---|
| Device | Samsung Galaxy S25 Ultra (`SM-S938U1`) |
| SoC | Snapdragon 8 Elite (`ro.board.platform=sun`), arm64 |
| Android | 16 (SDK 36) |
| ADB serial | `R3CXC08009D` |
| Date | 2026-06-28, ~07:35 KST |
| Commit tested | `148ba94` (branch `feature/model-loading-and-braille`) |
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Permissions | `CAMERA`, `RECORD_AUDIO` granted |
| Airplane Mode | **ON**; `Active default network: none` (no Wi-Fi/data) |
| `.pte` bundled | **No** (`assets/models/observa_detector.pte` absent) |

## Pass/Fail

| # | Check | Result | Evidence |
|---|---|---|---|
| A | Startup / camera / frames / no crash / offline | **PASS** | Camera Active, Frames 444→984 at ~30 FPS, no crash; Airplane Mode on; no INTERNET permission |
| B | Model loading honesty (absent → fallback) | **PASS** | `OBSERVA_EXECUTORCH: model absent … → heuristic fallback`; UI "AI model: unavailable — heuristic fallback"; no fake labels spoken |
| B | QNN honesty (detected ≠ active) | **PASS** | `OBSERVA_QNN: QNN delegate … present (onDisk=false, inApk=true) but NOT active`; fixes the prior false "absent" (extractNativeLibs=false) |
| C | Observing toggle ↔ voice sync | **PASS** | Tapping the on-screen Switch flipped Observing to **Off** (dashboard row + switch + spoken/braille "Observing off."); same `observe()` path backs voice "start/stop observing" |
| D | Braille / TalkBack readiness | **PARTIAL** | App-level polite live region shows composite `"OBSERVA: observing on, AI fallback"` on device; Braille toggle present and wired. TalkBack-read + **physical Braille display not verified** (no hardware) |
| E | Safety cues intact (TTS/haptics/audio, mute rules) | **PARTIAL** | Speech Ready, Haptics Available, Voice On-device ready; cue pipeline unchanged from the v1.2.0 pass. Audible/tactile confirmation deferred to a human pass |
| — | Camera preview not broken | **PASS (after fix)** | Added toggles initially squeezed the weighted preview to ~0; fixed via scrollable column + fixed 280dp preview (commit 148ba94); preview now visible |

## Log excerpts

```
I OBSERVA_QNN: QNN delegate 'libqnn_executorch_backend.so' present (onDisk=false, inApk=true) but NOT active: no QNN-delegated model is loaded.
I OBSERVA_EXECUTORCH: model absent at assets/models/observa_detector.pte → heuristic fallback. QNN delegate present (not active)
```

When a real `.pte` is added, expect additional lines: `load success in <ms>ms; methods=[…]; forward backends=[…]`, `inference <ms>ms; output tensor shapes=[…]`, and `OBSERVA_MODEL: parser=strict-unknown-shape … refusing to emit labels …` until a model-specific parser is written.

## Known issues

1. **No `.pte` bundled** → no ML detection yet (by design; heuristic fallback is honest). The loading path is real and exercised up to the asset check.
2. **Physical Braille display not verified** — only app-level live-region exposure confirmed; needs a paired display + TalkBack to fully validate.
3. **Sensory checks (TTS audibility, haptic feel, audio panning, real voice recognition) deferred** to a human-in-the-loop pass.
4. ExecuTorch native libs are arm64-v8a only; on a non-arm64 device load would fail → FAILED status + fallback (no crash).

## Next highest-impact fix

Bundle a small ExecuTorch-compatible detector exported to `observa_detector.pte`
(document source + export command + license in `assets/models/README.md`), read the
logged `OBSERVA_MODEL` output shapes on device, and implement a model-specific
`DetectionParser` for that exact format. Only after on-device verification should
object recognition be claimed. Then run a human sensory + physical-Braille pass.
