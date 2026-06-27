---
status: current
confidence: high
last_updated: 2026-06-28
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
- **No `INTERNET` permission.** It (and `ACCESS_NETWORK_STATE`, pulled in by ML Kit) is stripped from the merged manifest via `tools:node="remove"`, so the app **physically cannot open a network socket** — verified with `aapt2 dump permissions` and on device (`dumpsys package`). This is the strongest possible offline guarantee.
- Camera frames are processed in-memory and **closed immediately** (`imageProxy.close()`); nothing is persisted or transmitted. The OCR one-shot bitmap is recycled after recognition.
- All intelligence runs on device: heuristic vision, the (bundled, when present) ExecuTorch detector, ML Kit's **bundled** OCR model, and the platform on-device speech recognizer. Device-verified functioning in Airplane Mode.
- `RECORD_AUDIO` is used only for on-demand push-to-talk voice commands; no background/continuous recording.

## To preserve as we build
- Keep any future online skill's data flow opt-in and isolated from the core ([[ADR-0003-skill-system-boundaries]]).
- If any caching/persistence is added, document exactly what, where, for how long, and how to clear it — here.

## Related
- [[safety-principles]] · [[accessibility-principles]]
