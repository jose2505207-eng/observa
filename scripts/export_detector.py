#!/usr/bin/env python3
"""
Reproducibly export a YOLOv8n COCO detector to ExecuTorch as observa_detector.pte,
matching the parser OBSERVA ships (YoloDetectionParser).

OUTPUT CONTRACT this produces / OBSERVA expects
-----------------------------------------------
  input :  float32  [1, 3, 640, 640]  RGB, normalized 0..1, NCHW
  output:  float32  [1, 84, 8400]     (cx, cy, w, h in 0..640 pixels) + 80 COCO class probs,
                                       channels-first, no objectness (YOLOv8 head)

OBSERVA's YoloDetectionParser auto-detects [1,84,N] vs [1,N,84] and with/without an
objectness channel, so minor head variations still parse.

VERSION MATCH (important)
-------------------------
A .pte must be produced by an ExecuTorch version compatible with the runtime bundled in
app/libs/executorch.aar. If load fails on device, OBSERVA reports FAILED and keeps the
heuristic fallback (it never crashes). Pin executorch to the AAR's version; if unknown,
try the latest stable and verify on device (see docs/real-detector.md).

USAGE
-----
  python3 -m venv .venv && source .venv/bin/activate
  pip install "executorch==<match-aar>" ultralytics torch
  python scripts/export_detector.py --out app/src/main/assets/models/observa_detector.pte

LICENSE NOTE
------------
YOLOv8 (Ultralytics) is AGPL-3.0. Confirm license compatibility before bundling its weights
in a distributed app. For a permissive alternative, export an SSD-MobileNetV2 / NanoDet model
and add a matching DetectionParser instead (see docs/real-detector.md).
"""
import argparse
import os
import sys


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="app/src/main/assets/models/observa_detector.pte")
    ap.add_argument("--weights", default="yolov8n.pt")
    ap.add_argument("--imgsz", type=int, default=640)
    ap.add_argument("--qnn", action="store_true", help="lower to the Qualcomm QNN backend (NPU/HTP)")
    ap.add_argument("--qnn-raw-head", action="store_true",
                    help="QNN export returning the RAW multi-scale YOLOv8 head (pre-anchor-decode). "
                         "This avoids make_anchors / int64 grids / the I64toI32 'expand 3->1' failure; "
                         "the decode + NMS are done on Android in YoloRawHeadParser. Implies --qnn.")
    ap.add_argument("--no-xnnpack", action="store_true",
                    help="disable the XNNPACK CPU delegate (export slow portable kernels)")
    args = ap.parse_args()
    if args.qnn_raw_head:
        args.qnn = True

    try:
        import torch
        from ultralytics import YOLO
        from executorch.exir import to_edge, to_edge_transform_and_lower
    except Exception as e:  # pragma: no cover - tooling guidance only
        print(f"[export] missing tooling: {e}\n"
              f"[export] install: pip install executorch ultralytics torch", file=sys.stderr)
        return 2

    print(f"[export] loading {args.weights}")
    model = YOLO(args.weights).model.eval()

    if args.qnn_raw_head:
        # Replace the Detect head's forward with one that returns the raw per-scale tensors
        # (cat(box-DFL, class-logits)) BEFORE anchor decode/DFL/NMS. Those decode ops use int64
        # anchor grids (make_anchors) that break QNN's I64toI32 pass; doing them on-device instead
        # keeps the exported graph pure conv/silu/concat → fully QNN/HTP-lowerable. The matching
        # decoder is app/.../runtime/YoloRawHeadParser.kt.
        import types
        det = model.model[-1]
        def _raw_forward(self, x):
            return tuple(torch.cat((self.cv2[i](x[i]), self.cv3[i](x[i])), 1) for i in range(self.nl))
        det.forward = types.MethodType(_raw_forward, det)
        print(f"[export] raw-head mode: reg_max={det.reg_max} nc={det.nc} nl={det.nl} "
              f"(output channels = 4*reg_max + nc = {4 * det.reg_max + det.nc})")

    example = (torch.randn(1, 3, args.imgsz, args.imgsz),)
    print("[export] tracing + exporting to ATen")
    exported = torch.export.export(model, example)

    backend_tag = "cpu-portable"
    if args.qnn:
        # Lower to the Qualcomm QNN backend (HTP/NPU) for the Snapdragon 8 Elite (SM8750, HTP v79).
        # Requires the Qualcomm QNN SDK ($QNN_SDK_ROOT), its x86_64 host libs on LD_LIBRARY_PATH
        # ($QNN_SDK_ROOT/lib/x86_64-linux-clang), AND an ExecuTorch QNN host pybind built against the
        # SAME QNN SDK version (see docs/implementation/MODEL_RUNTIME.md — the prebuilt wheel's pybind
        # had an ABI mismatch with SDK 2.47 that made `InitBackend` fail; rebuilding it fixes that).
        #
        # NOTE (v2.2.0): with --qnn-raw-head the full YOLOv8n graph lowers cleanly to QNN/HTP and a
        # real QnnBackend `.pte` is produced (the decoded-head `--qnn` path still hits the I64toI32
        # 'expand 3->1' failure at make_anchors — use --qnn-raw-head). The `.pte` LOADS on device but
        # HTP execution is gated by the device DSP: on a production S25 Ultra the cDSP rejects the
        # unsigned HTP skel ("Failed to load skel, error 4000"), so OBSERVA falls back to XNNPACK and
        # reports it honestly. See docs/implementation/MODEL_RUNTIME.md.
        from executorch.backends.qualcomm.utils.utils import (
            generate_qnn_executorch_compiler_spec, generate_htp_compiler_spec,
            to_edge_transform_and_lower_to_qnn)
        from executorch.backends.qualcomm.serialization.qc_schema import QcomChipset
        spec = generate_qnn_executorch_compiler_spec(
            soc_model=QcomChipset.SM8750,
            backend_options=generate_htp_compiler_spec(use_fp16=True),
        )
        # Use the proper helper: it opens a QnnManagerContext that carries soc_model=SM8750 into the
        # host device config BEFORE partitioning. The bare `to_edge_transform_and_lower(partitioner=
        # [QnnPartitioner])` path hits a fallback that loses soc_model (device init then sees socModel=0).
        edge = to_edge_transform_and_lower_to_qnn(model, example, spec)
        prog = edge.to_executorch()
        os.makedirs(os.path.dirname(args.out), exist_ok=True)
        with open(args.out, "wb") as f:
            f.write(prog.buffer)
        print(f"[export] wrote {args.out} backend=qnn-htp (SM8750 / HTP v79)")
        return 0
    elif not args.no_xnnpack:
        # XNNPACK is the standard optimized CPU delegate; ~10-40x faster than portable reference
        # kernels on ARM. Bundled in the executorch.aar libexecutorch.so. No Qualcomm SDK needed.
        from executorch.backends.xnnpack.partition.xnnpack_partitioner import XnnpackPartitioner
        print("[export] lowering to ExecuTorch edge + XNNPACK delegate")
        edge = to_edge_transform_and_lower(exported, partitioner=[XnnpackPartitioner()])
        backend_tag = "xnnpack"
    else:
        print("[export] lowering to ExecuTorch edge (portable CPU kernels)")
        edge = to_edge(exported)

    prog = edge.to_executorch()
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "wb") as f:
        f.write(prog.buffer)
    size = os.path.getsize(args.out)
    print(f"[export] wrote {args.out} ({size} bytes) backend={backend_tag}")
    # Write the version sidecar OBSERVA reads for diagnostics.
    with open(args.out + ".version", "w") as f:
        import executorch
        f.write(f"yolov8n-coco imgsz={args.imgsz} executorch={getattr(executorch,'__version__','?')}"
                f" {backend_tag}\n")
    print("[export] done. Verify on device; check OBSERVA_MODEL logcat for output shapes.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
