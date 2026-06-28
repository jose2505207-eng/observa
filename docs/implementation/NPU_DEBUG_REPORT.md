# NPU Debug Report

Single source of truth for the NPU release-blocker investigation. Companion to the deep
reverse-engineering matrix in [`QNN_REVERSE_ENGINEERING_LOG.md`](QNN_REVERSE_ENGINEERING_LOG.md).

**Bottom line:** the detector does **not** run on the NPU on the retail Galaxy S25 Ultra. It is **not**
falling back to GPU — there is **no GPU/Vulkan path in the app at all**; the only fallback is **XNNPACK
CPU**, clearly labeled. The QNN/NPU path is fully built and the `.pte` loads, but HTP device-handle
creation is refused by the locked retail DSP (`skel 4000` / `device_handle 14001`). This is proven
model-independent and reproduced across two QNN stacks. NPU needs a signed/privileged build.

## What this session added (in-app diagnostics)

- **`inference/` package** — `BackendKind`, `BackendStage`, `BackendAttempt`, `BackendDiagnostics`,
  `BackendSelector`. Every backend attempt is recorded per stage (ASSET_CHECK → LIB_LOAD → MODEL_LOAD →
  BACKEND_INIT → WARMUP_FORWARD → FIRST_REAL_FORWARD → ACTIVE / FALLBACK) and logged under the single
  grep tag **`OBSERVA_NPU`** as `attempt=<backend> stage=<stage> success=<bool> detail=<msg>`.
- **`ui/NpuDebugScreen.kt`** — a visible, TalkBack-readable NPU Debug screen reachable from the
  **NPU Debug** button (in the Debug card) and the **"Open NPU debug"** TalkBack custom action. Shows
  build/device identity, backend priority, per-stage attempts, the exact blocker, and the QNN `.pte`
  checksum. Controls: **Force QNN Attempt** (re-runs init + warm-up), **Copy Debug Report**, **GPU
  Fallback: Disabled/Allowed** toggle. Accessible summary node: *"Backend: XNNPACK CPU. Fell back from
  QNN at WARMUP_FORWARD: skel 4000 / device_handle 14001."*
- **Honesty invariant:** `BackendDiagnostics.npuActive()` is true only after a `WARMUP_FORWARD` + `ACTIVE`
  success on a QNN backend. XNNPACK reaching ACTIVE is reported as CPU, never NPU. Unit-tested
  (`BackendDiagnosticsTest`).

## Backend reality (verified from current code + APK)

- **No GPU/Vulkan/LiteRT path is compiled in** — `grep -RniE "Vulkan|GpuDelegate|LiteRT|TFLite"` over
  `app/src/main/java` returns nothing. Fallback chain is exactly **ExecuTorch QNN → XNNPACK CPU**.
- Backend priority (`BackendSelector`): ExecuTorch QNN/NPU → LiteRT QNN/NPU → (GPU disabled) → XNNPACK
  CPU. `disableGpuFallback` defaults **true**.

## Experiment matrix (this build) — see log below for fresh device evidence

| Exp | Backend | Model | Result |
|---|---|---|---|
| A | ExecuTorch QNN, app skel + vendor `ADSP_LIBRARY_PATH` | YOLOv8n raw-head | `WARMUP_FORWARD` fails: `skel 4000` / `device_handle 14001` → fall back |
| D | ExecuTorch QNN | tiny Conv2d→ReLU (prior turn) | **identical** `skel 4000` — model-independent |
| E | LiteRT Qualcomm delegate (prior turn) | tiny probe | **identical** `skel 4000` — independent stack confirmation |
| — | XNNPACK CPU | YOLOv8n decoded-head | **ACTIVE**, real detections, ~16–32 ms |

Decision (per the matrix): tiny QNN model fails identically to YOLO → not an export/parser problem;
LiteRT fails identically → not an ExecuTorch problem; both die at HTP skel/device_handle → **root cause
is device/OS QNN protection-domain access**.

## External requirement to ever reach NPU (any one)
Signed PD / OEM-allowlisted HTP skel · platform-signed privileged app in a vendor SELinux domain ·
engineering/`userdebug` firmware (or unlocked bootloader) · Qualcomm AI Hub / AI Engine Direct
supported deployment · or a device whose policy permits third-party unsigned-PD HTP.

---

## Investigation log

### LOOP 1 — diagnostics + debug menu added; fresh device capture
- **Hypothesis:** the app falls back to GPU (per the report) OR to CPU; capture exactly which and why.
- **Code changed:** `inference/` package; `runtime/ExecuTorchDetector.kt` records `BackendAttempt`s;
  `ObservaController` exposes `npuDebugLines()/npuDebugReport()/forceQnnAttempt()/npuActive`;
  `ui/NpuDebugScreen.kt` + routing + NPU Debug button + "Open NPU debug" TalkBack action.
- **APK built:** demoOffline + provisioning debug (both signed; demoOffline no INTERNET). **Device
  installed:** demoOffline on S25 Ultra (`R3CXC08009D`).
- **Log evidence (captured 2026-06-29, `OBSERVA_NPU` tag):**
  ```
  OBSERVA_NPU attempt=EXECUTORCH_QNN stage=LIB_LOAD       success=true  detail=QNN delegate present (not active)
  OBSERVA_NPU attempt=EXECUTORCH_QNN stage=ASSET_CHECK    success=true
  OBSERVA_NPU attempt=EXECUTORCH_QNN stage=MODEL_LOAD     success=true  detail=loaded 6725KB, backends=[QnnBackend]
  OBSERVA_NPU attempt=EXECUTORCH_QNN stage=WARMUP_FORWARD success=false detail=[ExecuTorch Error 0x1] Execution failed for method: forward
  OBSERVA_NPU attempt=EXECUTORCH_QNN stage=FALLBACK       success=true  detail=falling back to next candidate
  OBSERVA_NPU attempt=XNNPACK_CPU    stage=MODEL_LOAD     success=true  detail=loaded 12408KB, backends=[XnnpackBackend]
  OBSERVA_NPU attempt=XNNPACK_CPU    stage=WARMUP_FORWARD success=true
  OBSERVA_NPU attempt=XNNPACK_CPU    stage=ACTIVE         success=true  detail=backends=[XnnpackBackend]
  ```
  Adjacent native lines in the same logcat (same pid), the actual blocker:
  ```
  [Qnn ExecuTorch] QnnDsp <E> Failed to load skel, error: 4000
  [Qnn ExecuTorch] Failed to create device_handle for Backend ID 6, error=14001
  ```
  Then `OBSERVA_MODEL inference 23ms (avg 32ms) ... backends=[XnnpackBackend]` — detector running on CPU.
- **Conclusion:** not GPU — there is no GPU path; the QNN `.pte` **loads** (`backends=[QnnBackend]`) but
  the warm-up forward fails at HTP device-handle creation (`skel 4000` / `device_handle 14001`); fallback
  is **XNNPACK CPU**, which reaches ACTIVE. `npuActive=false`. Consistent with the proven Outcome B. The
  in-app native-log read (`Runtime.exec("logcat")`) returns empty because `untrusted_app` is blocked from
  the log device on this locked build — so the native skel line must be captured over `adb logcat`; the
  NPU Debug screen says exactly this.
- **Next action:** none on NPU (blocked); keep diagnostics + honest fallback. Do not tag NPU-complete.
