# Braille display support — design & decision

## What OBSERVA does today (app-level, shipped)

OBSERVA exposes a concise, structured status string through standard Android
accessibility so it reaches **TalkBack and any Braille display TalkBack drives**:

- `accessibility/BrailleStatusState` — Compose state holding the current line.
- `ui/ObservaScreen.BrailleStatus` — renders it in a **polite** `liveRegion`
  with a `contentDescription` and a stable `testTag("brailleStatus")`.
- `accessibility/BrailleStatusPresenter` — formats short lines
  (`"OBSERVA: observing on, AI fallback"`, `"Obstacle center"`, `"Muted"`).
  An active hazard takes the whole line so urgent text is never buried.
- `accessibility/AccessibilityOutputRouter` — fans hazards/responses to speech +
  the Braille line, throttled by `OutputThrottler`; keeps the last message for
  **repeat**.
- Controls: a **Braille status** toggle (UI Switch + voice `braille on/off/status`).

This is the recommended, policy-safe path: TalkBack already supports refreshable
Braille displays (USB/Bluetooth) via *TalkBack ▸ Settings ▸ Braille display*, and
any app's `liveRegion`/`contentDescription` is surfaced to that display with no
special permission, no custom Bluetooth code, and no network.

### How to verify with a physical display

1. Pair the Braille display in Android *Settings ▸ Accessibility ▸ TalkBack ▸
   Braille display* (Bluetooth on).
2. Open OBSERVA. The status box and hazard banner are accessibility live regions,
   so their text appears on the Braille display and refreshes as state changes.
3. Toggle **Braille status** off to stop ambient updates (safety hazards still
   speak); on to resume.

## The direct API (`BrailleDisplayController`) — intentionally deferred

Android 15+ (API 35; the S25 Ultra runs Android 16/API 36) adds
`android.accessibilityservice.BrailleDisplayController` for **direct** read/write
to a connected Braille display. Tradeoffs:

- It is only available to an **AccessibilityService** (`BIND_ACCESSIBILITY_SERVICE`),
  and needs `<property android:name="android.accessibilityservice.BrailleDisplay" />`
  plus user enablement in Accessibility settings.
- Running OBSERVA's core as an AccessibilityService would be **disproportionate**:
  it broadens privileges, risks interfering with TalkBack, and adds a manual setup
  step — for no benefit over the live-region path for our short status strings.

**Decision for this run:** do *not* add an AccessibilityService. Keep Braille
support at the accessibility-semantics layer (above), which is privacy-preserving,
offline, TalkBack-compatible, and testable.

**If a direct integration is pursued later**, it must be: optional (core OBSERVA
works without it), privacy-preserving (no new data leaves the device), additive to
(not replacing) TalkBack, documented with setup steps, and manually verified on a
physical display before any claim.

## Status

- App-level live-region Braille channel: **implemented**.
- TalkBack exposure: **implemented** (verify on device — see device-validation notes).
- Physical Braille display: **not verified** (no hardware on hand this run).
- Direct `BrailleDisplayController` service: **intentionally deferred** (documented above).
