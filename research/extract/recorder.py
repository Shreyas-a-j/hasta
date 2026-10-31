"""Webcam recorder for HASTA research data.

Captures camera frames, runs MediaPipe Hand Landmarker in VIDEO mode and writes a
landmarks-v1 ``.npz`` sequence. It never writes raw video.

This path requires a webcam, a MediaPipe model file and the ``capture`` extra. It is
reported as NOT VERIFIED when no camera is available.

Example::

    cd research
    uv sync --extra capture
    uv run --frozen python -m extract.recorder \
      --model /path/to/hand_landmarker.task \
      --out data/sequences/S01/namaste/S01-namaste-001.npz \
      --clip-id S01-namaste-001 --signer S01 --label namaste \
      --consent-reference consent/S01-2026-10-31 \
      --license "CC BY 4.0" \
      --license-url https://creativecommons.org/licenses/by/4.0/ \
      --duration 10
"""

from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

from .capture import CaptureMetadata, SequenceBuilder
from .mediapipe_tracker import HandLandmarkerSession, TrackerConfig


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Record a landmarks-v1 sequence from a webcam.")
    parser.add_argument("--model", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--clip-id", required=True)
    parser.add_argument("--signer", required=True)
    parser.add_argument("--label", required=True)
    parser.add_argument("--consent-reference", required=True)
    parser.add_argument("--license", required=True)
    parser.add_argument("--license-url", required=True)
    parser.add_argument("--source-ref", default="webcam:default")
    parser.add_argument("--notes", default="")
    parser.add_argument("--duration", type=float, default=10.0, help="seconds")
    parser.add_argument("--camera", type=int, default=0)
    parser.add_argument("--width", type=int, default=1280)
    parser.add_argument("--height", type=int, default=720)
    parser.add_argument("--fps", type=float, default=30.0)
    parser.add_argument(
        "--mirrored-input",
        action="store_true",
        help="the source image is mirrored; features will un-mirror and swap labels",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    metadata = CaptureMetadata(
        clip_id=args.clip_id,
        signer_id=args.signer,
        label=args.label,
        source_kind="webcam",
        source_ref=args.source_ref,
        consent_reference=args.consent_reference,
        license_name=args.license,
        license_url=args.license_url,
        notes=args.notes,
    )
    metadata.validate()
    builder = SequenceBuilder(metadata, mirrored_input=args.mirrored_input)

    try:
        import cv2
    except ImportError as error:  # pragma: no cover - optional extra
        raise RuntimeError("OpenCV capture is not installed; run `uv sync --extra capture`") from error

    capture = cv2.VideoCapture(args.camera)
    capture.set(cv2.CAP_PROP_FRAME_WIDTH, args.width)
    capture.set(cv2.CAP_PROP_FRAME_HEIGHT, args.height)
    capture.set(cv2.CAP_PROP_FPS, args.fps)
    if not capture.isOpened():
        print(f"could not open camera {args.camera}", file=sys.stderr)
        return 1

    submitted = 0
    processed = 0
    dropped = 0
    previous_timestamp = -1
    started = time.monotonic()
    try:
        with HandLandmarkerSession(TrackerConfig(model_path=args.model)) as session:
            while time.monotonic() - started < args.duration:
                ok, frame = capture.read()
                submitted += 1
                if not ok or frame is None:
                    dropped += 1
                    continue
                timestamp = int((time.monotonic() - started) * 1000.0)
                timestamp = max(timestamp, previous_timestamp + 1)
                previous_timestamp = timestamp
                rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                hands = session.detect(rgb, timestamp)
                builder.add_frame(timestamp, frame.shape[1], frame.shape[0], hands)
                processed += 1
    except KeyboardInterrupt:
        print("stopped by user", file=sys.stderr)
    finally:
        capture.release()

    if processed == 0:
        print("no frames processed; nothing written", file=sys.stderr)
        return 1
    sequence = builder.build()
    sequence.save(args.out)
    print(
        f"wrote {args.out}: {sequence.frame_count} frames, {sequence.duration_ms} ms; "
        f"submitted={submitted} processed={processed} dropped={dropped}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
