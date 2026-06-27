---
status: decision
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# ADR-0002: Two-Tier Inference Architecture

**Status:** Accepted
**Date:** 2026-06-27

## Context
OBSERVA needs *continuous* awareness (cheap, constant) and *deep* understanding on request (expensive, occasional). Running heavy models continuously would drain battery and overheat the device; running only on-demand would lose the always-on awareness that makes OBSERVA different ([[vision]]).

## Decision
Split inference into two tiers:
- **Tier 1 — always-on:** lightweight, battery-efficient, continuous scene understanding ([[ambient-awareness]]).
- **Tier 2 — on-demand:** heavier OCR/VLM/document reasoning ([[ocr-mode]], [[conversational-vision]]).

**Tier 2 must never block Tier 1.** See [[two-tier-inference]].

## Consequences
- Positive: continuous awareness within a power budget; depth available without paying for it constantly.
- Negative/costs: added complexity (tier isolation, scheduling, resource budgeting); requires careful threading so Tier 2 can't starve Tier 1 ([[performance-targets]]).
- Follow-up: define execution isolation and frame-sampling strategy.

## Alternatives considered
- Single always-on heavy model — rejected: battery/thermal/latency.
- Single on-demand model only — rejected: loses ambient awareness, the core differentiator.

## Related
- [[ADR-0001-offline-first]] · [[ADR-0003-skill-system-boundaries]]
