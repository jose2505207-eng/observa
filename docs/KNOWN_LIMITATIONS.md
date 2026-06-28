# Known limitations (v1.7 → v1.8)

Honest ledger. Nothing here is hidden from the UI or the demo.

## Perception
- **No ML object recognition shipped.** The ExecuTorch + `YoloDetectionParser` path is implemented
  and unit-tested, but no `observa_detector.pte` is bundled (no export toolchain in the build
  environment; YOLOv8 AGPL license decision; `.pte`/AAR runtime version must match). The live
  detector is a **brightness heuristic** (generic obstacle by region) — it never emits semantic
  labels. Reproduce a model via `scripts/export_detector.py` (`docs/real-detector.md`).
- **QNN acceleration is not active.** The delegate library ships in the APK but is only "active" if a
  loaded model's `forward` uses a QNN backend (`MethodMetadata.getBackends()`); with no model, it is
  CPU/none. Reported truthfully as "QNN delegate present (not active)".

## Navigation
- **No live GPS.** Navigation uses a documented **demo location** + the **real device compass**, so
  clock-face guidance is demonstrable but absolute position is not real. No `ACCESS_FINE_LOCATION`,
  no Fused/Location provider yet.
- **No map packs / road graph.** Guidance is straight-line bearing-to-destination, not street
  turn-by-turn (`MapPackManager` is a future abstraction).

## Service / lifecycle
- **No screen-off background camera capture.** The foreground service keeps OBSERVA foregrounded with
  an honest notification + controls and an adaptive duty cycle, but camera + inference still run with
  the Activity; true always-on screen-off capture is future work.

## Accessibility
- **Physical Braille display not verified.** The app exposes concise status via TalkBack polite live
  regions (which drive a TalkBack-connected Braille display), but no hardware display has been tested.
  Direct `BrailleDisplayController` is intentionally deferred (`docs/accessibility/braille-support.md`).
- **Full human sensory pass pending** (audio panning audibility, haptic distinguishability, TalkBack
  speech, real voice recognition in noise). Checklists in `docs/manual-test-*.md`.

## Measurement
- **No model/latency numbers** (no model to run). FPS (~25–32) is observed; inference latency
  instrumentation exists and will populate once a model is bundled. See `docs/PERFORMANCE_METRICS.md`.

## Platform
- ExecuTorch native libs are arm64-v8a only; on other ABIs model load fails → honest fallback (no
  crash).
