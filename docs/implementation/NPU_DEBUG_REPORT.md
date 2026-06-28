# NPU Debug Report

Single source of truth for the NPU release-blocker investigation. Companion to the deep
reverse-engineering matrix in [`QNN_REVERSE_ENGINEERING_LOG.md`](QNN_REVERSE_ENGINEERING_LOG.md).

## ✅ RESOLVED 2026-06-29 — the detector now runs on the NPU

**The NPU works on the retail Galaxy S25 Ultra.** The blocker was **not** a signing / protection-domain
restriction (my earlier conclusion was wrong) — it was the **Android 12+ vendor-native-library access
rule**: the app could not `dlopen` the vendor `libcdsprpc.so` FastRPC client, so QNN HTP failed to
create the cDSP transport (`Failed to create transport for device, error: 4000` → `Failed to load skel,
error: 4000`). **The fix is one manifest line:**

```xml
<uses-native-library android:name="libcdsprpc.so" android:required="false" />
```

(Credit: github.com/psiddh/executorch pr-20057 `examples/qualcomm/qnn-htp-test`.)

**Device-verified after the fix** (fresh logcat, demoOffline build, no INTERNET):
```
OBSERVA_NPU attempt=EXECUTORCH_QNN stage=WARMUP_FORWARD success=true
OBSERVA_NPU attempt=EXECUTORCH_QNN stage=ACTIVE         success=true detail=backends=[QnnBackend]
adsprpc: Successfully opened .../lib/arm64/libQnnHtpV79Skel.so
adsprpc: remote_handle64_open ... libQnnHtpV79Skel.so?qnn_skel_handle_invoke ... on domain 3 (cDSP)
OBSERVA_EXECUTORCH: ... forward backends=[QnnBackend]; parser=yolo-raw-head; QNN/NPU ACTIVE.
OBSERVA_MODEL: inference 2ms (avg 2ms); outputs=[[1,144,40,40],[1,144,20,20],[1,144,10,10]] parser=yolo-raw-head
```
The v79 skel loads on the cDSP, the warm-up forward succeeds, and the YOLOv8n detector runs on the NPU
at **~2–3 ms** (vs ~22–32 ms on XNNPACK CPU — ~10–15×). `npuActive()` is true (set only after the real
warm-up). No INTERNET. XNNPACK CPU is retained as the automatic fallback on devices that lack the
library (`required="false"`).

> The investigation history below is preserved for honesty. It correctly captured the *symptom*
> (`skel 4000`) and proved it was model-independent and cross-stack, but it mis-attributed the *cause*
> to a protection-domain/signing block. The real cause was the missing `libcdsprpc.so` declaration.

---

## (Superseded) earlier bottom line

The detector does not run on the NPU; the QNN `.pte` loads, but HTP device-handle creation fails
(`skel 4000` / `device_handle 14001`). — **Now resolved; see above.** It was never a GPU fallback —
there is no GPU path; the only fallback is XNNPACK CPU.

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
