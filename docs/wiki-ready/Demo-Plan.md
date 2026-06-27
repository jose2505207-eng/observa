# Demo Plan

## Title
**“OBSERVA: Local AI Eyes That Do Not Leak Your Life”**

## Narrative

1. **Problem.** Blind and low-vision users cannot reliably aim a camera at the thing they need described.
2. **Existing app flaw.** Most AI vision apps are reactive, cloud-based, and privacy-invasive.
3. **OBSERVA inversion.** OBSERVA watches ambiently, understands locally, and alerts proactively.
4. **Live proof.**
   - Turn on Airplane Mode.
   - Launch the app.
   - Show the camera running.
   - Show frame count / FPS.
   - Show backend status.
   - Walk through a hazard or object scenario.
   - App announces a directional alert.
   - Trigger OCR / text detection if available.
   - Show the performance dashboard.
   - Explain what runs locally.
5. **Technical punchline.** The app is designed around on-device inference with ExecuTorch and Qualcomm acceleration where available, with transparent fallback when not.
6. **User punchline.** The user never has to aim perfectly. OBSERVA orients them.

## Honesty note
Demo only what the build proves. As of 2026-06-27 (verified on device in Airplane Mode) that is: live camera ~30 FPS, on-device brightness heuristic, hazard engine with cooldown/scene memory, spoken + haptic alerts, deterministic Demo Mode, and an accessible dashboard. **Not** real ExecuTorch/QNN/NPU inference, OCR, or voice — the UI shows `ExecuTorch: bundled, not invoked` and labels Demo/heuristic as such. Describe real inference/QNN as the next build target. See [[Technical Architecture]] Truthful Backend Policy and `docs/current-status.md`. Judges reward honesty + instrumentation.

## Backup Plan
- **Model fails** → show deterministic Demo Mode (REQ-010).
- **Camera fails** → play recorded video.
- **QNN fails** → explain CPU fallback honestly and show the architecture path.
- **Audio fails** → use the visual overlay and haptics.
- **Phone install fails** → show build success and a screen recording.

## Pre-flight
Run the [[Validation Gates]] (A, C, E, F, G) and the [[Development Loops]] Demo Loop before presenting. Keep a charged spare device and a backup video on hand.
