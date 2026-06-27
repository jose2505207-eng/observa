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
    ap.add_argument("--qnn", action="store_true", help="lower to the Qualcomm QNN backend")
    args = ap.parse_args()

    try:
        import torch
        from ultralytics import YOLO
        from executorch.exir import to_edge
    except Exception as e:  # pragma: no cover - tooling guidance only
        print(f"[export] missing tooling: {e}\n"
              f"[export] install: pip install executorch ultralytics torch", file=sys.stderr)
        return 2

    print(f"[export] loading {args.weights}")
    model = YOLO(args.weights).model.eval()

    example = (torch.randn(1, 3, args.imgsz, args.imgsz),)
    print("[export] tracing + exporting to ATen")
    exported = torch.export.export(model, example)

    print("[export] lowering to ExecuTorch edge")
    edge = to_edge(exported)

    if args.qnn:
        # Requires the ExecuTorch QNN backend + Qualcomm SDK; see docs/real-detector.md.
        try:
            from executorch.backends.qualcomm.partition.qnn_partitioner import QnnPartitioner
            from executorch.backends.qualcomm.utils.utils import generate_qnn_executorch_compiler_spec
            edge = edge.to_backend(QnnPartitioner(generate_qnn_executorch_compiler_spec()))
            print("[export] QNN partitioning applied")
        except Exception as e:
            print(f"[export] QNN lowering unavailable ({e}); exporting CPU .pte", file=sys.stderr)

    prog = edge.to_executorch()
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "wb") as f:
        f.write(prog.buffer)
    size = os.path.getsize(args.out)
    print(f"[export] wrote {args.out} ({size} bytes)")
    # Write the version sidecar OBSERVA reads for diagnostics.
    with open(args.out + ".version", "w") as f:
        import executorch
        f.write(f"yolov8n-coco imgsz={args.imgsz} executorch={getattr(executorch,'__version__','?')}"
                f"{' qnn' if args.qnn else ' cpu'}\n")
    print("[export] done. Verify on device; check OBSERVA_MODEL logcat for output shapes.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
