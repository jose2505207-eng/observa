# OBSERVA haptic language

A small, learnable vocabulary of vibration patterns. Original to OBSERVA (not a copy of any
proprietary UI), inspired by the idea of a directional "lock-on". All offline.

## Safety (always on unless haptics fully OFF; overrides navigation)
| Cue | Pattern | Meaning |
|---|---|---|
| Stop / hazard | strong escalating burst | hazard ahead — stop |
| Left | two short pulses (left-coded) | hazard/target to the left |
| Right | two longer pulses (right-coded) | hazard/target to the right |
| Forward | single centered tick | hazard/target ahead |

## Navigation lock-on (NAVIGATION_LOCK_ON / FULL modes)
Bearing error = route bearing − device heading (normalized −180..180). As you rotate toward the
target, ticks get **faster and stronger**; at alignment a distinct double-click fires.

| Bearing error | State | Cadence | Amplitude |
|---|---|---|---|
| > 60° | searching | no lock ticks (occasional spoken cue) | — |
| 30–60° | off-course | tick / 900 ms | low |
| 15–30° | approaching | tick / 500 ms | medium |
| 5–15° | near | tick / 250 ms | high |
| 0–5° | aligned | lock-on double-click / 700 ms | full |
| arrived | arrived | distinct arrival pattern | full |

Spoken companions: "Turn left toward 10 o'clock", "Almost aligned", "Aligned. Continue forward",
"Compass accuracy poor. Use caution."

## Honesty / accuracy
- **Poor/unavailable compass** → only a coarse "searching" cue + a spoken "Compass accuracy poor"
  warning. We never imply lock precision the sensor can't support.
- **Low accuracy** widens the cadence and caps amplitude.
- **Hazard always preempts** navigation haptics (the controller suppresses lock ticks for ~2 s after
  a hazard, and `DirectionalLockHaptics.cue(hazard=true)` returns the stop pattern).
- Amplitude uses hardware amplitude control when available (`Vibrator.hasAmplitudeControl()`), else
  falls back to on/off pulses.

## Modes (`HapticGuidanceMode`)
- **OFF** — no haptics.
- **SAFETY_ONLY** — hazard/safety cues only.
- **NAVIGATION_LOCK_ON** (default) — safety + navigation lock-on.
- **FULL** — everything.

## Implementation
- Pure mapping: `nav/DirectionalLockHaptics` (unit-tested in `DirectionalLockHapticsTest`).
- Playback: `cue/HapticCuePlayer` (`lockTick`, `lockOn`, `arrived`) via `cue/SpatialCueEngine`.
- Driven by `ObservaController.navigationTick()` using the real device compass
  (`nav/SensorNavFixProvider`).

## Validation status
Logic unit-tested; on-device tactile distinguishability (feel of each cadence/pattern) requires a
human pass — see `docs/manual-test-sensory-accessibility.md`.
