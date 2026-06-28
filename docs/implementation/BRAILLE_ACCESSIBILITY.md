# Braille / Native Accessibility (Agent 2)

Canonical, hands-on detail lives in [`docs/ACCESSIBILITY_VALIDATION.md`](../ACCESSIBILITY_VALIDATION.md)
and [`docs/accessibility/braille-support.md`](../accessibility/braille-support.md). This file is the
implementation summary against the Agent 2 gate.

## Design rule honored
No fake custom braille UI. OBSERVA exposes correct Android accessibility semantics so the
Samsung/Android native braille stack (TalkBack → connected refreshable display) presents the app.

## How it meets the gate
- **Native operating layer (v2.2.0):** the top of the screen exposes three stable, focusable
  accessible nodes — **Current status**, **Last alert**, **Available actions** — derived by the pure
  `AccessibilityStatusReducer`. The Available-actions node carries Compose
  **`CustomAccessibilityAction`s** so a TalkBack user (or connected braille display) can run every
  core flow from the actions menu without any visual button:
  *Start awareness · Pause awareness · **Open voice commands** · Repeat last alert · Start OCR (read
  text) · Start scene question · Start translation mode · Start/Repeat/Stop orientation · Silence
  alerts · Open debug status.*
- **Two-layer input, blind-first:** these native actions are **Layer A — the guaranteed path** for
  TalkBack/braille users. Raw one-finger screen gestures (triple-tap voice, swipe-up translation,
  swipe-down navigation) are **Layer B**, wired **only when TalkBack is off** (when it's on, the
  screen reader owns one-finger gestures, so OBSERVA routes the user to Layer A and the hint line
  reads *"Gestures available through TalkBack actions."*). Mapping is the unit-tested pure
  `input/BlindGestureController`. See [`UX_INPUT_MAP.md`](UX_INPUT_MAP.md).
- **Semantic state, not baked-in strings:** toggles use `stateDescription` ("on"/"off"); the status
  node carries `awarenessState` ("Awareness active" / "Awareness paused") and
  `detectorState` ("Detector backend: XNNPACK"). TalkBack announces state changes correctly and a
  braille display shows compact state.
- **Operable without visual buttons (four redundant paths):** custom semantic actions, offline
  **voice commands**, physical **volume-rocker hotkeys** (`HotkeySequencer`, off by default), and
  TalkBack-focusable labeled controls. The first three are fully non-visual.
- **Short, stable, braille-friendly status:** `BrailleStatusPresenter` + the reducer emit concise
  lines. The three operating-layer nodes are **not** live regions — they update silently and are read
  on demand, so a refreshable braille display is not flooded.
- **No frame spam / debug never leaks:** non-urgent duplicates are debounced (`OutputThrottler`,
  1.2 s); **HAZARD events bypass the debounce and barge-in** (flush the TTS queue). Live regions:
  polite for ambient status, assertive for the hazard banner. `AccessibilityStatusReducer.stripDebug`
  guarantees confidence/bbox/latency never reach user output (that detail lives only in the
  "Open debug status" action and `OBSERVA_MODEL` logcat).
- **Repeat / current status** are first-class (router `repeatLast`, `announceBrailleStatus`, and the
  Current-status node).

## Honesty held
- **Translation** is a shell: the "Start translation mode" action says *"Translation not installed —
  no on-device model bundled, never cloud."* `translationState` reports the same.
- **Scene question** is the honest brightness-based summary (no scene VLM bundled), labeled as such.
- **Detector backend** is reported from the real `InferenceStatus` (XNNPACK today; QNN only if a QNN
  model actually loads) — never invented.

## Known gaps (honest)
- A physical refreshable braille display has not been hardware-tested; the app-level semantics + live
  region that feed it are implemented and verified via TalkBack. The custom actions and
  `stateDescription` are exposed through the standard AccessibilityNodeInfo path that braille displays
  consume, but on-hardware confirmation is still pending (see `ACCESSIBILITY_DEMO.md`).

## Manual validation checklist
TalkBack on → swipe-navigate the dashboard (all controls announce) → connect a braille display in
Android Settings → operate by voice/hotkeys with the screen off → trigger OCR → repeat last alert →
start/stop observing → confirm Airplane Mode unaffected. (Full matrix in `ACCESSIBILITY_VALIDATION.md`.)
