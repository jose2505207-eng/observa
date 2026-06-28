# OBSERVA — demo script (v2.2.0)

A tight ~4-minute walkthrough. Honest throughout: real local inference, offline, **XNNPACK CPU** on
this device (NPU pursued, documented, not claimed active). Companion:
[`FINAL_PRESENTER_CHECKLIST.md`](FINAL_PRESENTER_CHECKLIST.md),
[`AIRPLANE_MODE_DEMO.md`](AIRPLANE_MODE_DEMO.md), [`ACCESSIBILITY_DEMO.md`](ACCESSIBILITY_DEMO.md).

## Setup (once)
```
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
```
Enable **Airplane Mode**. Grant camera + mic.

## Beat 1 — "It runs locally, offline" (60s)
- Launch OBSERVA. Note the persistent "running offline" notification.
- Point the camera at a person and a chair.
- Spoken: "Person ahead" / obstacle cue; stereo audio + directional haptic fire.
- Show logcat: `OBSERVA_MODEL: inference 26ms (avg 31ms) ... backends=[XnnpackBackend]`.
- Line: *"Real YOLOv8n via ExecuTorch, on-device, in Airplane Mode — median ~32 ms, under our 100 ms
  danger target."*

## Beat 2 — "Private by construction" (30s)
- `aapt2 dump permissions app-debug.apk | grep -i internet` → no output.
- Line: *"No INTERNET permission. The app physically cannot upload an image or a frame."*

## Beat 3 — "Accessible without looking" (90s)
- Turn on TalkBack. Swipe to the top three nodes: Current status, Last alert, Available actions.
- On **Available actions**, open the TalkBack actions menu and run:
  - **Repeat last alert** → re-speaks the last hazard.
  - **Start OCR** → reads text in view (or honest "no readable text").
  - **Open debug status** → speaks the exact backend + latency.
- Line: *"Eight core actions are native accessibility actions — operable by TalkBack and a refreshable
  braille display, no visual buttons required."*

## Beat 4 — "We chased the NPU honestly" (40s)
- Open debug status: *"Detector backend: XNNPACK CPU fallback. QNN attempted: skel load 4000."*
- Line: *"The full Qualcomm QNN/NPU pipeline is in the app — a real HTP-lowered model plus the v79
  runtime. The retail DSP blocks unsigned skels, so we fall back to CPU and tell the truth. No fake
  acceleration."*

## Close
*"OBSERVA: real local AI vision for blind and low-vision users — offline, private, accessible by
design."*

## Backup if the camera/device misbehaves
- Start Demo Mode (scripted hazards) to show the output pipeline end to end.
- Or show `adb logcat -s OBSERVA_MODEL` proving live on-device inference.
