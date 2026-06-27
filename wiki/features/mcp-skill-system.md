---
status: research
confidence: low
last_updated: 2026-06-27
owner: jose2505207-eng
---

# MCP Skill System

> The framework that lets OBSERVA expand into an assistive AI layer through **permissioned, isolated skills** — without ever compromising the offline safety core. See [[ai-os-expansion]] and [[ADR-0003-skill-system-boundaries]].

## Current Reality
Not implemented. There is no skill framework, no MCP integration, no skill registry in the code.

## Future Vision
- A registry of **skills** (alarms, email, web search, future capabilities) the user can enable individually.
- Each skill declares its permissions and whether it requires network access.
- A routing layer (driven by [[voice-command-layer]]) dispatches user intents to the right skill.
- Skills run **outside** the safety core's trust/execution boundary.

## Hard boundaries (non-negotiable)
- The offline safety core ([[offline-first-design]]) must function fully with **zero** skills enabled.
- A skill failure or a missing network must never degrade Tier 1 awareness ([[two-tier-inference]], [[safety-principles]]).
- Online skills are optional enhancements, explicitly permissioned, with disclosed data flow ([[privacy-model]]).

## Open questions
- Whether to literally use the Model Context Protocol or an MCP-inspired internal interface on-device.
- Sandboxing/permission model for skills on Android.

## Related
- [[alarms-reminders-skill]] · [[email-reading-writing-skill]] · [[web-search-online-skill]]
