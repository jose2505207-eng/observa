# Accessibility validation

OBSERVA is built for non-visual use. This summarizes accessibility status and points to the
hands-on checklists. Automated/JVM tests cover the pure logic; sensory/hardware items need a human.

## Implemented (device-verified unless noted)
- **TTS** is the primary output channel; hazards barge-in (flush queue).
- **Directional audio cues** (stereo-panned) + **directional haptics** (left/right/forward/urgent/
  confirm/error); cues continue when speech is muted (safety).
- **Braille/TalkBack**: concise composite status in a **polite live region**; assertive alert banner;
  `contentDescription` + `testTag` on all major controls; on-screen toggles for Observing, Braille,
  Audio cues, Haptics; accessible Navigation panel and Read Text button.
- **Voice-first** control of the whole app (offline recognizer).
- **High-contrast** dark UI, large touch targets, content reachable past the gesture nav bar
  (scrollable, safe insets).

## Validation matrix
| Capability | Status |
|---|---|
| Camera preview | PASS (device) |
| Heuristic detection → output | PASS (device) |
| OCR on demand | PASS (device, offline; read real text) |
| Audio cues | mechanism PASS (device); audibility = human pass |
| Haptics | mechanism PASS (device, vibrator HAL fired); feel = human pass |
| Voice commands | "On-device ready" (device); recognition accuracy = human pass |
| TalkBack labels/live regions | implemented; screen-reader read-through = human pass |
| Physical Braille display | **not verified** (no hardware) |
| Foreground service + notification | PASS (device) |
| Navigation guidance | PASS (device, demo location + real compass) |
| Airplane mode / no INTERNET | PASS (device) |

## Hands-on checklists (run with a user where possible)
- `docs/manual-test-sensory-accessibility.md` — TalkBack, Braille display, muted/haptics-only, OCR,
  repeat, hazard interruption.
- `docs/manual-test-foreground-service.md` — notification actions, degraded modes.
- `docs/manual-test-offline-navigation.md` — destination selection, guidance, uncertainty.
- `docs/manual-test-real-detector.md` — once a model is bundled.

## Honest gaps
Physical Braille display and a full human sensory pass (audio/haptic/voice in real conditions) are
not yet performed. See `docs/KNOWN_LIMITATIONS.md`.
