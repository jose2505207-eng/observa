# Manual test — real detector

Run after bundling `app/src/main/assets/models/observa_detector.pte` (see
`docs/real-detector.md`). Device: Galaxy S25 Ultra, **Airplane Mode**.

## Setup
- Airplane Mode ON; Wi-Fi/data OFF; Bluetooth only for Braille display.
- Grant Camera (+ Microphone for voice).
- `adb logcat -s OBSERVA_EXECUTORCH OBSERVA_MODEL OBSERVA_QNN OBSERVA_OCR OBSERVA_CAMERA`

## Diagnostics to read (dashboard "AI model" + "AI detail" rows / logcat)
- model asset present, model loaded, input shape, output shape, last + average latency,
  `forward` backends, QNN active true/false.

## Checks
| # | Check | Pass criteria |
|---|---|---|
| 1 | Airplane mode launch | App runs offline, no crash, no INTERNET permission |
| 2 | Camera preview | visible and live |
| 3 | Model detected | logcat `model present`; dashboard "AI model: loaded — CPU/QNN" |
| 4 | Model loads | `OBSERVA_EXECUTORCH: load success … forward backends=[…]` |
| 5 | Real detection | Point camera at a **person** → "Person on your left/right/ahead" spoken + Braille + cue; `OBSERVA_MODEL: objects=…` shows person |
| 6 | Person left/right/center | Move across frame → direction matches (cue pans, haptic side matches) |
| 7 | Obstacle ahead | Large central object → urgent hazard + stop haptic |
| 8 | No object / no spam | Empty scene → no repeated chatter; "Path clear" once after a hazard clears |
| 9 | OCR still works | "Read Text" on signage → text spoken; "No readable text found." when none |
| 10 | Braille/TalkBack | status line + hazards appear via live region / Braille display |
| 11 | Audio/haptic cues | distinguishable L/R/forward/urgent |
| 12 | Latency | record last + average ms from "AI detail"; target <100ms inference, <250ms alert |
| 13 | QNN status | "QNN active" only if backends include QNN; else "off". Truthful either way |
| 14 | Fallback honesty | Remove the `.pte`, rebuild → "AI model: unavailable — heuristic fallback"; no fake labels |

## Interpreting QNN
- `LOADED_QNN` requires the model's `forward` to actually run on a QNN backend (lowered with the
  Qualcomm partitioner). A CPU `.pte` shows `LOADED_CPU` even though the delegate library is present.
- Library present ≠ acceleration. Trust the "AI detail" QNN field, which derives from
  `MethodMetadata.getBackends()`.

## Result log
| # | Result | Notes (latency, screenshots, log excerpts) |
|---|---|---|
| 1–14 | | |
