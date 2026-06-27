---
status: research
confidence: low
last_updated: 2026-06-27
owner: jose2505207-eng
---

# AI OS Expansion

> Long-term vision: OBSERVA grows from a vision assistant into a permissioned **assistive AI operating layer** for blind and low-vision users.

## The idea
Once continuous on-device awareness and a voice layer exist, the same assistant can mediate other everyday tasks — but only through **explicitly permissioned skills**. The vision core stays the foundation; capabilities are added at the edges.

## Hard boundary
- The **offline safety core** ([[offline-first-design]], [[safety-principles]]) must remain **isolated** from online capabilities.
- Skills are optional enhancements, never requirements. The app must remain fully useful in airplane mode with zero skills enabled.
- See [[mcp-skill-system]] and [[ADR-0003-skill-system-boundaries]].

## Candidate skills (all Planned/Research)
- [[alarms-reminders-skill]] (local, offline-capable)
- [[email-reading-writing-skill]] (online)
- [[web-search-online-skill]] (online)
- Translation and navigation extensions building on the core.

## Current Reality
None of this exists. This page records direction only; nothing here should be treated as a commitment or a built capability.

## Related
- [[long-term-ai-os]] · [[vision]]
