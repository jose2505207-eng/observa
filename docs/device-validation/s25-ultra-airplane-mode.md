# Device Validation — Galaxy S25 Ultra (Airplane Mode)

Real on-device validation pass for the post-crash recovery + voice/cue/model
integration work, performed offline.

## Environment

| Field | Value |
|---|---|
| Device model | Samsung Galaxy S25 Ultra (`SM-S938U1`) |
| Manufacturer | samsung |
| SoC / platform | Snapdragon 8 Elite (`ro.board.platform=sun`), arm64 |
| Android version | 16 (SDK 36) |
| ADB serial | `R3CXC08009D` |
| Date/time (device) | 2026-06-28, ~06:51–06:57 KST |
| Commit tested | `f780fb38ac9b9ed39c1cdf7adec3c49087f63ddb` (branch `test/pure-modules-and-device-readiness`) |
| APK path | `app/build/outputs/apk/debug/app-debug.apk` (versionName 1.0, versionCode 1, ~26 MB) |
| Permissions granted | `CAMERA`, `RECORD_AUDIO` (both via `pm grant`) |
| Airplane Mode | **ON** (`settings get global airplane_mode_on` = 1) for the entire pass |
| Network permissions | **No `INTERNET` permission requested.** `ACCESS_NETWORK_STATE` present (merged from a dependency manifest; read-only, cannot transmit) |

## Pass/Fail summary

| # | Check | Result | Evidence |
|---|---|---|---|
| A | Startup integrity | **PASS** | App launched in Airplane Mode; camera preview rendered; Frames 1645→4060→5890 (increasing); FPS ~29.7–32.1; no crash; honest fallback logs |
| A | Honest model/QNN status | **PASS** | Dashboard "Inference: ExecuTorch: not connected (model absent · QNN absent (CPU fallback))"; matching logcat |
| B | Observing control | **PARTIAL** | "Observing: On" state field works; **no on-screen toggle button** — observing is changed via voice ("start/stop observing") only, which needs a human mic. Not tap-testable. |
| C | Voice commands | **DEFERRED** | "Voice: On-device ready" (offline ASR available). Requires human speech to exercise end-to-end; not automatable via adb. `unmute`≠`mute` fix is unit-tested. |
| D | Audio cues (directional) | **PARTIAL** | Output pipeline verified: demo emitted hazards through `SpatialCueEngine` (synthesized stereo `AudioTrack`). Left/right *audibility* requires human ears — deferred. |
| E | Haptics | **PARTIAL** | "Haptics: Available"; vibration HAL active during demo (`VibrationThread`/`SecVibrator-HAL`). Tactile distinguishability requires human touch — deferred. |
| F | TalkBack / Braille readiness | **PARTIAL** | Braille-status live region updated on device to "[Demo] Path clear." (screenshot). All controls have `contentDescription`/`liveRegion` in code. Actual TalkBack speech + physical Braille display require a human + hardware — deferred. |
| G | Privacy proof | **PASS** | Airplane Mode ON, app fully functional; **no `INTERNET` permission** → cannot transmit; no sign-in/upload/cloud prompt; no networking code in repo |

Legend: PASS = objectively verified via automation/logs/UI; PARTIAL = mechanism verified, human sensory/hardware confirmation outstanding; DEFERRED = cannot be exercised without a human in the loop.

## Log excerpts (`OBSERVA_*` and supporting)

```
I OBSERVA_QNN: No QNN libraries in /data/app/.../com.observa.app-.../lib/arm64 — CPU fallback (looked for [libQnnHtp.so, libQnnSystem.so, libqnn_executorch_backend.so])
I OBSERVA_EXECUTORCH: ExecuTorch init: model absent · QNN absent (CPU fallback) → status=not connected (modelPath=models/observa_detector.pte)
I OBSERVA_EXECUTORCH: No model bundled at assets/models/observa_detector.pte; running heuristic fallback.
I TextToSpeech: Setting up the connection to TTS engine...
I BLASTBufferQueue: [SurfaceView[com.observa.app/...MainActivity]] onFrameAvailable the first frame is available
D VibrationThread: Vibration 217 finished with EndInfo{status=FINISHED, ...}
```

Dashboard at runtime (screenshot): Camera **Active** · Frames **5890** · FPS **30.6** · Backend **Heuristic (brightness) (active)** · Inference **ExecuTorch: not connected (model absent · QNN absent (CPU fallback))** · Observing **On** · Speech **Ready** · Haptics **Available** · Voice **On-device ready** · Privacy **Local only · No network required**. Alert banner + braille row both showed **"[Demo] Path clear."** after the demo sequence completed.

## Known issues

1. **Voice Command button overlaps the system gesture navigation bar** — the bottom button is not inset for `WindowInsets.navigationBars`, so it is partially obscured and hard to tap (stray taps reached Recents during testing). Fix: apply `navigationBarsPadding()` / `Modifier.safeDrawingPadding()` to the root column.
2. **No on-screen "Observing" toggle** — observing can only be turned off/on by voice; there is no accessible tap control for it. Add a button so it is usable without speech.
3. **`ACCESS_NETWORK_STATE` is present** via dependency manifest merge. Harmless (no `INTERNET`), but for a strict privacy story consider a manifest `tools:node="remove"` to drop it.
4. **Sensory items (C/D/E/F) not human-verified** in this pass — audio directionality, haptic distinguishability, real voice recognition, TalkBack speech, and a physical Braille display still need a manual operator.

## Next recommended fix

Apply navigation-bar insets to the main screen (issue #1) and add an accessible on-screen Observing toggle (issue #2); then schedule a human-in-the-loop sensory pass (voice commands, stereo audio, haptics, TalkBack + Samsung Braille display) before any production claim. Do **not** add OCR / navigation / real ExecuTorch inference until that manual pass is recorded here.
