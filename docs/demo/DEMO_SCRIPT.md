# Demo Script — pointer

The full, maintained demo script and judge walk-through are in
[`docs/FINAL_DEMO.md`](../FINAL_DEMO.md), [`docs/demo-script.md`](../demo-script.md), and
[`wiki/demo/judge-script.md`](../../wiki/demo/judge-script.md).

For the offline / real-model proof (airplane mode, on-device inference, latency capture), use
[`AIRPLANE_MODE_DEMO.md`](AIRPLANE_MODE_DEMO.md).

30-second pitch: enable Airplane Mode → launch OBSERVA → dashboard shows `AI model: loaded — CPU
(XNNPACK)` with live `~32 ms` inference → point at a person/vehicle/obstacle → short directional
spoken + haptic hazard → volume-up ×1 repeats it → volume-up ×3 reads a sign aloud (OCR). No network,
no cloud, no `INTERNET` permission.
