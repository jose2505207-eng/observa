---
status: planned
confidence: high
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Accessibility Principles

Non-negotiable rules for a non-visual-first product. These constrain every feature.

## 1. Non-visual is the default, not an add-on
- The app must be fully operable with TalkBack and without looking at the screen.
- Every interactive element has a meaningful `contentDescription`.

## 2. Audio-first output
- Primary output is speech and earcons/haptics, not on-screen text.
- Spoken output is concise, prioritized (most safety-relevant first), and interruptible.

## 3. Low cognitive and motor load
- Large touch targets; few, forgiving gestures; voice as a first-class input ([[voice-command-layer]]).
- Avoid modal dialogs that trap a screen-reader user.

## 4. Latency is an accessibility feature
- Slow feedback is unusable feedback for navigation. See [[performance-targets]].

## 5. Predictability and trust
- Consistent phrasing for the same situation; never silently change behavior.
- Distinguish certainty levels in speech ("looks like" vs "there is").

## 6. Respect the user's other tools
- Coexist with TalkBack, cane, guide dog, and existing navigation apps — don't fight them.

## Current Reality
Largely realized in the output layer (device-verified): TTS speech (primary channel), stereo earcons, directional haptics, and a Braille/TalkBack polite live region — all via one unified router with hazard-priority interruption. High-contrast UI, `contentDescription`s, live regions, test tags, and an on-screen Observing/Braille/Audio/Haptics toggle set. Coexists with TalkBack. Still pending: physical Braille-display verification and a full human sensory pass.

## Related
- [[safety-principles]] · [[ambient-awareness]] · [[spatial-guidance]]
