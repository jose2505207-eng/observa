# UX Input Map (Agent 4)

Brutally-simple, eyes-free control. Full gesture detail: [`docs/HOTKEYS.md`](../HOTKEYS.md) and
[`docs/HAPTIC_LANGUAGE.md`](../HAPTIC_LANGUAGE.md).

## Modes
Always-on Awareness · OCR (on demand) · Pause/Silence · Repeat Last · Navigation (guidance-first) ·
Translation (on-demand shell, no model yet). Scene-question/VLM: not bundled.

## Visible mode buttons (sighted + low-vision, also TalkBack-labeled)

The main screen now shows a high-contrast button hub (no icon-only controls; every button has
`role=Button`, a `contentDescription`, and a `stateDescription` where relevant, 72 dp targets):

| Button | Action |
|---|---|
| Awareness | start/stop the detector (`observe`) |
| Navigate | start/stop Navigation Mode (compass + GPS bearing guidance) |
| Translate | start Translation Mode (honest offline readiness) |
| Voice Commands | `openVoiceCommands()` (hold-to-talk) |
| Read Signs | one offline OCR pass; speaks sign text or "no readable sign text" |
| Repeat Alert | repeat the last spoken alert |

Below them: a **Navigation** card (live guidance + map-pack status + Start/Stop/Repeat), a
**Translation** card (readiness + Start/Stop/Repeat), and a collapsible **Debug Status** card.

## Two-layer, blind-first input model

OBSERVA uses **two layers** so that the guaranteed path never depends on a gesture the OS might
intercept:

- **Layer A — native accessibility actions (TalkBack/braille users):** the primary, guaranteed path.
- **Layer B — raw screen gestures (only when TalkBack is OFF):** a convenience for sighted/low-vision
  users not running a screen reader. When TalkBack/touch-exploration is on, one-finger gestures are
  consumed by the screen reader, so OBSERVA **does not wire raw gestures** and routes the user to
  Layer A instead.

The pure mapping lives in `input/BlindGestureController.kt` (unit-tested); the Compose surface only
detects raw events and asks the controller what to do. The live state is tracked via
`AccessibilityManager.isTouchExplorationEnabled` (`TrackTalkBackState`), and the controller exposes
`rawGesturesActive` / `gestureStatusLine`.

### Layer A — native accessibility actions (the guaranteed non-visual path)

The top of the screen is a **native operating layer**: three stable, focusable nodes (Current status,
Last alert, Available actions). The Available-actions node carries Compose
`CustomAccessibilityAction`s, surfaced in TalkBack's **Actions menu** (and to connected braille
displays). These do not depend on any custom gesture:

| Action (TalkBack actions menu) | Effect |
|---|---|
| Start awareness / Pause awareness | `observe(true/false)` |
| **Open voice commands** | `openVoiceCommands()` (hold-to-talk) |
| Repeat last alert | `repeatLast()` |
| Start OCR, read text | `readText()` |
| Start scene question | `sceneQuestion()` (honest brightness summary) |
| Start translation mode | honest readiness gate |
| Start / Repeat / Stop orientation | GPS Orientation Lite |
| Silence alerts | `silenceAlerts()` (hazards still fire) |
| Open debug status | speaks detector backend + diagnostics |

State is exposed via `stateDescription` (toggles "on"/"off") and semantic state lines
("Awareness active" / "Detector backend: XNNPACK"), so TalkBack and braille announce changes
correctly. The three nodes are **not** live regions (read on demand, never flood braille). When
TalkBack is on, the gesture-hint line reads **"Gestures available through TalkBack actions."**

### Layer B — raw screen gestures (TalkBack OFF only)

Detected on the main camera surface; resolved by `BlindGestureController`. Status line (TalkBack off):
**"Triple tap for voice commands. Swipe up translation. Swipe down navigation. Double tap repeats."**

| Gesture | Action |
|---|---|
| Triple tap | Open voice commands (hold-to-talk) |
| Double tap | Repeat last alert |
| Swipe up | Start translation mode (honest readiness) |
| Swipe down | Start navigation / GPS Orientation Lite |
| Long press | Push-to-talk |

All actions are idempotent and emit at non-hazard priority, so a **vision hazard always interrupts**
(the output router enforces `HAZARD > NAVIGATION > OCR > MODE > INFO`).

## Physical inputs

### Volume rocker (`HotkeySequencer`, foreground; OFF by default so volume isn't hijacked)
Mission cap honored: **no more than three clicks per button.**

| Presses | Volume **Up** | Volume **Down** |
|---|---|---|
| ×1 | Repeat last alert | Mute toggle |
| ×2 | Current status | Stop navigation |
| ×3 | **Open voice commands** | Emergency pause (hazards still fire) |

**Tap volume‑up three times → voice commands** (requires "Button shortcuts" enabled). Then speak any
command below.

(A 4th press is intentionally unmapped → `NONE`. Find-exit moved to voice / front-tap.)

### Taps (system-level — configure, not app-intercepted)
The desired back/front tap gestures are **Samsung/Android system-level** (TalkBack consumes taps when
on). They are mapped via TalkBack → Customize gestures / Universal Switch / side-key, **not** by the
app. The app never *depends* on a tap it can't reliably receive — the guaranteed non-visual paths are
**TalkBack custom actions**, **voice**, and **volume hotkeys**. Full honesty table:
[`docs/demo/ACCESSIBILITY_DEMO.md`](../demo/ACCESSIBILITY_DEMO.md).

### Voice (offline, hold-to-talk) — every feature is callable by voice
Whole-app control: observing on/off, describe scene, what is ahead, read text, **read signs**, find
exit, braille on/off/status, **start/stop navigation**, navigate to ‹place›, where am I, **start/stop
translation** ("translate"), **download map**, **download ‹language›** (e.g. "download French"), repeat,
mute/unmute, help. Opened by holding the mic button, the "Open voice commands" TalkBack action, or
**volume‑up ×3**.

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
