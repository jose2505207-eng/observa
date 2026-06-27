---
status: decision
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# ADR-0001: Offline-First Safety Core

**Status:** Accepted
**Date:** 2026-06-27

## Context
OBSERVA serves blind and low-vision users in the physical world, often where connectivity is poor, unavailable, or undesirable for privacy. Cloud-dependent vision assistants add latency, fail without signal, and stream private imagery off-device. For a tool that influences safe mobility, none of that is acceptable for the core function.

## Decision
The **safety-critical core** (Tier 1 ambient awareness and orientation) runs **entirely on-device** and must work with no network, including airplane mode. Cloud services are **optional enhancements only** and may never be required by the core. See [[offline-first-design]], [[two-tier-inference]], [[safety-principles]].

## Consequences
- Positive: works anywhere; private by construction; lower latency; honest airplane-mode demo ([[airplane-mode-demo]]).
- Negative/costs: constrained to on-device models; harder performance/feasibility work ([[executorch-qnn]], [[model-selection]], [[performance-targets]]).
- Follow-up: all online features become skills isolated from the core ([[ADR-0003-skill-system-boundaries]]).

## Alternatives considered
- Cloud-first with offline fallback — rejected: violates privacy/latency/availability for a safety tool.
- Hybrid where core can call cloud — rejected: makes the safety core depend on connectivity.

## Related
- [[ADR-0002-two-tier-architecture]]
