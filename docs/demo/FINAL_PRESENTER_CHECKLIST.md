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
   orientation** → hear "Destination ahead-left, 40 meters. Turn slightly left." Rotate to see it
   update. Point at a person → the hazard **interrupts** orientation (safety priority). It's
   heading/bearing/distance with honest confidence (good / weak GPS / compass unstable), not maps;
   fully offline (satellite GPS + compass).
6. **Offline Translation Mode (honest).** Actions menu → **Start translation mode** → hears
   "Translation needs an offline language pack, which is not installed. OBSERVA never translates over
   the network." The full offline turn pipeline is built (speech → language-ID → translate → speak),
   only the engine/pack assets are deferred to a provisioning flavor. Wired and truthful, not faked.

## If asked about the NPU
"We built the full QNN/NPU pipeline — the YOLOv8n raw head lowers to Qualcomm HTP, we ship a real
QnnBackend .pte and the v79 runtime. We just re-verified the entire host+export chain from scratch
(rebuilt the QNN host adaptor, re-exported the HTP model — both pass clean) and the .pte loads on the
phone; the only failure is the on-device HTP **skel load** (`error 4000`). On this *retail* S25 Ultra
the Hexagon DSP refuses to load an unsigned HTP skel from a third-party app. We proved it's an
OS-level lock, not our bug or a build artifact, two ways: ExecuTorch QNN and Google's official LiteRT
Qualcomm delegate fail *identically*, while in the same logcat the phone's own camera opens an
unsigned PD on that DSP and loads its skel. Running our model on the NPU would need a
signed/privileged/`userdebug` build or an OEM allowlist. So we run real on-device inference on XNNPACK
CPU at ~22–32 ms — under the 100 ms danger target — and the app reports the backend honestly."

## Download Maps + Languages (v2.5.0 — the user's ask)
- [ ] Build both flavors: `./gradlew assembleDemoOfflineDebug assembleProvisioningDebug`.
- [ ] **Translation demo:** install **provisioning** build → **Download Languages** → Download Spanish +
  English (needs internet, one time). Then install **demoOffline** build (or Airplane Mode) → type
  "¿Dónde está la entrada?" → **Translate** → real English, fully offline.
- [ ] **Map demo:** **Download Map** → **Install Demo Map Pack** (works offline, any build) → navigation
  shows "Map ready offline".
- [ ] Privacy proof: `aapt2 dump permissions <demoOffline apk> | grep INTERNET` → nothing.
  `<provisioning apk>` → INTERNET present (clearly the Setup build).

## Maps / Translate visibility (the user's ask)
- [ ] Main screen shows big labeled buttons: **Awareness · Navigate · Translate · Voice Commands ·
  Read Signs · Repeat Alert**.
- [ ] **Navigate** → Navigation card: heading/bearing/distance + "Map pack missing" (compass works
  without a pack). **Translate** → Translation card: honest readiness. **Debug Status** → backend,
  QNN stage, packs, GPS, compass, OCR, voice, INTERNET (not declared), build sha/time.
- [ ] Everything also reachable via TalkBack Available Actions (Navigate, Translate, Voice commands,
  Read signs) and via gestures when TalkBack is off.

## Blind-first gestures (TalkBack off only)
- [ ] With TalkBack OFF, on the camera surface: triple-tap → voice; swipe up → translation; swipe
  down → orientation; double-tap → repeat. Hint line shows the gesture map.
- [ ] With TalkBack ON, the hint flips to "Gestures available through TalkBack actions," and every
  action (incl. **Open voice commands**) is in the TalkBack actions menu. Never depend on raw
  one-finger gestures while TalkBack is on.

## Hard rules
- [ ] Never say "NPU active" — the status node is the source of truth.
- [ ] Keep Airplane Mode on; OBSERVA needs no network.
- [ ] If anything fails, fall back to the dashboard + logcat showing real on-device inference.

## Release status
v2.2.0 is **built, tested, pushed to `main`, NOT tagged** (QNN/NPU not active on device; tagging
without NPU is the developer's call). XNNPACK path is real, offline, accessible, presentation-ready.
