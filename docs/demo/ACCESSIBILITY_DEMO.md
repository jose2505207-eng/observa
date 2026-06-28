# Accessibility demo & manual validation (v2.2.0)

OBSERVA is built to be operated **without looking at the screen**. This is the hands-on script for
validating the native TalkBack / braille operating layer. It needs a real device (the operating layer
uses Android accessibility services); the unit-tested logic lives in `AccessibilityStatusReducer`.

## What v2.2.0 adds
- Three stable, focusable accessible nodes at the top of the screen: **Current status**, **Last
  alert**, **Available actions**.
- TalkBack/braille **custom actions** on the Available-actions node (no custom gesture required):
  Start/Pause awareness, Repeat last alert, Start OCR, Start scene question, Start translation mode,
  Silence alerts, Open debug status.
- `stateDescription` on toggles and semantic state lines ("Awareness active", "Detector backend:
  XNNPACK", "OCR ready", "Translation not installed").

## Manual validation checklist

Run on a Galaxy S25 Ultra (or any TalkBack device), ideally in **Airplane Mode**.

1. **TalkBack enabled** — Settings → Accessibility → TalkBack → On.
2. **Navigate the app without looking** — swipe right/left through controls; confirm each announces a
   clear label and (for toggles) an "on/off" state. The first nodes reached are the operating layer.
3. **Read current status** — focus the "Current status" node; confirm it reads e.g.
   *"Awareness active. Detector backend: XNNPACK. OCR ready."* and does **not** re-announce on its own.
4. **Repeat last alert** — with the Available-actions node focused, open TalkBack's **Actions menu**
   (swipe up-then-right) → choose **Repeat last alert**. Confirm the last alert is re-spoken.
5. **Start / pause awareness** — via the Actions menu choose **Pause awareness**, then **Start
   awareness**. Confirm spoken confirmation and that the Observing toggle state follows.
6. **Trigger OCR action** — Actions menu → **Start OCR, read text**; point at text; confirm it reads
   or honestly says no readable text.
7. **Trigger translation action (honest unavailable)** — Actions menu → **Start translation mode**;
   confirm it says translation is **not installed** (no fake translation).
8. **Open debug status** — Actions menu → **Open debug status**; confirm engineering detail (backend,
   shapes, latency) is spoken here and **never** mixed into normal alerts.
9. **Hazard priority** — point the camera at a person/obstacle; confirm the hazard interrupts
   (assertive) and that ambient status never buries it.
10. **No braille flooding** — connect a refreshable braille display (Settings → Accessibility →
    Braille display / BrailleBack). Confirm the operating-layer nodes update only when focused, while
    the polite status line stays concise.
11. **Confirm no network permission** — `aapt dump permissions app-debug.apk` shows no `INTERNET`.
12. **Confirm detector still runs offline** — in Airplane Mode, the dashboard "AI model" shows
    *loaded — CPU* and `OBSERVA_MODEL` logcat reports per-frame inference (~32 ms).

## Physical gestures — what is real vs. system-level (honest)

The desired eyes-free UX map (below) mixes app-interceptable inputs with **Samsung/Android
system-level gestures the app cannot reliably receive**. We never pretend to implement the latter.

| Desired gesture | Reality |
|---|---|
| Double-tap **Back**: repeat last / status | **System-level.** With TalkBack on, taps are consumed by the screen reader. Map it via **TalkBack → Customize gestures** or a **Universal Switch / Side-key** binding to the in-app action. Not app-intercepted. |
| Triple-tap **Back**: pause/resume awareness | Same — configure as a TalkBack/Bixby/side-key shortcut. |
| Single/double/hold **front (screen)** taps | With TalkBack on, single tap = focus, double tap = activate focused item. Use the focused **Available-actions** custom actions instead of bespoke taps. Hold-to-talk is the on-screen **Voice Command** button. |
| **Volume buttons** (≤3 clicks/button) | **Really implemented** in-app via `HotkeySequencer` (foreground, opt-in). See the table below. |

**Guaranteed non-visual paths that do not depend on any unsupported gesture:** TalkBack custom
actions, offline voice commands, and volume-rocker hotkeys.

See `docs/implementation/UX_INPUT_MAP.md` for the volume-key table and voice grammar.
