# Manual test — offline navigation (v1.7)

Device: Galaxy S25 Ultra, Android 16, **Airplane Mode**. Grant Camera, Microphone, Notifications.

## Scope (honest)
Guidance-first, bearing-to-destination. Heading is the **real device compass**; location is a
**documented demo fix** (no live GPS in this build). See `docs/offline-navigation.md`.

## Checks
| # | Check | Pass criteria | Status |
|---|---|---|---|
| 1 | Build / unit tests | assembleDebug + 106 tests pass | **PASS** |
| 2 | No INTERNET / no location permission | `aapt2 dump permissions` shows neither | **PASS** |
| 3 | Airplane-mode launch, camera preview | runs, preview live, no crash | **PASS** |
| 4 | Nav panel renders accessibly | "Navigation (offline)" + Go/Where am I/Stop buttons, TalkBack labels, testTag | **PASS** |
| 5 | Select destination without sight | tap "Go: the park" → guidance spoken + Braille | **PASS** (device: "Destination ahead. home." for demo-start target) |
| 6 | Heading-relative instructions | clock-face + distance toward target | unit-verified (`GuidanceEngine`); device demo-start gives arrival |
| 7 | GPS/compass uncertainty announced | "GPS accuracy low…" / "Compass accuracy low. Calibrate phone." | **PASS** (guidance appends; unit-tested) |
| 8 | Stop navigation | "Stop nav" / voice "stop navigation" ends session; "stop" alone still stops observing | **PASS** (unit-tested; Stop nav disabled when idle) |
| 9 | Where am I | speaks current guidance or "Not navigating…" | **PASS** |
| 10 | Hazard overrides navigation | a HAZARD interrupts guidance (router priority) | unit-verified (`OutputArbiter`) |
| 11 | OCR / cues / Braille still work | unchanged | **PASS** (OCR read real text during testing) |
| 12 | Voice nav | "navigate to the park", "where am i", "stop navigation" parse + route | unit-verified |

## How to verify
```
adb shell pm dump com.observa.app | grep -iE "INTERNET|ACCESS_FINE"   # expect none
# In app: scroll to Navigation (offline), tap Go: <place>; listen for guidance + watch Braille line.
# Say "where am i", "stop navigation".
```

## Blocked / not validated
- Live GPS movement (no GPS provider; demo fix only).
- Real long-distance directional walk-through (needs live location + movement).

## Result log
| # | Result | Notes |
|---|---|---|
| 1–12 | | |
