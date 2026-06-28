# Braille / Native Accessibility (Agent 2)

Canonical, hands-on detail lives in [`docs/ACCESSIBILITY_VALIDATION.md`](../ACCESSIBILITY_VALIDATION.md)
and [`docs/accessibility/braille-support.md`](../accessibility/braille-support.md). This file is the
implementation summary against the Agent 2 gate.

## Design rule honored
No fake custom braille UI. OBSERVA exposes correct Android accessibility semantics so the
Samsung/Android native braille stack (TalkBack → connected refreshable display) presents the app.

## How it meets the gate
- **Operable without visual buttons:** every primary action is reachable via (a) offline **voice
  commands**, (b) physical **volume-rocker hotkeys** (`HotkeySequencer`, off by default), and
  (c) TalkBack-focusable labeled controls. Voice + hotkeys are fully non-visual.
- **Short, stable, braille-friendly status:** `BrailleStatusPresenter` emits concise lines
  ("Observa active", hazard line, mode). `AccessibilityOutputRouter` is the single fan-out point.
- **No frame spam:** non-urgent duplicates are debounced (`OutputThrottler`, 1.2 s); **HAZARD events
  bypass the debounce and barge-in** (flush the TTS queue). Live regions: polite for ambient status,
  assertive for the hazard banner.
- **Repeat / current status** are first-class (router `repeatLast`, `announceBrailleStatus`).

## Known gaps (honest)
- No Compose `CustomAccessibilityAction` / `stateDescription` yet — actions are surfaced through
  labeled focusable controls + voice + hotkeys instead. Adding semantic custom actions is a clean
  future enhancement (a third, redundant access path).
- A physical refreshable braille display has not been hardware-tested; the app-level live region that
  feeds it is implemented and verified via TalkBack.

## Manual validation checklist
TalkBack on → swipe-navigate the dashboard (all controls announce) → connect a braille display in
Android Settings → operate by voice/hotkeys with the screen off → trigger OCR → repeat last alert →
start/stop observing → confirm Airplane Mode unaffected. (Full matrix in `ACCESSIBILITY_VALIDATION.md`.)
