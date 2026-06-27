---
status: planned
confidence: low
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Web Search Skill (Online)

> An **online**, optional skill for answering general questions. Part of the skill layer ([[mcp-skill-system]]), outside the safety core.

## Current Reality
Not implemented. No networking or search integration exists.

## Future Vision
- Voice-initiated web search with spoken, summarized results ([[voice-command-layer]]).
- Clearly an online enhancement; unavailable in airplane mode — which is fine because it is never part of the core.

## Boundaries
- **Online** and **optional**, permissioned, with disclosed data flow ([[privacy-model]]).
- Isolated from the offline safety core ([[ADR-0003-skill-system-boundaries]]).

## Related
- [[email-reading-writing-skill]] · [[alarms-reminders-skill]]
