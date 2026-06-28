# Offline Maps / Navigation Mode

## v2.5.0 — visible Download Map + offline provisioning

There is now a visible **Download Map** button and a Map Download screen.

**How to use:**
- **Install Demo Map Pack** (works in **any** build, no network): writes an OBSERVA waypoint bundle to
  `filesDir/map_packs/demo.map` (header `OBSERVA_MAP_V1`). Navigation then reports "Map ready offline"
  and uses the offline map context. Honestly labeled **"Demo offline map pack"** — waypoints + metadata,
  not full street tiles.
- **Download Area Map** (real regional map): gated to the **provisioning** build (INTERNET). The
  demoOffline build states this honestly instead of pretending.
- A pack is **"Ready offline" only after `MapPackVerifier` passes** (non-empty, readable, recognized
  header — OBSERVA demo or Mapsforge magic). Delete is supported.

**Two flavors** (privacy split): `demoOffline` (default, no INTERNET) vs `provisioning` (`-setup`, adds
INTERNET). Same applicationId, so a pack provisioned by the setup build persists for an `adb install -r`
of the offline build.

**Code:** `maps/MapPackStatus.kt`, `maps/MapRegion.kt`, `maps/MapPackVerifier.kt`,
`maps/MapPackRepository.kt`, `maps/MapDownloadController.kt`, `ui/MapDownloadScreen.kt`. Navigation reads
the same `map_packs/` dir via `navigation/OfflineMapPackManager`.

---

# Offline Maps / Navigation Mode (compass-bearing guidance)

**Status: Navigation Mode is real and usable today with compass + GPS bearing guidance. The rendered
offline-map / route layer is an honest readiness gate — no map pack is bundled in the no-INTERNET
runtime build.**

## What Navigation Mode does today (no map pack needed)
Navigation Mode (the **Navigate** button / "Navigate" accessibility action / swipe-down gesture) runs
real **GPS Orientation Lite**: device GPS (`LocationManager`, satellite, offline) + the compass
(rotation vector) → **heading, bearing, distance, and a six-way relative direction** (ahead, ahead-left,
left, ahead-right, right, behind) with honest confidence (good / weak GPS / compass unstable). It speaks
e.g. *"Destination ahead-left, 40 meters."* This needs **no map pack** — it works on bearing alone.

It is **not** turn-by-turn street routing. We never call it turn-by-turn unless real route data exists
(none is bundled).

## The optional offline map pack
`OfflineMapPackManager` + `OfflineMapRepository` track a local map file under
`filesDir/map_packs/<area>.map` (Mapsforge `.map`, or a MapLibre offline region). Status is one of:

| Status | Meaning |
|---|---|
| `Map pack not installed` | no `map_packs/` dir |
| `Map pack download required` | dir exists, no `.map` file |
| `Map pack corrupt` | a `.map` file is zero-byte/unreadable |
| `Map ready offline` | a readable `.map` is present |

`NavigationModeController` composes the orientation guidance with this status into one short line, e.g.
*"Destination ahead-left, 40 meters. Map pack missing."* The map pack only adds the rendered-map/route
layer; guidance works without it.

## Provisioning a map pack (offline-after-install)
Runtime stays offline; a pack may need a one-time download. Sideload a `.map` into the app's files dir:
```
adb shell mkdir -p /data/data/com.observa.app/files/map_packs
adb push seoul.map /data/data/com.observa.app/files/map_packs/seoul.map
```
or use a separate provisioning/dev flavor with INTERNET (the demo/runtime app keeps **no INTERNET**).
Restart the app → status flips to "Map ready offline". No runtime code or permission change needed.

## Safety priority (hazards always win)
Object/hazard detection keeps running during Navigation Mode. Hazard speech **interrupts** navigation
guidance: the output router enforces `HAZARD > NAVIGATION > OCR > MODE > INFO`, and
`NavigationSafetyArbiter` additionally suppresses guidance for a hold window after a hazard. Navigation
guidance is rate-limited; hazard guidance is immediate. Example: navigation would say "ahead-left" but
the detector sees a person ahead → "Person ahead" is spoken first.

## Street signs / text while navigating
`StreetSignTracker` is a pure stability gate: it only triggers a single OCR read after a sign/text-like
candidate is stable for N frames (then a cooldown), so OCR never runs every frame and never blocks
hazard detection. No dedicated street-sign **detector** is bundled (YOLO COCO-80 has no street-name
class), so auto-trigger stays inactive and the honest path is the **Read Signs** button, which runs one
real ML Kit OCR pass and speaks *"Sign text: …"* or *"No readable sign text."* — never fabricated.

## Code map
| Concern | Where |
|---|---|
| Navigation Mode seam (guidance + map status) | `navigation/NavigationModeController.kt` |
| Map pack status | `navigation/OfflineMapPackManager.kt` |
| Map file data layer | `navigation/OfflineMapRepository.kt` |
| Route vs bearing guidance (honest) | `navigation/RouteGuidanceEngine.kt` |
| Bearing/distance/relative direction | `navigation/BearingCalculator.kt` + `nav/Geo` |
| GPS / compass | `navigation/LocationProvider.kt` · `navigation/CompassProvider.kt` |
| Hazard-beats-navigation | `navigation/NavigationSafetyArbiter.kt` |
| Sign-read gating | `navigation/StreetSignTracker.kt` |

## Tests
`NavigationModulesTest` (RouteGuidanceEngine bearing fallback, NavigationSafetyArbiter hazard priority,
StreetSignTracker stability + no-candidate), `OrientationControllerTest`, `OrientationGuidanceTest`.
