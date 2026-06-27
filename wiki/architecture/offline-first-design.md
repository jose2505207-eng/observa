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
Offline-first is now **enforced**, not just incidental: the app declares **no `INTERNET` permission** (and `ACCESS_NETWORK_STATE` is stripped), so it cannot make network calls even though it now bundles real inference — ML Kit's on-device OCR model, the on-device speech recognizer, and the ExecuTorch detector path all run locally. Verified by `aapt2 dump permissions` and on device in Airplane Mode. The guarantee must be preserved as features are added: any future online skill must be opt-in and isolated from the core ([[privacy-model]], [[ADR-0003-skill-system-boundaries]]).

## Demo implication
The flagship demo runs in airplane mode. See [[airplane-mode-demo]].

## Related
- [[safety-principles]] · [[performance-targets]]
