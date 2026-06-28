# GPS Orientation Lite

**Status: implemented, unit-tested, surfaced in the accessibility layer; live on-device GPS guidance
pending an outdoor TalkBack pass.** Offline by construction.

## What it is (and is NOT)
GPS Orientation Lite tells a blind/low-vision user **which way to face and how far** to a destination.
It is **not** turn-by-turn street navigation and **not** offline maps. It provides:
- current heading (device compass / rotation-vector sensor),
- bearing to the destination + approximate distance,
- relative direction in a six-way vocabulary: **ahead, ahead-left (slight left), left, ahead-right
  (slight right), right, behind**, plus a turn hint ("Turn slightly right."),
- an honest confidence state: **good / weak GPS / compass unstable**.

Example spoken lines: *"Destination ahead-left, 40 meters."* · *"Turn slightly right."* ·
*"GPS signal weak."*

## Offline guarantee
Location comes from the Android `LocationManager` **GPS/network providers directly — no Google Play
Services, no INTERNET**. GPS is satellite-based and works in Airplane Mode. The app keeps
`INTERNET`/`ACCESS_NETWORK_STATE` stripped at manifest-merge time; only `ACCESS_FINE_LOCATION` /
`ACCESS_COARSE_LOCATION` were added (location ≠ network).

## Code map (`navigation/` package; reuses `nav/` great-circle math — no duplication)
| Concern | Where |
|---|---|
| Real device location | `navigation/LocationProvider.kt` (`LocationManager`, last-known seed + updates, `GpsAccuracy`) |
| Device compass heading | `navigation/CompassProvider.kt` (rotation-vector sensor → heading + `HeadingAccuracy`; offline) |
| Bearing/distance + six-way relative direction | `navigation/BearingCalculator.kt` (delegates `nav/Geo` math) |
| Guidance text + confidence (good/weak GPS/compass unstable) | `navigation/OrientationGuidanceEngine.kt` |
| Active destination (configurable demo target) | `navigation/DestinationStore.kt` (single target; `setDestination(...)`) |
| Hazard-beats-navigation rule | `navigation/NavigationSafetyArbiter.kt` (suppresses guidance for a window after a hazard) |
| Orchestration | `navigation/OrientationController.kt` — composes the above; rate-limited; honest |
| Location/heading seams for tests | `navigation/LocationSource`, `navigation/HeadingSource` interfaces |

The destination is configurable in code (default: `DestinationStore.DEMO`) and at runtime via
`OrientationController.setDestination(...)` for a debug control.

## Safety priority (hazards always win)
Two layers enforce it:
1. **Router priority** — orientation is emitted through `AccessibilityOutputRouter` at **NAVIGATION**
   priority (`HAZARD > NAVIGATION > OCR > MODE > INFO`), and the processing loop emits vision hazards
   *before* `orientationTick()`.
2. **`NavigationSafetyArbiter`** — additionally suppresses orientation guidance for a hold window
   (default 2.5 s) after the last hazard, so the user is never talked over a safety alert.

Updates are also rate-limited (default 4 s) so a braille display is never flooded.

## Accessibility
- TalkBack/braille custom actions on the operating layer: **Start orientation**, **Repeat
  orientation** (label flips to Repeat when active), **Stop orientation**.
- Current-status node appends the live orientation line when active, e.g.
  *"Awareness active. Detector backend: XNNPACK. OCR ready. Orientation active. Destination ahead-left,
  40 meters"*.
- Honest states: "Orientation needs location permission", "GPS signal weak", "Compass unstable. Move
  the phone in a figure eight."

## Tests
- `OrientationControllerTest` (6): GPS guidance toward destination, no-permission honesty, no-fix
  "GPS signal weak", rate-limiting, **hazard suppresses guidance**, off/repeat — via fake
  `LocationSource` + `HeadingSource`.
- `OrientationGuidanceTest` (7): six-way relative directions, ahead+distance, off-axis turn hint,
  weak-GPS/compass-unstable confidence, arrival, and the `NavigationSafetyArbiter` hold window.

## Pending
Live outdoor GPS guidance through the TalkBack action on the S25 Ultra (indoor fixes are often
`NONE`/`LOW`). The code path is verified by unit tests and the app launches with location granted.
