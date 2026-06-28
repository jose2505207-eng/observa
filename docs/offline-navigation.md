# Offline navigation (v1.7)

OBSERVA's navigation is **guidance-first**, not a sighted map. It speaks heading-relative,
clock-face instructions toward a saved destination, fully offline, and always yields to hazards.

## What it is (and isn't)
- **Is:** bearing-to-destination guidance — "Continue straight, 20 meters", "Slight right, 1 o'clock.
  15 meters", "Destination ahead", with honest GPS/compass uncertainty.
- **Is not:** street-level turn-by-turn routing. There is no road graph; guidance points toward the
  destination's bearing. This is documented and intentional for the guidance-first model.

## Components
- `Geo` — haversine distance + initial bearing (pure).
- `RelativeDirectionTranslator` — (route bearing − device heading) → clock-face (pre-existing).
- `GuidanceEngine` — combines them into `NavGuidance` with distance + uncertainty warnings.
- `SavedDestination` / `DestinationStore` — offline saved places (demo set preloaded).
- `NavigationSession` — decides *when* to speak (throttled; re-announces on direction change /
  interval / arrival).
- `NavFixProvider` → `SensorNavFixProvider` — **real device compass** (rotation-vector sensor,
  offline) for heading; **location is a documented demo fix** (this build wires no live GPS and
  requests no `ACCESS_FINE_LOCATION`). `sourceLabel = "demo location, live compass"`, announced when
  navigation starts. GPS is reported `LOW` so guidance always appends the caution note.

## Output & priority
Guidance routes through the unified `AccessibilityOutputRouter` at **NAVIGATION** priority → TTS +
Braille/live-region + repeat store. Because `HAZARD > NAVIGATION`, any safety hazard interrupts
navigation guidance (enforced by the router / `OutputArbiter`, unit-tested).

## Voice + UI
- Voice: "navigate to <place>", "stop navigation", "where am I", "repeat".
- UI: a "Navigation (offline)" panel with large, TalkBack-labeled **Go: <place>** buttons,
  **Where am I**, and **Stop nav** (disabled when idle). `testTag("navPanel")`.

## Honest limitations / blockers
- **No live GPS** in this build: navigation uses a fixed demo start point + the real compass, so
  clock-face guidance is demonstrable but absolute position is not real. Wiring a live
  `LocationManager`/Fused provider (+ `ACCESS_FINE_LOCATION`, runtime request, offline-validation)
  is the remaining device step.
- No map packs / road graph yet (`MapPackManager` is a future abstraction); distances are
  straight-line.
- Compass needs calibration; low accuracy is announced ("Compass accuracy low. Calibrate phone.").

## Device evidence (S25 Ultra, airplane mode)
Selecting a destination speaks guidance via TTS + Braille status. Verified: "Destination ahead.
home." when the demo start equals the target; directional clock-face + distance for other targets
(unit-tested). No INTERNET permission; no location permission required for the demo provider.
