# On-device models

Drop ExecuTorch model files here. They are bundled into the APK and loaded
**locally** — OBSERVA never downloads models or sends frames anywhere.

Expected file (validated by `ExecuTorchDetector`):

- `observa_detector.pte` — object/hazard detector exported for ExecuTorch.

Until a real `.pte` is present, `ExecuTorchDetector` reports
`NOT_CONNECTED`, the app runs on the on-device brightness heuristic
fallback, and the dashboard says so honestly. When a model is added,
`QnnRuntimeChecker` reports whether Qualcomm QNN (NPU) acceleration
libraries are present; absence means CPU fallback.

No model is committed yet, so no acceleration or ML detection is claimed.
