---
status: decision
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# ADR-0003: Skill System Boundaries

**Status:** Accepted
**Date:** 2026-06-27

## Context
OBSERVA's long-term vision is an assistive AI layer with everyday-assistant capabilities — alarms, email, web search, and more ([[ai-os-expansion]]). Some are online. Without a firm boundary, these would creep into the safety core and undermine offline-first ([[ADR-0001-offline-first]]) and privacy ([[privacy-model]]).

## Decision
All expansion happens through **permissioned, isolated skills** ([[mcp-skill-system]]) governed by hard boundaries:
- The offline safety core must function fully with **zero** skills enabled.
- A skill failure or absent network must never degrade Tier 1 awareness ([[two-tier-inference]]).
- Online skills are optional, explicitly permissioned, with disclosed data flow.
- Skills run outside the safety core's trust/execution boundary.

## Consequences
- Positive: safety core stays small, offline, auditable; capabilities grow safely at the edges.
- Negative/costs: requires a real isolation/permission mechanism on Android; more upfront design ([[mcp-skill-system]] is currently **Research**).
- Follow-up: decide MCP-proper vs MCP-inspired internal interface; define sandboxing.

## Alternatives considered
- Build features directly into the app — rejected: erodes the offline/privacy core.
- Cloud-orchestrated assistant — rejected: violates [[ADR-0001-offline-first]].

## Related
- [[alarms-reminders-skill]] · [[email-reading-writing-skill]] · [[web-search-online-skill]]
