---
status: current
confidence: high
last_updated: 2026-06-29
owner: jose2505207-eng
---

# Offline Navigation & Maps

> Tells a blind/low-vision user **which way to face and how far** to a destination — fully offline,
> with a visible **Navigate** button, a **Download Map** button, and TalkBack/braille actions. Shipped
> in **v2.5.0**.

## Current Reality (device-verified)
- **Navigation Mode** = real device GPS (`navigation/LocationProvider`, `LocationManager`, satellite,
  offline) + real compass (`navigation/CompassProvider`, rotation vector) → **heading, bearing,
  distance, and a six-way relative direction** (ahead / ahead-left / left / ahead-right / right /
  behind) with honest confidence (good / weak GPS / compass unstable). Speaks e.g. *"Destination
  ahead-left, 40 meters."* Works with **no map pack** (bearing guidance only). `navigation/`:
  `NavigationModeController`, `BearingCalculator`, `RouteGuidanceEngine`, `OrientationGuidanceEngine`,
  `OrientationController`, `DestinationStore`, `NavigationSafetyArbiter`.
- **Offline map packs** (`maps/` package + `ui/MapDownloadScreen`): visible **Download Map** →
  **Install Demo Map Pack** writes an offline OBSERVA waypoint bundle to `filesDir/map_packs/demo.map`
  (no network; honestly labeled "Demo offline map pack"). Status: not installed / downloading / ready
  offline / corrupt / failed. "Ready offline" only after `MapPackVerifier` passes.
- **Download Area Map (v3.1.0):** `MapDownloadController.downloadAreaMap(lat,lon)` pulls the **real
  named places around you** from the OSM **Overpass API** (provisioning/INTERNET only), stores them
  offline as map-pack waypoints, and loads them as navigation destinations
  (`OfflineMapRepository.places()`). Real place data — **not rendered tiles**, labeled honestly — and
  it works offline after the one-time download.
- **Navigation haptics (v3.1.0):** `SpatialCueEngine.navDirection` + `OrientationGuidance.direction` →
  left/right **turn pulses**, a **forward buzz** when aligned, and an **arrival pattern**. Object
  detection runs concurrently; hazards still interrupt.
- **Safety:** object/hazard detection keeps running during navigation; hazards **interrupt** navigation
  speech (router priority `HAZARD > NAVIGATION > …` + `NavigationSafetyArbiter` hold window). Navigation
  guidance is rate-limited; hazards are immediate. Street signs/text: `StreetSignTracker` gates OCR by
  frame stability; **Read Signs** runs one real ML Kit OCR pass (never fabricated).

## Honesty / not built
- **Not** full turn-by-turn street routing — labeled "orientation/bearing guidance" unless real route
  data exists (none bundled). `RouteGuidanceEngine` falls back to bearing and never claims a route.
- A real regional map download needs a one-time connection → the `provisioning` flavor (INTERNET); the
  default `demoOffline` flavor has **no INTERNET** and uses preloaded/sideloaded packs.

## Related
- [[offline-first-design]] · [[spatial-guidance]] · [[ambient-awareness]] · [[offline-translation]]
- Detail: `docs/implementation/OFFLINE_MAPS.md`, `docs/implementation/GPS_ORIENTATION.md`
