"""Extract landmarks from a local video into a landmarks-v1 sequence.

The source video is read in place and never copied, bundled or redistributed. The
output contains only landmarks, validity, timing and provenance. Datasets that do
not permit derivative landmark extraction must not be used with this tool.

This path requires MediaPipe, OpenCV and a local video. It is reported as NOT
VERIFIED when no dataset is available.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from .capture import CaptureMetadata, SequenceBuilder
from .mediapipe_tracker import HandLandmarkerSession, TrackerConfig


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Extract a landmarks-v1 sequence from a video.")
    parser.add_argument("--model", required=True, type=Path)
    parser.add_argument("--video", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--clip-id", required=True)
    parser.add_argument("--signer", required=True)
    parser.add_argument("--label", required=True)
    parser.add_argument("--dataset", required=True)
    parser.add_argument("--source-ref", required=True)
    parser.add_argument("--consent-reference", required=True)
    parser.add_argument("--license", required=True)
    parser.add_argument("--license-url", required=True)
    parser.add_argument("--notes", default="")
    parser.add_argument("--max-frames", type=int, default=0, help="0 means all frames")
    parser.add_argument(
        "--mirrored-input",
        action="store_true",
        help="the source video is mirrored; features will un-mirror and swap labels",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    if not args.video.is_file():
        print(f"video not found: {args.video}", file=sys.stderr)
        return 1
    metadata = CaptureMetadata(
        clip_id=args.clip_id,
        signer_id=args.signer,
        label=args.label,
        source_kind="dataset",
        source_ref=f"{args.dataset}:{args.source_ref}",
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
        raise RuntimeError("OpenCV is not installed; run `uv sync --extra capture`") from error

    capture = cv2.VideoCapture(str(args.video))
    if not capture.isOpened():
        print(f"could not open video: {args.video}", file=sys.stderr)
        return 1
    fps = capture.get(cv2.CAP_PROP_FPS)
    if not fps or fps <= 0:
        fps = 30.0

    submitted = 0
    processed = 0
    dropped = 0
    previous_timestamp = -1
    frame_index = 0
    try:
        with HandLandmarkerSession(TrackerConfig(model_path=args.model)) as session:
            while True:
                if args.max_frames and processed >= args.max_frames:
                    break
                ok, frame = capture.read()
                if not ok or frame is None:
                    break
                submitted += 1
                position_ms = capture.get(cv2.CAP_PROP_POS_MSEC)
                if not position_ms or position_ms <= 0:
                    position_ms = frame_index * 1000.0 / fps
                timestamp = max(int(round(position_ms)), previous_timestamp + 1)
                previous_timestamp = timestamp
                frame_index += 1
                rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                hands = session.detect(rgb, timestamp)
                if not hands:
                    dropped += 1
                builder.add_frame(timestamp, frame.shape[1], frame.shape[0], hands)
                processed += 1
    finally:
        capture.release()

    if processed == 0:
        print("no frames processed; nothing written", file=sys.stderr)
        return 1
    sequence = builder.build()
    sequence.save(args.out)
    print(
        f"wrote {args.out}: {sequence.frame_count} frames, {sequence.duration_ms} ms; "
        f"submitted={submitted} processed={processed} frames_without_hands={dropped}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
