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
What can be shown **today**, all in airplane mode (device-verified): camera preview live + frame/FPS counter (always-on offline loop); **Start Demo** scripted hazards spoken with directional audio + haptics + Braille/live-region status; **Read Text** offline OCR reading a sign aloud; **voice commands** (push-to-talk: "describe scene", "what is ahead", "read text", "braille status", etc.); Observing/Mute/Braille/Audio/Haptics toggles. Be honest on stage about two things: the live scene detector runs a **brightness heuristic** (no ML model bundled yet — the real ExecuTorch/YOLO path is implemented but the `.pte` isn't shipped), and **QNN is not active**. The dashboard states both truthfully.

## Related
- [[judge-script]] · [[demo-checklist]] · [[failure-recovery]]
