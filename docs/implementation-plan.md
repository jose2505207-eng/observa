# OBSERVA Implementation Plan (P0 Demo)

Maps the P0 demo slice to roles. Source of truth: `docs/wiki-ready/` (esp. Product-Requirements, Technical-Architecture, Validation-Gates). Truthfulness rule: every UI label and doc states what is real vs simulated.

## Target slice
Camera frame stream → frame count / FPS → detector (Demo script **or** brightness heuristic) → HazardEngine (cooldown + scene memory) → spoken + haptic alert → accessible dashboard → Demo Mode → offline/privacy status → buildable APK.

## Role mapping

| Role | Deliverable | Files | Verifies |
|------|-------------|-------|----------|
| Product Captain | Keep scope to P0 spine; truthful labels | this plan, docs | REQ-001/006/008 |
| Mobile Developer | CameraX preview, permission/lifecycle safety, frame count, FPS, single Compose screen | `MainActivity.kt`, `ObservaController.kt`, `ui/` | Gate A, C |
| Accessibility UX | High-contrast large UI, TalkBack labels, TTS, mute toggle, no spam | `ui/ObservaScreen.kt`, `output/Speaker.kt` | Gate G, D |
| Perception Engineer | Hazard model (enums), HazardEngine, cooldown, scene memory | `hazard/Models.kt`, `hazard/HazardEngine.kt` | Gate D |
| AI Runtime Engineer | `VisionRuntime` abstraction + Heuristic + Demo + ExecuTorch stub (truthful status) | `runtime/*` | REQ-007 |
| Performance Engineer | Dashboard: camera/frames/FPS/backend/cooldown/demo/privacy | `ui/ObservaScreen.kt` | Gate F |
| Demo Lead | Deterministic Demo Mode timeline through the same engine | `demo/DemoScript.kt` | Demo Loop |
| Documentation Lead | README, current-status, demo-script, this plan | `docs/`, `README.md` | Gate H |
| Release Manager | Small staged commits, build-gated pushes via git-auto-push | git | Gate A, I |

## Architecture (this slice)
```
CameraX ImageAnalysis (bg executor)
  → luminance thirds → FrameInput  (+ frameCount, FPS)
        │
   [Demo Mode?]──yes─→ DemoVisionRuntime (scripted timeline)
        │no
        └────────────→ HeuristicVisionRuntime (brightness, on-device)
                            → List<Detection>
                                → HazardEngine.process (map + cooldown + scene memory)
                                    → List<Hazard>
                                        → Speaker (TTS) + Haptics (Vibrator) + UI alert log
```
`ExecuTorchVisionRuntime` exists as a truthful **stub** (status: bundled, not invoked) and is shown in the dashboard but not used for inference.

## Honesty ledger (kept current in `docs/current-status.md`)
- **Real:** camera loop, frame count, FPS, brightness heuristic, hazard engine + cooldown/scene memory, TTS, haptics, dashboard, demo mode, offline (no network code).
- **Simulated:** Demo Mode hazard events (labeled "Demo"); brightness heuristic is a proxy, **not** ML detection.
- **Not done:** real ExecuTorch/QNN/NPU inference, OCR, voice input.

## Build order (Phase 3)
build ok → launch → preview+frames → dashboard → hazard model → demo generator → engine suppression → TTS → haptics → demo/privacy UI → TalkBack → docs → APK → demo rehearsal.
