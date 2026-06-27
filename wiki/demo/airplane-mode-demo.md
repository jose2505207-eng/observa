---
status: planned
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Airplane-Mode Demo

> The flagship demo: OBSERVA's core works with the network off. This is the proof of [[offline-first-design]] and the whole [[vision]].

## The point
Put the phone in **airplane mode**, then show continuous on-device awareness working anyway. No cloud, no excuses. This single act communicates offline-first, privacy-first, and low-latency at once.

## Target flow (Future Vision)
1. Enable airplane mode on stage.
2. Open OBSERVA; camera analyzer is live.
3. [[ambient-awareness]] announces what's around (obstacle/person/text present).
4. [[spatial-guidance]] orients the user toward a target.
5. On demand, [[ocr-mode]] reads a sign/label aloud — still offline.
6. (Optional) [[conversational-vision]] answers a question about the scene.

## Current Reality
What can be shown **today**: airplane mode on, app open, camera preview live, and the on-screen frame counter incrementing — proving an always-on, fully-offline analysis loop. No model output yet. Be honest about this on stage.

## Related
- [[judge-script]] · [[demo-checklist]] · [[failure-recovery]]
