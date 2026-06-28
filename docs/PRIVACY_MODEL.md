# Privacy model (production)

OBSERVA sees a user's most private spaces. Privacy is enforced by construction, not by policy.

## Guarantees (provable)
- **No network capability.** The app declares **no `INTERNET` permission** (and `ACCESS_NETWORK_STATE`
  is stripped via `tools:node="remove"`, since ML Kit pulls them transitively). It cannot open a
  socket. Verify: `aapt2 dump permissions app-debug.apk` and `adb shell dumpsys package com.observa.app`.
- **No image/video/frame upload, no streaming, no cloud inference.** All intelligence is on-device:
  brightness heuristic, the (bundled-when-present) ExecuTorch detector, ML Kit's **bundled** OCR
  model, and the platform **on-device** speech recognizer.
- **Transient frames.** Camera frames are analyzed in-memory and closed immediately; the OCR one-shot
  bitmap is recycled after recognition. Nothing is persisted.
- **Microphone** is used only for on-demand push-to-talk voice commands (no background recording).
- **Location** is not collected from GPS in this build (navigation uses a demo fix + compass); no
  `ACCESS_FINE_LOCATION` is requested.

## Verified
- Galaxy S25 Ultra, Airplane Mode: full functionality with no connectivity (`Active default network:
  none`); no INTERNET permission in the installed package.

## If online features are ever added
Any future online skill must be opt-in, per-skill, isolated from the safety core, and disclosed —
and must not weaken the airplane-mode core ([[privacy-model]] in the wiki,
`wiki/decisions/ADR-0003-skill-system-boundaries.md`).
