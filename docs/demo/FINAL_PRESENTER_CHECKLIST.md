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

## If asked about the NPU
"We built the full QNN/NPU pipeline: the YOLOv8n raw head lowers to Qualcomm HTP and we ship a real
QnnBackend .pte plus the v79 runtime libs. On a retail S25 Ultra the Hexagon DSP refuses to load an
unsigned HTP skel from app storage (`error 4000`) — that needs a signed PD or an engineering build, so
we fall back to XNNPACK CPU and report it truthfully. It already beats the 100 ms danger target at
~32 ms, so there's no functional gap in the demo."

## Hard rules
- [ ] Never say "NPU active" — the status node is the source of truth.
- [ ] Keep Airplane Mode on; OBSERVA needs no network.
- [ ] If anything fails, fall back to the dashboard + logcat showing real on-device inference.

## Release status
v2.2.0 is **built, tested, pushed to `main`, NOT tagged** (QNN/NPU not active on device; tagging
without NPU is the developer's call). XNNPACK path is real, offline, accessible, presentation-ready.
