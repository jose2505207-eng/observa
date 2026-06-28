# Performance Metrics (Agent 3) — pointer

Canonical, continuously-updated metrics live in [`docs/PERFORMANCE_METRICS.md`](../PERFORMANCE_METRICS.md).

Headline (Galaxy S25 Ultra · SM8750 · Airplane Mode): model `forward` **median ~32 ms, p90 49 ms,
p95 58 ms** (YOLOv8n@320, XNNPACK delegate); load ~11 ms; danger-recognition **< 100 ms target met**.
Pipeline/battery design: `STRATEGY_KEEP_ONLY_LATEST`, off-UI-thread inference, adaptive battery/
thermal duty cycle (`BatteryThermalPolicy`), on-demand OCR/voice unloaded when idle.
