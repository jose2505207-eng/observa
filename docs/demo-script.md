# OBSERVA — Demo Script

Title: **“OBSERVA: Local AI Eyes That Do Not Leak Your Life”**

## Setup (before judges)
1. `./gradlew assembleDebug`
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. `adb shell pm grant com.observa.app android.permission.CAMERA` (avoids fumbling the prompt live)
4. Turn the phone volume **up** (speech) and confirm haptics are on.
5. **Enable Airplane Mode** — leave it on for the whole demo.

## Narrative + actions (~60–90s)
1. **Problem (10s):** "Blind and low-vision users can't reliably aim a camera at the thing they need. Existing AI vision apps assume they can — and send your surroundings to the cloud."
2. **Inversion (10s):** "OBSERVA watches ambiently, understands locally, and warns you proactively — fully offline."
3. **Live proof (20s):** Open the app. Point to the dashboard:
   - Camera **Active**, **FPS ~30**, Frames climbing → the loop is live.
   - Backend **Heuristic (brightness)** and **Inference: ExecuTorch bundled, not invoked** → "we show the truth about our AI path."
   - Privacy **Local only · No network required**, and the ✈ airplane icon → "no cloud, no upload."
4. **Heuristic reaction (10s):** Briefly cover the left side of the lens → a darker third triggers an **Obstacle** alert with direction (spoken + vibration). Cooldown prevents spam.
5. **Demo Mode (20s):** Tap **Start Demo**. The scripted sequence plays through the *same* engine:
   - ~2s "Camera active. Offline mode ready."
   - ~5s "Obstacle ahead, two steps." (high severity → stronger haptic)
   - ~9s "Text detected on your left."
   - ~13s "Doorway left."
   - ~17s "Path clear."
   Every alert is labeled **[Demo]**; backend reads **Demo (simulated)**.
6. **Accessibility (10s):** Turn on TalkBack (or note it): buttons are labeled, the alert banner is an assertive live region, text is large and high-contrast. One primary control (Start Demo) + Mute.
7. **Punchline:** "Fast enough to warn, private enough to trust, simple enough to use without sight — and architected to drop in real ExecuTorch/QNN inference next."

## Honesty (say this)
"Today the live detector is a brightness heuristic and the rich alerts are a scripted demo — both labeled in the UI. ExecuTorch is bundled and the runtime abstraction is ready; we did not fake NPU acceleration."

## Backup plan
- Model/heuristic odd → use **Demo Mode** (deterministic).
- Camera fails → recorded screen video.
- Audio fails → the on-screen alert banner + haptics still convey alerts.
- Install fails → show `./gradlew assembleDebug` success + screen recording.

## On-device validation checklist
- [ ] Launch, grant camera, preview shows.
- [ ] Frames increase, FPS updates (~30).
- [ ] Cover lens → obstacle alert; repeated frames don't spam (cooldown).
- [ ] Start Demo → spoken + haptic alerts, each labeled [Demo].
- [ ] Dashboard says Local/offline; airplane mode on.
- [ ] No NPU/QNN/real-inference claim anywhere.
