---
status: planned
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Core Use Cases

Primary user journeys, in priority order. Each maps to features and the [[two-tier-inference]] model.

## 1. Ambient hazard & object awareness (Tier 1)
"Tell me what's around me without me having to point at it."
- Continuous, low-power scene understanding announces salient changes (person approaching, obstacle, step, doorway).
- Feature: [[ambient-awareness]]. Must work in airplane mode.

## 2. Orientation / aiming help (Tier 1 → Tier 2)
"Help me face the thing."
- After ambient awareness flags something, OBSERVA guides the user to orient toward it ("text to your left").
- Feature: [[spatial-guidance]].

## 3. Read text on demand (Tier 2)
"Read this label / sign / document."
- Heavier OCR pass, optionally translated. See [[ocr-mode]].

## 4. Ask about the scene (Tier 2)
"What's in front of me? What color is this? Is the stove on?"
- On-demand VLM reasoning. See [[conversational-vision]].

## 5. Voice control across the app
"Do everything hands-free."
- See [[voice-command-layer]].

## 6. Everyday assistant skills (online, optional)
Alarms/reminders, email, web search — permissioned, never part of the safety core. See [[mcp-skill-system]].

## Current Reality
None of these journeys are user-complete. The camera/analyzer loop that use case 1 will build on exists ([[ambient-awareness]]).
