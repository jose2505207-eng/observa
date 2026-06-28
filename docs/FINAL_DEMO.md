# OBSERVA — Final offline demo

A judge-facing, fully-offline walkthrough. Every step is honest about what is real vs. fallback.
Device: Galaxy S25 Ultra, Android 16. APK: `app/build/outputs/apk/demoOffline/debug/app-demoOffline-debug.apk`
(also attached to the [latest GitHub release](https://github.com/jose2505207-eng/observa/releases/latest)).
For the detailed beat-by-beat script see [`docs/demo/DEMO_SCRIPT.md`](demo/DEMO_SCRIPT.md).

## Pre-flight
1. **Enable Airplane mode** (Wi-Fi + mobile data off). Bluetooth only if pairing a Braille display.
2. Install + grant Camera, Microphone, Notifications:
   `adb install -r app/build/outputs/apk/demoOffline/debug/app-demoOffline-debug.apk`
3. Prove no network capability: `adb shell dumpsys package com.observa.app | grep INTERNET` → **nothing**.

## Script
| # | Step | What to expect (honest) |
|---|---|---|
| 1 | Airplane mode on | Status bar shows ✈. |
| 2 | Launch OBSERVA | Camera preview live; foreground notification "OBSERVA running offline. … No network used." |
| 3 | Confirm no network | Show `dumpsys ... grep INTERNET` empty; app fully functional. |
| 4 | Ambient awareness | Dashboard: Camera Active, Frames rising, FPS ~30, "Backend: QNN/NPU active". |
| 5 | Detect object | Point at a person/chair → real YOLOv8n detection on the **Hexagon NPU**; `OBSERVA_MODEL inference 2ms ... backends=[QnnBackend]`. Spoken "Person ahead" + cue. (Brightness heuristic only as a no-model fallback.) |
| 6 | Directional audio/haptic cue | Hazards produce stereo-panned tones + directional vibration; "on your left/right/ahead". |
| 7 | Trigger OCR | Tap **Read Signs** (or say "read signs"). |
| 8 | Read text aloud | Recognized text spoken + shown in Braille status; "No readable text found." when none. (Offline, ML Kit bundled model.) |
| 9 | Braille/TalkBack status | Braille line shows concise status + alerts; with TalkBack on, controls announce. |
| 10 | Voice control everything | **Volume-up ×3** → "Voice commands." Say "start navigation", "read signs", "download Spanish", "start translation". Deterministic offline parser; declines honestly when a pack is missing. |
| 11 | Voice-to-voice translation | Start translation, speak a sentence → spoken in the target language. Listen → on-device ML Kit translate → speak, looped, fully offline once the pack is installed. |
| 12 | Navigation + haptics | "Start navigation" → real GPS + compass guidance ("Destination ahead-left, 40 meters") with **turn/arrival haptics**. Detection keeps running; a hazard **interrupts** navigation speech. |
| 13 | Foreground service / screen-off | Show the persistent notification + Stop/Mute/Repeat actions. **Honest note:** screen-off background camera capture is not wired; the service keeps the app foregrounded and controllable. |
| 14 | Diagnostics | Dashboard rows: AI model + backend (QNN/NPU + latency), **NPU Data** live graph, Service, Voice, Haptics, Privacy. |
| 15 | Confirm no cloud | Reiterate: no INTERNET permission (demoOffline flavor), no uploads, no streaming — all on-device. |

## What is real vs. fallback (say this plainly)
- **Real, offline, device-verified:** YOLOv8n object detection on the **Hexagon NPU** (~2–3 ms), camera
  loop, hazard engine, TTS, directional audio + haptics, Braille/TalkBack live region, on-demand OCR,
  voice control of every feature, real-time voice-to-voice ML Kit translation, real device-GPS +
  compass navigation with turn haptics, foreground service, adaptive battery/thermal duty cycle, and
  the no-INTERNET privacy guarantee (demoOffline flavor).
- **Fallback:** XNNPACK CPU detection (automatic, on devices without the NPU path); brightness
  heuristic (only if no model loads); Demo Mode hazards (scripted, labeled `[Demo]`).
- **Not verified:** physical refreshable Braille display hardware; a full human sensory pass.
- **Not built:** doors/stairs/curbs/crosswalks (not in COCO-80); turn-by-turn street routing (we do
  orientation/bearing guidance + offline named-place destinations, not rendered tiles).

Never claim a backend the dashboard status node doesn't show, physical Braille hardware, or routing we
don't have. The app reports the live backend honestly — show the node.
