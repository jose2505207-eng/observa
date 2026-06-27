---
status: planned
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Safety Principles

OBSERVA can influence how a blind user moves through the physical world. Getting safety wrong is worse than being silent.

## 1. The safety core is offline and self-contained
- Hazard awareness and orientation must work with no network. See [[offline-first-design]] and [[ADR-0001-offline-first]].
- The safety core must not depend on any online skill ([[mcp-skill-system]]).

## 2. Never be confidently wrong
- Communicate uncertainty. Prefer "possible step ahead" over a false-precision claim.
- When confidence is low, say less, not more.

## 3. Fail safe and fail loud
- If the model, camera, or pipeline fails, tell the user immediately and clearly — never go silently dark.
- Degrade gracefully: if Tier 2 is unavailable, Tier 1 must keep running ([[two-tier-inference]]).

## 4. Don't replace the primary mobility aid
- OBSERVA augments the cane/dog/skills the user already has; it is not a substitute. Frame guidance as supplementary.

## 5. Latency bounds are safety bounds
- Hazard-relevant feedback must meet the Tier 1 latency target or it is dangerous. See [[performance-targets]].

## 6. Privacy is part of safety
- Surveillance of a user's environment is a harm. Keep imagery on-device. See [[privacy-model]].

## Current Reality
No hazard detection exists yet; nothing in the app currently makes safety claims to a user. These principles gate all hazard/guidance features before they ship.

## Related
- [[accessibility-principles]] · [[risks-and-mitigations]]
