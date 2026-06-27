# Manual sensory & accessibility test plan

Human-in-the-loop checklist for OBSERVA's output and OCR layer. Run on a device
(reference: Galaxy S25 Ultra, Android 16) in **Airplane Mode**. These checks cover
behavior that automated/JVM tests cannot (audible, tactile, screen-reader, Braille
hardware). Record pass/fail + notes.

## Setup
- Airplane Mode **ON**; Wi-Fi OFF; mobile data OFF.
- Bluetooth ON only if testing a physical Braille display.
- Grant Camera (required) and Microphone (for voice) when prompted.
- Set media volume to a comfortable, audible level.
- `adb logcat -s OBSERVA_EXECUTORCH OBSERVA_QNN OBSERVA_MODEL OBSERVA_OCR OBSERVA_TTS OBSERVA_ASR OBSERVA_CAMERA OBSERVA_AUDIO`

## 1. TalkBack enabled
- Enable TalkBack. Navigate the screen with swipe gestures.
- [ ] Every control announces a useful label (Start Demo, Mute, Observing,
      Braille status, Audio cues, Haptics, Read Text, Voice Command).
- [ ] The status box (`testTag brailleStatus`) is announced as a polite live
      region and re-announces when state changes.
- [ ] Hazard banner announces assertively (interrupts) during Demo.

## 2. Braille display via TalkBack
- Pair a refreshable Braille display in *Settings ▸ Accessibility ▸ TalkBack ▸
      Braille display* (Bluetooth).
- [ ] The status line ("OBSERVA: observing on, AI fallback") appears on the display.
- [ ] Toggling **Braille status** off stops ambient updates; on resumes.
- [ ] An OCR result and a Demo hazard both surface on the display.
- (If no hardware: mark "not verified — no device".)

## 3. Speech muted
- Tap **Mute** (or say "mute").
- [ ] Speech stops.
- [ ] Audio cues and haptics STILL fire on a Demo hazard (safety).
- [ ] Braille/live-region status still updates.
- Say "unmute" → [ ] speech resumes (and does NOT mute — regression guard).

## 4. Haptics only
- Mute speech; turn **Audio cues** off; keep **Haptics** on.
- Run Demo.
- [ ] Left/right/forward hazards produce distinguishable vibration patterns.
- [ ] An urgent (HIGH) hazard produces the stronger stop pattern.

## 5. Airplane mode (offline proof)
- [ ] App launches and runs fully with Airplane Mode on.
- [ ] No sign-in, no network prompt.
- [ ] `dumpsys package com.observa.app | grep INTERNET` → no INTERNET permission.

## 6. OCR on demand
- Point the camera at printed text. Tap **Read Text** (or say "read text").
- [ ] App says "Reading text…", then speaks the recognized text.
- [ ] Recognized text appears in the Braille/live-region status.
- [ ] With no text in view → says "No readable text found."
- [ ] OCR does NOT run continuously (only on tap/command).
- [ ] Works with Airplane Mode on (offline model).

## 7. Repeat last message
- After any alert/OCR result, say "repeat" (or trigger repeat).
- [ ] The last meaningful message is spoken again.

## 8. Hazard interruption behavior
- Start a Read Text on a long passage, then run Demo (or trigger a hazard).
- [ ] A HAZARD interrupts in-progress OCR/navigation speech immediately.
- [ ] A lower-priority message (INFO/MODE) does NOT interrupt a hazard.

## 9. Model fallback honesty (regression)
- [ ] With no `observa_detector.pte`, dashboard shows "AI model: unavailable —
      heuristic fallback"; logs show QNN "present (not active)".
- [ ] No object labels (person/stairs/etc.) are ever spoken from the heuristic.

## Result log
| Section | Result | Notes |
|---|---|---|
| 1 TalkBack | | |
| 2 Braille display | | |
| 3 Speech muted | | |
| 4 Haptics only | | |
| 5 Airplane mode | | |
| 6 OCR on demand | | |
| 7 Repeat | | |
| 8 Hazard interruption | | |
| 9 Fallback honesty | | |
