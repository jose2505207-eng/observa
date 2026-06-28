# OBSERVA — demo script (v3.1.0)

A tight ~4–5 minute walkthrough. Honest throughout: real local inference, offline, running on the
**Hexagon NPU** on this device (~2–3 ms, device-verified). Companion:
[`FINAL_PRESENTER_CHECKLIST.md`](FINAL_PRESENTER_CHECKLIST.md),
[`AIRPLANE_MODE_DEMO.md`](AIRPLANE_MODE_DEMO.md), [`ACCESSIBILITY_DEMO.md`](ACCESSIBILITY_DEMO.md).

## Setup (once)
```
adb install -r app/build/outputs/apk/demoOffline/debug/app-demoOffline-debug.apk
adb logcat -c
```
Enable **Airplane Mode**. Grant camera + mic. (The `demoOffline` APK has **no INTERNET permission**.)

## Beat 1 — "It runs locally, offline, on the NPU" (60s)
- Launch OBSERVA. Note the persistent "running offline" notification.
- Point the camera at a person and a chair.
- Spoken: "Person ahead" / obstacle cue; stereo audio + directional haptic fire.
- Show logcat: `OBSERVA_MODEL: inference 2ms (avg 2ms) ... forward backends=[QnnBackend]; QNN/NPU ACTIVE`.
- Line: *"Real YOLOv8n via ExecuTorch, running on the Snapdragon Hexagon NPU, on-device, in Airplane
  Mode — about 2–3 ms per frame, ~10–15× faster than CPU and far under our 100 ms danger target."*

## Beat 2 — "Private by construction" (30s)
- `aapt2 dump permissions app-demoOffline-debug.apk | grep -i internet` → no output.
- Line: *"No INTERNET permission. The app physically cannot upload an image or a frame."*

## Beat 3 — "Accessible without looking" (90s)
- Turn on TalkBack. Swipe to the top three nodes: Current status, Last alert, Available actions.
- On **Available actions**, open the TalkBack actions menu and run:
  - **Repeat last alert** → re-speaks the last hazard.
  - **Start OCR** → reads text in view (or honest "no readable text").
  - **Open debug status** → speaks the exact backend + latency (now: QNN/NPU active, ~2–3 ms).
- Line: *"The core actions are native accessibility actions — operable by TalkBack and a refreshable
  braille display, no visual buttons required."*

## Beat 4 — "Voice control of everything, hands-free" (60s)
- **Volume-up ×3** → "Voice commands. Speak now." Then, by voice:
  - *"Start navigation"* / *"Stop navigation"*, *"Read signs"*, *"Download map"*, *"Download Spanish"*
    (any of ~45 languages), *"Start translation"* / *"Stop translation"*.
- Line: *"Every feature is reachable by voice — volume-up three times, then just say it. The parser is
  deterministic and offline; it checks stop-commands before start-commands so 'stop navigation' is
  never misread, and it declines honestly when a pack isn't installed."*

## Beat 5 — "Real-time voice-to-voice translation" (40s)
- Start translation; speak a sentence in one language → the app speaks it in the target language.
- Line: *"Listen → on-device ML Kit translation → speak in the target language, looped — fully offline
  once the language pack is downloaded. No cloud, and it's honest when a pack or a target-language TTS
  voice is missing."*

## Beat 6 — "Maps, navigation and haptics" (40s)
- Tap **Navigate** (or say "start navigation") → Navigation card shows live heading/bearing/distance
  ("Destination ahead-left, 40 meters") with **turn/arrival haptics** (left/right pulse, forward buzz
  when aligned). Hazard detection keeps running and **interrupts** navigation speech.
- **Download Map** (provisioning build): pulls the **real named places around you** from OpenStreetMap
  and stores them offline as navigation destinations — real place data, not rendered tiles, labeled so.
- Line: *"Real device GPS + compass guidance with turn haptics; a one-time map download of your actual
  surroundings then works offline. Every status is honest."*

## Close
*"OBSERVA: real local AI vision for blind and low-vision users — on the NPU, offline, private,
accessible by design, and fully controllable by voice."*

## Backup if the camera/device misbehaves
- Start Demo Mode (scripted hazards) to show the output pipeline end to end.
- Or show `adb logcat -s OBSERVA_MODEL` proving live on-device NPU inference.
