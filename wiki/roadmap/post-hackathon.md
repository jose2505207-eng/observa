---
status: planned
confidence: low
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Post-Hackathon

> Near-term direction once the [[hackathon-mvp]] thesis is proven. Order is indicative, not committed.

## Themes
1. **Harden Tier 1.** Better models, frame sampling, real [[performance-targets]] from [[profiling-plan]]; always-on foreground service.
2. **Spatial guidance.** Directional cues from detections ([[spatial-guidance]]).
3. **Tier 2 OCR + translation.** On-demand reading with offline translation ([[ocr-mode]]).
4. **Voice layer.** On-device ASR/TTS and intent routing ([[voice-command-layer]]).
5. **Conversational vision.** Evaluate on-device VLM feasibility ([[conversational-vision]]).
6. **Skill framework.** Stand up the isolated, permissioned [[mcp-skill-system]] and a first local skill ([[alarms-reminders-skill]]).

## Constraints carried forward
- Offline safety core stays isolated ([[ADR-0001-offline-first]], [[ADR-0003-skill-system-boundaries]]).
- Accessibility and safety gates on every feature ([[accessibility-principles]], [[safety-principles]]).

## Related
- [[long-term-ai-os]]
