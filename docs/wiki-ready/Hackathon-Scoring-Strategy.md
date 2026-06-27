# Hackathon Scoring Strategy

How each scoring axis maps to concrete implementation and demo moments. Prioritize P0 requirements ([[Product Requirements]]) that move these needles.

## Winning framing
> **OBSERVA is not a camera description app. It is an ambient mobility layer: fast enough to warn, private enough to trust, and simple enough to use without sight.**

## 1. Technical Implementation
- Show FPS, latency, and backend (REQ-006, REQ-007).
- Show the pipeline ([[Technical Architecture]]).
- Explain the Snapdragon / QNN / ExecuTorch path **truthfully** — claim NPU only if verified.
- Demonstrate adaptive frame processing if implemented (`STRATEGY_KEEP_ONLY_LATEST` already drops stale frames).

## 2. Use Case and Innovation
- Explain why blind users cannot be expected to aim perfectly.
- Show proactive alerts (not reactive "describe this").
- Show directional feedback (REQ-004).
- Show suppression of repeated spam (REQ-005).
- Emphasize user-first design.

## 3. Local Processing and Privacy
- Airplane Mode demo (REQ-001, Gate E).
- No camera upload; local inference only.
- Privacy status visible in UI (REQ-008).
- No cloud dependency for the core path.

## 4. Deployment and Accessibility
- One-command build (`./gradlew assembleDebug`, REQ-015).
- Simple install (Gate B).
- TalkBack-friendly controls, large text, high contrast (REQ-009).
- Voice/speech-first flow (REQ-012).

## 5. Presentation and Documentation
- Architecture diagram + this wiki.
- Demo script ([[Demo Plan]]).
- README + benchmarks (REQ-014).
- Known limitations documented honestly ([[Validation Gates]] Gate H).

## Priority call
If time is short, protect the P0 spine: **offline loop → one good directional, non-spammy alert → visible dashboard → honest backend label → airplane-mode demo.** Everything else (OCR, voice, full haptics) is upside, not the core story.
