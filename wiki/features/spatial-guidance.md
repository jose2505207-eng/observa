---
status: planned
confidence: medium
last_updated: 2026-06-28
owner: jose2505207-eng
---

# Spatial Guidance

> Turns "something is there" into "face it / move toward it." This is how OBSERVA reverses the aim-the-camera assumption ([[vision]]).

## Current Reality
The **directional cue output layer is implemented** (device-verified); the **active aiming/orientation loop is not** — hence this feature is still `planned`.
- Coarse direction (LEFT / CENTER / RIGHT) is derived for every detection from the normalized box center (`DirectionMapper`) and carried on hazard alerts.
- Cues are emitted through `SpatialCueEngine`: stereo-**panned** synthesized audio (`AudioCuePlayer`, in-memory, offline) + **directional haptics** (`HapticCuePlayer`: left/right/forward/urgent-stop/confirm/error). Speech says the direction ("on your left/right/ahead"); non-speech cues keep working when speech is muted (safety). Throttled via `CueThrottler`; toggles for audio cues and haptics.
- **Not built:** the closed-loop "orient toward a target" correction ("a little right… there"), and any precise localization beyond left/center/right.

## Offline navigation (v1.7, shipped)
Beyond per-detection direction, OBSERVA now has a **guidance-first offline navigation** layer:
clock-face, heading-relative instructions toward saved destinations using the real device compass
(location is a documented demo fix; no live GPS yet). Routes at NAVIGATION priority so hazards
override. See `docs/offline-navigation.md`. (`Geo`, `GuidanceEngine`, `DestinationStore`,
`NavigationSession`, `SensorNavFixProvider`.)

## Future Vision
- Live GPS provider + map packs / road graph for true turn-by-turn.
- Use detection position over time to guide the user to face/approach a target so a Tier 2 action ([[ocr-mode]], [[conversational-vision]]) can capture it well.
- Provide simple, calm correction rather than rapid chatter.

## Design notes
- Direction can start coarse (left / center / right / up / down) before any precise localization.
- Must respect [[safety-principles]]: never imply precision the model doesn't have.
- Latency-sensitive — this is part of the Tier 1 budget ([[performance-targets]]).

## Open questions
- Audio spatialization approach (stereo panning vs verbal only).
- How much guidance before it becomes noise.

## Related
- [[ambient-awareness]] · [[voice-command-layer]]
