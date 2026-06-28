# UX Input Map (Agent 4)

Brutally-simple, eyes-free control. Full gesture detail: [`docs/HOTKEYS.md`](../HOTKEYS.md) and
[`docs/HAPTIC_LANGUAGE.md`](../HAPTIC_LANGUAGE.md).

## Modes
Always-on Awareness · OCR (on demand) · Pause/Silence · Repeat Last · Navigation (guidance-first) ·
Translation (on-demand shell, no model yet). Scene-question/VLM: not bundled.

## Native accessibility actions (v2.2.0 — the guaranteed non-visual path)

The top of the screen is a **native operating layer**: three stable, focusable nodes (Current status,
Last alert, Available actions). The Available-actions node carries Compose
`CustomAccessibilityAction`s, surfaced in TalkBack's **Actions menu** (and to connected braille
displays). These do not depend on any custom gesture:

| Action (TalkBack actions menu) | Effect |
|---|---|
| Start awareness / Pause awareness | `observe(true/false)` |
| Repeat last alert | `repeatLast()` |
| Start OCR, read text | `readText()` |
| Start scene question | `sceneQuestion()` (honest brightness summary) |
| Start translation mode | honest "not installed" |
| Silence alerts | `silenceAlerts()` (hazards still fire) |
| Open debug status | speaks detector backend + diagnostics |

State is exposed via `stateDescription` (toggles "on"/"off") and semantic state lines
("Awareness active" / "Detector backend: XNNPACK"), so TalkBack and braille announce changes
correctly. The three nodes are **not** live regions (read on demand, never flood braille).

## Physical inputs

### Volume rocker (`HotkeySequencer`, foreground; OFF by default so volume isn't hijacked)
Mission cap honored: **no more than three clicks per button.**

| Presses | Volume **Up** | Volume **Down** |
|---|---|---|
| ×1 | Repeat last alert | Mute toggle |
| ×2 | Current status | Stop navigation |
| ×3 | Read text (OCR) | Emergency pause (hazards still fire) |

(A 4th press is intentionally unmapped → `NONE`. Find-exit moved to voice / front-tap.)

### Taps (system-level — configure, not app-intercepted)
The desired back/front tap gestures are **Samsung/Android system-level** (TalkBack consumes taps when
on). They are mapped via TalkBack → Customize gestures / Universal Switch / side-key, **not** by the
app. The app never *depends* on a tap it can't reliably receive — the guaranteed non-visual paths are
**TalkBack custom actions**, **voice**, and **volume hotkeys**. Full honesty table:
[`docs/demo/ACCESSIBILITY_DEMO.md`](../demo/ACCESSIBILITY_DEMO.md).

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
