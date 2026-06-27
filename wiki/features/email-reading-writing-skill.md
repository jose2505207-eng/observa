---
status: planned
confidence: low
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Email Reading & Writing Skill

> An **online**, optional everyday-assistant skill. Part of the skill layer ([[mcp-skill-system]]), strictly outside the safety core.

## Current Reality
Not implemented. No email, accounts, or networking code exists.

## Future Vision
- Read incoming email aloud and compose/send replies by voice ([[voice-command-layer]]).
- Accessible summarization and dictation tuned for non-visual use.

## Boundaries
- **Online** and **optional.** Requires explicit user permission and discloses data flow ([[privacy-model]]).
- Must be fully isolated from the offline safety core; disabling it changes nothing about core awareness ([[ADR-0003-skill-system-boundaries]]).

## Open questions
- Provider/auth model; how much processing stays on-device.

## Related
- [[web-search-online-skill]] · [[alarms-reminders-skill]]
