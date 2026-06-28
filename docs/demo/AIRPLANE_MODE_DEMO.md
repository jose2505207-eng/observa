# Airplane-Mode Demo & Offline Proof

Proves OBSERVA runs **real on-device AI inference with no network**, on the Galaxy S25 Ultra
(Snapdragon 8 Elite). Every step below was executed on device `R3CXC08009D` in Airplane Mode.

## One-time setup
```bash
cd observa-hackathon/observa-android
./gradlew assembleDemoOfflineDebug
adb install -r app/build/outputs/apk/demoOffline/debug/app-demoOffline-debug.apk
adb shell pm grant com.observa.app android.permission.CAMERA
```

## Prove offline
```bash
adb shell settings get global airplane_mode_on        # → 1 (airplane mode ON)
# App declares no INTERNET permission (stripped at manifest merge):
adb shell dumpsys package com.observa.app | grep -i internet   # → (no INTERNET line)
```

## Prove the real model is loading + running locally
```bash
adb logcat -c
adb shell am start -n com.observa.app/.MainActivity
adb logcat -s OBSERVA_EXECUTORCH OBSERVA_MODEL
```
Expected (verified):
```
OBSERVA_EXECUTORCH: load success; version=yolov8n-coco imgsz=320 … methods=[forward];
                    forward backends=[QnnBackend]; parser=yolo-raw-head; QNN/NPU ACTIVE.
OBSERVA_NPU: stage=WARMUP_FORWARD success=true → stage=ACTIVE backends=[QnnBackend]
OBSERVA_MODEL: inference 2ms (avg 2ms); outputs=[[1, 84, 2100], …]; objects=N via parser=yolo […]
```
- `forward backends=[QnnBackend]` → the model is really running on the Hexagon NPU on device.
- `inference ~2–3 ms` → ~10–15× faster than the XNNPACK CPU fallback, far under the 100 ms target.
- `objects=0` when nothing recognizable is in frame is **honest** — point the camera at a person or a
  vehicle and `objects` becomes ≥1 with `person`/`car → PERSON/VEHICLE` and a spoken/haptic hazard.

## Latency sample (reproduce the numbers)
```bash
adb logcat -c; adb shell am start -n com.observa.app/.MainActivity; sleep 25
adb logcat -d -s OBSERVA_MODEL | grep -oE 'inference [0-9]+ms'   # collect, then median/p95
```
On the NPU (QnnBackend) a warm run sits at **~2–3 ms** per inference. The XNNPACK CPU fallback path,
for reference, measured min 17 ms · median 38 ms · p95 58 ms · max 70 ms on the same device.

## Live walk-through checklist (presenter)
1. Confirm Airplane Mode is on (status bar).
2. Launch OBSERVA; camera preview goes live; dashboard shows `AI model: loaded — QNN/NPU`.
3. Point at a **person** → "Person ahead/left/right" + directional cue.
4. Point at a **car/bike** → vehicle hazard.
5. Approach an **obstacle filling the center** → urgent central hazard (barge-in, strong haptic).
6. **Double-tap front / volume-up ×3** (if hotkeys on) → OCR reads a sign aloud.
7. **Repeat last** (volume-up ×1) → replays the last alert.
8. Show the dashboard "AI detail": input/output shapes + live `~2 ms` latency + `QNN/NPU active` — the
   proof. The **NPU Data** button shows a live latency/throughput graph.

See also: `docs/implementation/MODEL_RUNTIME.md`, `docs/PERFORMANCE_METRICS.md`, `docs/FINAL_DEMO.md`.
