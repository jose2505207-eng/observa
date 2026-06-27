---
status: planned
confidence: medium
last_updated: 2026-06-27
owner: jose2505207-eng
---

# Spatial Guidance

> Turns "something is there" into "face it / move toward it." This is how OBSERVA reverses the aim-the-camera assumption ([[vision]]).

## Current Reality
Not implemented. No spatial reasoning, no directional output exists.

## Future Vision
- After [[ambient-awareness]] flags a salient element, derive its rough direction/region in the frame.
- Guide the user with directional cues: speech ("text to your left," "doorway ahead") plus spatialized earcons/haptics.
- Help orient toward a target so a Tier 2 action ([[ocr-mode]], [[conversational-vision]]) can capture it well.
- Provide simple, calm correction ("a little right... there") rather than rapid chatter.

## Design notes
- Direction can start coarse (left / center / right / up / down) before any precise localization.
- Must respect [[safety-principles]]: never imply precision the model doesn't have.
- Latency-sensitive — this is part of the Tier 1 budget ([[performance-targets]]).

## Open questions
- Audio spatialization approach (stereo panning vs verbal only).
- How much guidance before it becomes noise.

## Related
- [[ambient-awareness]] · [[voice-command-layer]]
