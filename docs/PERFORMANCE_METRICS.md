# Performance metrics

Honest measurement status. Numbers are either measured on device or marked **not measured** with a
reason — never fabricated.

## Measured (Galaxy S25 Ultra · SM8750 Snapdragon 8 Elite · Airplane Mode)
| Metric | Value | Notes |
|---|---|---|
| **Model inference (`forward`)** | **median ~32 ms** (range 26–43 ms) | YOLOv8n@320, **XNNPACK** delegate; `OBSERVA_MODEL` logcat `inference Nms (avg Mms)` |
| **Danger-recognition < 100 ms target** | **MET** (~32 ms) | on CPU (XNNPACK); no NPU required to hit target |
| Model load (one-time) | ~11 ms | `OBSERVA_EXECUTORCH` `load success in 11ms` |
| Inference delegate | `forward backends=[XnnpackBackend]` | confirmed at load; QNN reported off (honest) |
| Portable-kernels baseline | ~4490 ms | same model, no delegate — XNNPACK is ~140× faster |
| Camera analyzer FPS | ~25–32 FPS | dashboard "FPS"; `KEEP_ONLY_LATEST`, YUV_420_888 |
| App launch → camera live | a few seconds | qualitative; no crash |
| Offline OCR (on demand) | sub-second qualitative | ML Kit bundled model; not yet timed precisely |
| Foreground service | stable, `isForeground=true` | notification persistent |

## Not measured (with reason)
| Metric | Status | Reason |
|---|---|---|
| Hazard alert latency < 250 ms target | **partially** | inference ~32 ms + parse/engine/route are sub-ms in unit tests; not yet stopwatch-timed camera-frame→audio end-to-end on device. |
| QNN vs CPU latency | **not measured** | QNN not active (model is XNNPACK CPU). QNN SDK present; lowering documented in `docs/implementation/MODEL_RUNTIME.md`. |
| Battery drain / mAh | **not measured** | needs a sustained measurement harness; adaptive duty cycle implemented + unit-tested (`BatteryThermalPolicy`). |
| Thermal behavior under load | **not measured** | duty cycle backs off on `PowerManager` thermal status (unit-tested); not stress-tested on device. |
| Memory footprint | **not measured** | profiling harness is future work (`wiki/engineering/profiling-plan.md`). |

## Reproduce these numbers
1. `python scripts/export_detector.py --imgsz 320` (XNNPACK delegate, default).
2. `./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk`.
3. Launch, point camera around; `adb logcat -s OBSERVA_MODEL OBSERVA_EXECUTORCH` → read
   `load success in <ms>` and per-frame `inference <ms> (avg <ms>)`; dashboard "AI detail" mirrors it.
