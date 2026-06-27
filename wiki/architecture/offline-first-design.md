---
status: planned
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Offline-First Design

> The safety-critical core of OBSERVA must work with no network — including airplane mode. This is mandatory; see [[ADR-0001-offline-first]].

## What "offline-first" guarantees
- Tier 1 ambient awareness and orientation guidance run **entirely on-device** ([[two-tier-inference]], [[ambient-awareness]]).
- No camera imagery leaves the device for any core function ([[privacy-model]]).
- The app is fully useful in airplane mode with zero skills enabled.

## Design rules
- Core models, weights, and assets ship in (or are provisioned to) the app — no network fetch at inference time.
- Online **skills** are optional add-ons, isolated from the core ([[mcp-skill-system]], [[ADR-0003-skill-system-boundaries]]). Disabling them must not degrade the safety core.
- No core code path may block on a network call.

## Current Reality
The current app makes **no network calls at all** (no networking code, no internet usage in the core loop). The camera/analyzer loop runs locally. This satisfies offline-first by construction today — but only because inference doesn't exist yet. The guarantee must be preserved as models and features are added.

## Demo implication
The flagship demo runs in airplane mode. See [[airplane-mode-demo]].

## Related
- [[safety-principles]] · [[performance-targets]]
