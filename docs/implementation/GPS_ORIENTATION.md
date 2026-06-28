# GPS Orientation Lite

**Status: implemented, unit-tested, surfaced in the accessibility layer; live on-device GPS guidance
pending an outdoor TalkBack pass.** Offline by construction.

## What it is (and is NOT)
GPS Orientation Lite tells a blind/low-vision user **which way to face and how far** to a destination.
It is **not** turn-by-turn street navigation and **not** offline maps. It provides:
- current heading (device compass / rotation-vector sensor),
- bearing to the destination + approximate distance,
- relative, clock-face direction ("straight ahead", "slight right, 1 o'clock", "turn around"),
- an honest confidence state (good GPS / GPS accuracy low / compass needs calibration).

## Offline guarantee
Location comes from the Android `LocationManager` **GPS/network providers directly — no Google Play
Services, no INTERNET**. GPS is satellite-based and works in Airplane Mode. The app keeps
`INTERNET`/`ACCESS_NETWORK_STATE` stripped at manifest-merge time; only `ACCESS_FINE_LOCATION` /
`ACCESS_COARSE_LOCATION` were added (location ≠ network).

## Code map (reuses the existing `nav/` math — no duplication)
| Concern | Where |
|---|---|
| Real device location (new) | `navigation/LocationProvider.kt` (`LocationManager`, last-known seed + updates, `GpsAccuracy`) |
| Orchestration (new) | `navigation/OrientationController.kt` — combines location + compass heading + guidance; rate-limited; honest |
| Location seam for tests (new) | `navigation/LocationSource` interface |
| Bearing/distance | `nav/Geo.kt` (haversine + initial bearing) — reused |
| Clock-face relative direction | `nav/RelativeDirectionTranslator.kt` — reused |
| Guidance text + confidence warnings | `nav/GuidanceEngine.kt` — reused |
| Heading + heading accuracy | `nav/SensorNavFixProvider.kt` (rotation-vector compass) — reused |
| Destinations (demo set, configurable) | `nav/DestinationStore.kt` / `SavedDestination` — reused |

The destination is configurable in code (default: `DestinationStore.DEMO.first()`); a debug UI hook can
set a custom coordinate.

## Safety priority (hazards always win)
Orientation guidance is emitted through the existing `AccessibilityOutputRouter` at **NAVIGATION**
priority. The router enforces `HAZARD > NAVIGATION > OCR > MODE > INFO`, and the processing loop emits
vision hazards *before* `orientationTick()`, so **a hazard always interrupts orientation**. Updates are
rate-limited (default 4 s) so a braille display is never flooded.

## Accessibility
- TalkBack/braille custom actions on the operating layer: **Start orientation**, **Repeat
  orientation** (label flips to Repeat when active), **Stop orientation**.
- Current-status node appends the live orientation line when active, e.g.
  *"Awareness active. Detector backend: XNNPACK. OCR ready. Orientation active. 1 o'clock · 40m"*.
- Honest states: "Orientation needs location permission", "GPS unavailable", "GPS accuracy low",
  "Compass accuracy low. Calibrate phone."

## Tests
`OrientationControllerTest` (5): GPS guidance toward destination, no-permission honesty, no-fix
"GPS unavailable", rate-limiting, off/repeat. Pure logic via a fake `LocationSource`.

## Pending
Live outdoor GPS guidance through the TalkBack action on the S25 Ultra (indoor fixes are often
`NONE`/`LOW`). The code path is verified by unit tests and the app launches with location granted.
