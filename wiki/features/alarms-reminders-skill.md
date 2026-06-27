---
status: planned
confidence: low
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Alarms & Reminders Skill

> A local, offline-capable everyday-assistant skill. Part of the optional skill layer ([[mcp-skill-system]]), not the safety core.

## Current Reality
Not implemented. `POST_NOTIFICATIONS` and `WAKE_LOCK` are declared in the manifest; no alarm/reminder logic exists.

## Future Vision
- Set, list, and cancel alarms and reminders by voice ([[voice-command-layer]]).
- Local notifications/alarms; works **offline** (no cloud dependency).
- Accessible confirmations spoken back to the user.

## Boundaries
- A skill, invoked through the permissioned [[mcp-skill-system]]; it must not be part of or required by the offline safety core ([[ADR-0003-skill-system-boundaries]]).

## Related
- [[email-reading-writing-skill]] · [[web-search-online-skill]]
