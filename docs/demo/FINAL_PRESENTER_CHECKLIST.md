# OBSERVA — Final presenter checklist (v3.1.0)

Truthful, presentation-ready. The detector runs **real local ExecuTorch inference on the Hexagon
NPU, fully offline** (~2–3 ms, device-verified). XNNPACK CPU remains the automatic fallback on
devices without the NPU path. The app reports the live backend honestly — show the status node.

## 5 minutes before
- [ ] Device: Galaxy S25 Ultra, charged, screen timeout long.
- [ ] Install: `adb install -r app/build/outputs/apk/demoOffline/debug/app-demoOffline-debug.apk`
- [ ] Grant camera + microphone when prompted.
- [ ] **Airplane Mode ON** (proves offline). Wi-Fi/data off.
- [ ] Optional: TalkBack ON for the accessibility segment.
- [ ] Optional log window: `adb logcat -s OBSERVA_MODEL OBSERVA_NPU OBSERVA_EXECUTORCH`

## What to show
1. **Offline real inference on the NPU.** Open app in Airplane Mode. Dashboard "AI model: loaded —
   QNN/NPU"; `OBSERVA_MODEL` logs `inference 2ms (avg 2ms) ... forward backends=[QnnBackend]`. Point
   at a person → spoken "Person ahead" + haptic/audio cue.
2. **No network.** `aapt2 dump permissions app-demoOffline-debug.apk | grep INTERNET` → nothing.
   Mention the manifest strips INTERNET in the demoOffline flavor.
3. **Native accessibility operating layer.** With TalkBack, swipe to the top nodes: **Current status**
   ("Awareness active. Detector backend: QNN/NPU. OCR ready."), **Last alert**, **Available actions**.
   Open TalkBack actions menu on Available actions → run **Repeat last alert**, **Start OCR**,
   **Open debug status**. Everything works without looking.
4. **Honest backend status.** "Open debug status" speaks the live backend
   (`Detector backend: QNN/NPU active, ~2 ms`) plus packs, GPS, compass, OCR, voice, INTERNET (not
   declared in this flavor), build sha/time.
5. **Voice control of everything (v3.1.0).** **Volume-up ×3** → "Voice commands. Speak now." Then by
   voice: *start/stop navigation*, *read signs*, *download map*, *download ‹language›* (~45), *start/
   stop translation*. Deterministic offline parser; declines honestly when a pack is missing.
6. **Real-time voice-to-voice translation (v3.1.0).** Start translation, speak a sentence → the app
   speaks it in the target language. Listen → on-device ML Kit translate → speak-in-target, looped,
   fully offline once the pack is installed. Honest when a pack or target TTS voice is missing.
7. **GPS Orientation + nav haptics.** Actions menu / "start navigation" → "Destination ahead-left, 40
   meters" with **turn/arrival haptics** (left/right pulse, forward buzz when aligned). Rotate to see
   it update. Point at a person → the hazard **interrupts** orientation (safety priority). Real
   satellite GPS + compass, honest confidence; not turn-by-turn maps.

## If asked about the NPU
"OBSERVA runs the YOLOv8n detector on the Snapdragon **Hexagon NPU (HTP v79)** at ~2–3 ms, fully
offline and device-verified (`forward backends=[QnnBackend]`, `QNN/NPU ACTIVE`) — about 10–15× faster
than the XNNPACK CPU path, which we keep as the automatic fallback. The enabler was one manifest line:
`<uses-native-library android:name="libcdsprpc.so" android:required="false"/>`, which grants the app
access to the vendor cDSP FastRPC client on Android 12+. The earlier `skel 4000` failure we'd seen was
that missing native-library access, not a signing block — we corrected that conclusion honestly. We
never set the QNN-active flag unless a real warm-up forward succeeds, so the status node is always
truthful."

## Download Maps + Languages (v3.1.0)
- [ ] Build both flavors: `./gradlew assembleDemoOfflineDebug assembleProvisioningDebug`.
- [ ] **Translation demo:** install **provisioning** build → **Download Languages** → download any of
  ~45 languages (needs internet, one time). Then install **demoOffline** build (or Airplane Mode) →
  type or speak a phrase → **Translate** → real output, fully offline.
- [ ] **Map demo (real area):** **provisioning** build → **Download Map** → **Download Area Map** pulls
  the real named places around you from OpenStreetMap and stores them offline as nav destinations.
  (Demo waypoint pack also installs offline in any build.)
- [ ] Privacy proof: `aapt2 dump permissions <demoOffline apk> | grep INTERNET` → nothing.
  `<provisioning apk>` → INTERNET present (clearly the Setup build).

## Maps / Translate visibility
- [ ] Main screen shows big labeled buttons: **Awareness · Navigate · Download Map · Translate ·
  Download Languages · Voice Commands · Read Signs · Repeat Alert**.
- [ ] **Navigate** → Navigation card: heading/bearing/distance + map-pack status. **Translate** →
  Translation card: honest readiness. **Debug Status** → backend, NPU stage, packs, GPS, compass, OCR,
  voice, INTERNET (not declared), build sha/time. **NPU Data** → live latency/throughput graph.
- [ ] Everything also reachable via TalkBack Available Actions and via gestures when TalkBack is off.

## Blind-first gestures (TalkBack off only)
- [ ] With TalkBack OFF, on the camera surface: triple-tap → voice; swipe up → translation; swipe
  down → orientation; double-tap → repeat. Hint line shows the gesture map.
- [ ] With TalkBack ON, the hint flips to "Gestures available through TalkBack actions," and every
  action (incl. **Open voice commands**) is in the TalkBack actions menu. Never depend on raw
  one-finger gestures while TalkBack is on.

## Hard rules
- [ ] The status node is the source of truth — it reads QNN/NPU active because it is; never claim a
  backend the node doesn't show.
- [ ] Keep Airplane Mode on; OBSERVA needs no network for the core demo.
- [ ] If anything fails, fall back to the dashboard + logcat showing real on-device NPU inference.

## Release status
v3.1.0 is **built (both flavors), 190 tests green, pushed to `main`, tagged `v3.1.0`**. NPU active and
device-verified; demoOffline has no INTERNET. Presentation-ready.
