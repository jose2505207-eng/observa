# OBSERVA — Final offline demo

A judge-facing, fully-offline walkthrough. Every step is honest about what is real vs. fallback.
Device: Galaxy S25 Ultra, Android 16. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Pre-flight
1. **Enable Airplane mode** (Wi-Fi + mobile data off). Bluetooth only if pairing a Braille display.
2. Install + grant Camera, Microphone, Notifications:
   `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Prove no network capability: `adb shell dumpsys package com.observa.app | grep INTERNET` → **nothing**.

## Script
| # | Step | What to expect (honest) |
|---|---|---|
| 1 | Airplane mode on | Status bar shows ✈. |
| 2 | Launch OBSERVA | Camera preview live; foreground notification "OBSERVA running offline. … No network used." |
| 3 | Confirm no network | Show `dumpsys ... grep INTERNET` empty; app fully functional. |
| 4 | Ambient awareness | Dashboard: Camera Active, Frames rising, FPS ~30, "Backend: Heuristic (brightness)". |
| 5 | "Detect" object | **Honest note:** no ML model is bundled, so the live detector is the brightness heuristic (generic obstacle by region), **not** semantic object recognition. Use **Start Demo** to show the full scripted hazard → cue → speech path. |
| 6 | Directional audio/haptic cue | Demo hazards produce stereo-panned tones + directional vibration; "on your left/right/ahead". |
| 7 | Trigger OCR | Tap **Read Text** (or say "read text"). |
| 8 | Read text aloud | Recognized text spoken + shown in Braille status; "No readable text found." when none. (Offline, ML Kit bundled model.) |
| 9 | Braille/TalkBack status | Braille line shows concise status ("OBSERVA: observing on, AI fallback") + alerts; with TalkBack on, controls announce. |
| 10 | Start offline navigation | Scroll to "Navigation (offline)", tap **Go: the park** (or say "navigate to the park"). |
| 11 | Compass-relative guidance | Spoken clock-face guidance + distance ("Slight right, 1 o'clock. 15 meters") with "GPS accuracy low. Use caution." **Honest note:** real device compass + a documented **demo location** (no live GPS in this build). |
| 12 | Foreground service / screen-off | Show the persistent notification + Stop/Mute/Repeat actions. **Honest note:** screen-off *background camera capture* is not yet wired; the service keeps the app foregrounded and controllable. |
| 13 | Diagnostics | Dashboard rows: AI model + AI detail (model/QNN), Service (duty cycle), Voice, Haptics, Privacy. |
| 14 | Confirm no cloud | Reiterate: no INTERNET permission, no uploads, no streaming — all on-device. |

## What is real vs. fallback (say this plainly)
- **Real, offline, device-verified:** camera loop, heuristic hazard detection, TTS, directional
  audio + haptics, Braille/TalkBack live region, on-demand OCR, voice commands, foreground service +
  notification, adaptive battery/thermal duty cycle, compass-relative navigation guidance, the
  no-INTERNET privacy guarantee.
- **Fallback / simulated:** object recognition (heuristic, no ML model bundled), Demo Mode hazards
  (scripted, labeled `[Demo]`), navigation location (demo fix, real compass).
- **Detected but not active:** QNN acceleration.
- **Not verified:** physical Braille display, live GPS movement.

Never claim object recognition, QNN acceleration, physical Braille, or GPS precision that isn't
demonstrably running.
