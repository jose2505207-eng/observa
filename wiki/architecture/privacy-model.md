---
status: planned
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Privacy Model

> A vision assistant sees the user's most private spaces. Privacy is a first-class requirement, not a setting.

## Guarantees (target)
- **On-device by default.** Camera frames are processed locally and not transmitted for any core function ([[offline-first-design]]).
- **No silent recording.** Frames are analyzed transiently; no persistent video/photo storage unless the user explicitly asks.
- **Explicit, scoped sharing.** Any online skill that would send data off-device requires explicit, per-skill user permission and is clearly disclosed ([[mcp-skill-system]]).
- **Data minimization.** Send the least data necessary, only when the user opts in.

## Current Reality
- The app captures camera frames in-memory for analysis and **closes each frame immediately** (`imageProxy.close()`); nothing is persisted or transmitted.
- No networking code exists, so no data currently leaves the device.
- Audio/foreground-service permissions are *declared* in the manifest but no audio capture or background recording is implemented.

## To preserve as we build
- Keep any future online skill's data flow opt-in and isolated from the core ([[ADR-0003-skill-system-boundaries]]).
- If any caching/persistence is added, document exactly what, where, for how long, and how to clear it — here.

## Related
- [[safety-principles]] · [[accessibility-principles]]
