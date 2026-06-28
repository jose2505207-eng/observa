# OBSERVA — Current-State Audit (by priority)

Verified 2026-06-27 on the target device **Galaxy S25 Ultra (SM-S938U1 · Snapdragon 8 Elite SM8750
· arm64-v8a)**, serial `R3CXC08009D`, debug build, **Airplane Mode**. Build + unit tests green; the
merged manifest has **no `INTERNET` permission**.

## P1 — Local AI model  ✅ gate met
- **Real local inference runs.** YOLOv8n→ExecuTorch (`assets/models/observa_detector.pte`, 320×320,
  XNNPACK delegate). On device: load ~11 ms, `forward backends=[XnnpackBackend]`, output `[1,84,2100]`.
- **Latency:** median ~32 ms (p95 ~58 ms) — under the 100 ms target. Offline (airplane mode).
- Camera→YUV→RGB→tensor→forward→parse→hazard pipeline is wired (`ObservaScreen` analyzer →
  `ExecuTorchDetector` → `YoloDetectionParser` → `HazardEngine` → `AccessibilityOutputRouter`).
- Detector/runtime isolated behind `InferenceEngine`/`VisionRuntime`; off-UI-thread inference.
- **Gaps:** QNN/NPU not active (XNNPACK CPU; QNN documented + attemptable). Doors/stairs/curbs not in
  COCO-80. Detail: `MODEL_RUNTIME.md`.

## P2 — Braille / native accessibility  ✅ substantially met
- `AccessibilityOutputRouter` fans every event to TTS + Braille/TalkBack live region + repeat store +
  audio/haptic cues; non-urgent duplicates debounced (`OutputThrottler`, 1.2 s) so braille/TalkBack
  are not frame-spammed; **HAZARD events bypass the debounce and barge-in**.
- Concise, stable status (`BrailleStatusPresenter`): "Observa active" / hazard line / mode — short,
  braille-friendly. `contentDescription`/`testTag` on controls; polite + assertive live regions.
- **Non-visual operation paths:** offline voice commands (whole app) + physical volume-rocker hotkeys
  (off by default) — so the app is operable without sight or on-screen buttons.
- **Gaps:** no Compose `CustomAccessibilityAction`/`stateDescription` (actions are exposed via
  focusable labeled buttons + voice + hotkeys instead); physical refreshable-braille-display hardware
  not yet tested (app-level live region is exposed). See `BRAILLE_ACCESSIBILITY.md`.

## P3 — Performance / battery  ✅ substantially met
- `ImageAnalysis` `STRATEGY_KEEP_ONLY_LATEST` (no queue buildup); RGB capture only while a model is
  loaded; inference off the UI thread; per-frame timing logged (`OBSERVA_MODEL`).
- Adaptive duty cycle from real battery + `PowerManager` thermal status (`BatteryThermalPolicy`,
  unit-tested); OCR/voice models stay unloaded until requested (Tier-1 detector independent).
- Cue/alert throttling, cooldowns, scene memory (`HazardEngine`, `CueThrottler`).
- **Gaps:** `ExecuTorchDetector.preprocess` allocates a fresh float buffer per inference (buffer reuse
  is a future micro-opt; immaterial at 32 ms / duty-cycled). Battery mAh + thermal soak not stress-
  measured. See `PERFORMANCE_METRICS.md`.

## P4 — UX / audio / haptics / voice  ✅ substantially met
- Modes: always-on awareness, OCR (on demand), pause/silence, repeat-last, navigation, translation
  shell. Stereo-panned audio cues + directional/severity haptics (`SpatialCueEngine`).
- Physical input map (`HotkeySequencer`, ≤4 counted presses, ≤3 honored per mission's "no more than
  three clicks" where mapped) + hold-to-talk voice. See `UX_INPUT_MAP.md`.
- **Gaps:** translation mode has no on-device translation model yet (shell only, never cloud);
  scene-question/VLM not bundled.

## Cross-cutting honesty
- No networking code; `INTERNET`/`ACCESS_NETWORK_STATE` stripped at manifest merge (`tools:node=remove`),
  verified absent in the merged manifest. UI never claims acceleration/recognition it can't prove.
