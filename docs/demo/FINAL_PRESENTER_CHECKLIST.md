# OBSERVA — Final presenter checklist (v2.2.0)

Truthful, presentation-ready. The detector runs **real local ExecuTorch inference, fully offline**.
NPU was pursued honestly; on this production handset it falls back to **XNNPACK CPU** (~32 ms) and the
app says so. Do not claim NPU active.

## 5 minutes before
- [ ] Device: Galaxy S25 Ultra, charged, screen timeout long.
- [ ] Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- [ ] Grant camera + microphone when prompted.
- [ ] **Airplane Mode ON** (proves offline). Wi-Fi/data off.
- [ ] Optional: TalkBack ON for the accessibility segment.
- [ ] Optional log window: `adb logcat -s OBSERVA_MODEL OBSERVA_EXECUTORCH`

## What to show
1. **Offline real inference.** Open app in Airplane Mode. Dashboard "AI model: loaded — CPU";
   `OBSERVA_MODEL` logs `inference ~30ms ... backends=[XnnpackBackend]`. Point at a person → spoken
   "Person ahead" + haptic/audio cue.
2. **No network.** `aapt2 dump permissions app-debug.apk | grep INTERNET` → nothing. Mention the
   manifest strips INTERNET via merger.
3. **Native accessibility operating layer.** With TalkBack, swipe to the top nodes: **Current status**
   ("Awareness active. Detector backend: XNNPACK. OCR ready."), **Last alert**, **Available actions**.
   Open TalkBack actions menu on Available actions → run **Repeat last alert**, **Start OCR**,
   **Open debug status**. Everything works without looking.
4. **Honest backend status.** "Open debug status" speaks
   `Detector backend: XNNPACK CPU fallback. QNN attempted: <skel 4000> ...`.
5. **GPS Orientation Lite** (outdoors / near a window; grant location). Actions menu → **Start
   orientation** → hear "Slight right, 1 o'clock. 40 meters." Rotate to see it update. Point at a
   person → the hazard **interrupts** orientation (safety priority). It's heading/bearing/distance,
   not maps; fully offline (satellite GPS).
6. **Offline Translation Mode (honest).** Actions menu → **Start translation mode** → hears
   "Translation needs an offline language pack, which is not installed. OBSERVA never translates over
   the network." Shows the mode is wired and truthful, not faked.

## If asked about the NPU
"We built the full QNN/NPU pipeline — the YOLOv8n raw head lowers to Qualcomm HTP, we ship a real
QnnBackend .pte and the v79 runtime. On this *retail* S25 Ultra the Hexagon DSP refuses to load an
unsigned HTP skel from a third-party app (`error 4000`). We proved it's an OS-level lock, not our bug,
two ways: ExecuTorch QNN and Google's official LiteRT Qualcomm delegate fail *identically*, while the
phone's own camera loads DSP skels at the same moment. Running our model on the NPU would need a
signed/privileged build or an engineering device. So we run real on-device inference on XNNPACK CPU at
~32 ms — under the 100 ms danger target — and the app reports the backend honestly."

## Hard rules
- [ ] Never say "NPU active" — the status node is the source of truth.
- [ ] Keep Airplane Mode on; OBSERVA needs no network.
- [ ] If anything fails, fall back to the dashboard + logcat showing real on-device inference.

## Release status
v2.2.0 is **built, tested, pushed to `main`, NOT tagged** (QNN/NPU not active on device; tagging
without NPU is the developer's call). XNNPACK path is real, offline, accessible, presentation-ready.
