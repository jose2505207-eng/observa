# UX Input Map (Agent 4)

Brutally-simple, eyes-free control. Full gesture detail: [`docs/HOTKEYS.md`](../HOTKEYS.md) and
[`docs/HAPTIC_LANGUAGE.md`](../HAPTIC_LANGUAGE.md).

## Modes
Always-on Awareness · OCR (on demand) · Pause/Silence · Repeat Last · Navigation (guidance-first) ·
Translation (on-demand shell, no model yet). Scene-question/VLM: not bundled.

## Physical inputs

### Volume rocker (`HotkeySequencer`, foreground; OFF by default so volume isn't hijacked)
Mission cap honored: **no more than three clicks per button.**

| Presses | Volume **Up** | Volume **Down** |
|---|---|---|
| ×1 | Repeat last alert | Mute toggle |
| ×2 | Current status | Stop navigation |
| ×3 | Read text (OCR) | Emergency pause (hazards still fire) |

(A 4th press is intentionally unmapped → `NONE`. Find-exit moved to voice / front-tap.)

### Taps (where the device exposes them)
- Double-tap back: repeat last / current status.
- Single-tap front: confirm suggested action · double-tap front: start OCR · hold front: hold-to-talk.
- Validated against availability — the app never *depends* on a tap it can't reliably receive;
  voice + volume hotkeys are the guaranteed paths.

### Voice (offline, hold-to-talk)
Whole-app control: observing on/off, describe scene, what is ahead, read text, find exit, braille
on/off/status, navigate to, stop navigation, where am I, repeat, mute/unmute, help.

## Audio (spatial)
Stereo-panned cues — left object → left ear, right → right, central danger → centered urgent tone.
Distinct tones per category (obstacle / person / vehicle / text). Spoken alerts kept short; not
continuous. (`SpatialCueEngine`, `AudioCuePlayer`.)

## Haptics
Left/right directional pulses, centered forward-hazard pulse, strong repeating pulse for immediate
danger, gentle pulse for non-danger awareness; progressive lock-on ticks for navigation alignment.
Cues fire even when speech is muted (safety). (`HapticCuePlayer`, `DirectionalLockHaptics`.)

## Translation mode (honest)
On-demand only; must not degrade always-on hazard detection. **No on-device translation model is
bundled yet** — the mode is a shell and says so. Never cloud.
