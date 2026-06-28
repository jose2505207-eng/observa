# Performance metrics

Honest measurement status. Numbers are either measured on device or marked **not measured** with a
reason — never fabricated.

## Measured (Galaxy S25 Ultra, Android 16, Airplane Mode)
| Metric | Value | Notes |
|---|---|---|
| Camera analyzer FPS | ~25–32 FPS | dashboard "FPS"; `KEEP_ONLY_LATEST`, YUV_420_888 |
| App launch → camera live | a few seconds | qualitative; no crash |
| Offline OCR (on demand) | sub-second qualitative | ML Kit bundled model; not yet timed precisely |
| Foreground service | stable, `isForeground=true` | notification persistent |

## Not measured (with reason)
| Metric | Status | Reason |
|---|---|---|
| Model inference latency (last/p50/p95) | **not measured** | No `observa_detector.pte` bundled — no model to run. Instrumentation exists (`ExecuTorchDetector` last/avg ms → dashboard "AI detail" + `OBSERVA_MODEL` logcat) and will populate once a model ships. |
| Detection latency < 100 ms target | **not measured** | same — needs a bundled model. |
| Hazard alert latency < 250 ms target | **partially** | heuristic→alert path is well under this qualitatively; not formally timed. |
| QNN vs CPU latency | **not measured** | QNN not active (no QNN-lowered model). |
| Battery drain / mAh | **not measured** | needs a sustained measurement harness; adaptive duty cycle implemented + unit-tested (`BatteryThermalPolicy`). |
| Thermal behavior under load | **not measured** | duty cycle backs off on `PowerManager` thermal status (unit-tested); not stress-tested on device. |
| Memory footprint | **not measured** | profiling harness is future work (`wiki/engineering/profiling-plan.md`). |

## How to populate once a model is bundled
1. `python scripts/export_detector.py --out app/src/main/assets/models/observa_detector.pte`
2. Install, run; read `OBSERVA_MODEL` logcat (`inference <ms> (avg <ms>)`) and the dashboard "AI detail".
3. Record load ms, last/avg latency, and whether `forward` backends include QNN here.
