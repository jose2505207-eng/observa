---
status: planned
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Failure Recovery

> What to do if the live demo misbehaves. Stay calm; the story still lands.

## If the app crashes or freezes
- Relaunch (it's fast). Keep narrating the problem/vision while it reloads.
- Fall back to the pre-recorded video ([[demo-checklist]]).

## If the camera/preview is black
- Check permission was granted; check another app isn't holding the camera.
- Toggle the screen; rebind by reopening the app.

## If inference is slow/unresponsive (once it exists)
- Emphasize Tier 1 keeps running even if Tier 2 stalls ([[two-tier-inference]]) — that's by design, not a bug.

## If someone questions the offline claim
- Point to airplane mode being on and that there is no networking code in the core ([[offline-first-design]], [[privacy-model]]).

## Golden rule
Never claim something works that just failed. Acknowledge it, show the fallback, return to the vision ([[judge-script]]).
