# Physical-button hotkeys

Hands-free volume-rocker shortcuts so a blind user can drive OBSERVA without the screen.

## Layer 1 — foreground shortcuts (shipped)
Active **only** when "Button shortcuts" is enabled (off by default, so the volume rocker is never
hijacked otherwise). Implemented in `MainActivity.onKeyDown` (ACTION_DOWN only, debounced, resolved
in a 1200 ms rolling window by the pure `HotkeySequencer`). Every command is confirmed by speech +
Braille/TalkBack status.

| Sequence | Command |
|---|---|
| Volume Up ×1 | Repeat last message |
| Volume Up ×2 | Current status |
| Volume Up ×3 | Read text (OCR) |
| Volume Up ×4 | Find exit (honest attempt) |
| Volume Down ×1 | Mute / unmute speech |
| Volume Down ×2 | Stop navigation |
| Volume Down ×3 | Emergency pause non-hazard output (hazards still fire) |

Long-press and mixed Up+Down chords are intentionally omitted for reliability against system volume
behavior on Samsung One UI.

## Find exit (honest)
`findExit()` runs **one real OCR pass**. If the camera actually reads "exit", it says "Exit text seen
ahead. Scan slowly toward it." Otherwise: "Exit not found. Try scanning slowly or use navigation."
It never invents an exit. Door/exit-sign *detection* needs a real model; a mapped-exit path needs a
map pack — both are documented future work.

## Layer 2 — global Accessibility Shortcut Service (deferred, documented)
A global hardware-key shortcut (working when OBSERVA is backgrounded) would require an
`AccessibilityService` with key-event filtering. **Not implemented** this pass because:
- It is disproportionate/privileged for the benefit, and must coexist with TalkBack.
- On Samsung S25 Ultra, reliable global volume-key capture by a non-system service is **not
  guaranteed** (system volume handling can take precedence).

If pursued later it must be: opt-in, off by default, no inspection of other apps' content, no
unrelated accessibility actions, clearly explained privacy, and verified on hardware. Until then,
Layer 1 (foreground) is the supported path.

## Tests
`HotkeySequencerTest`: mapping, windowed counting, debounce, button-reset, expiry, flush-clears.

## Validation status
Sequencer logic unit-tested; foreground key handling builds and runs on device. Tactile/real-press
verification (multi-press timing feel) is part of the human sensory pass
(`docs/manual-test-sensory-accessibility.md`).
