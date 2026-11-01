"""Shared synthetic fixtures for features-v1 tests.

The hand is deliberately simple and upright: wrist at the bottom, fingers up.
It is not anatomically exact; it only has to be deterministic and non-degenerate.
"""

from __future__ import annotations

from pathlib import Path

import numpy as np

from extract.sequence import (
    FORMAT as SEQUENCE_FORMAT,
    VERSION as SEQUENCE_VERSION,
    LandmarkSequence,
    empty_arrays,
    encode_handedness,
)
from features.types import HandObservation, Landmark


def research_metadata(clip_id="clip-1", signer="S01", label="namaste"):
    return {
        "format": SEQUENCE_FORMAT,
        "version": SEQUENCE_VERSION,
        "clip_id": clip_id,
        "signer_id": signer,
        "label": label,
        "source": {"kind": "webcam", "ref": "webcam:test"},
        "consent": {"granted": True, "reference": f"consent/{signer}"},
        "license": {
            "name": "CC BY 4.0",
            "url": "https://creativecommons.org/licenses/by/4.0/",
        },
    }


def make_research_sequence(
    frames=3,
    clip_id="clip-1",
    signer="S01",
    label="namaste",
    base_dir=None,
    relative_path=None,
):
    arrays = empty_arrays(frames)
    arrays["timestamp_ms"][:] = np.arange(frames) * 100
    arrays["width"][:] = 640
    arrays["height"][:] = 480
    arrays["present"][0, 0] = True
    arrays["validity"][0, 0, :] = True
    arrays["handedness"][0, 0] = encode_handedness("right")
    arrays["handedness_score"][0, 0] = 0.9
    sequence = LandmarkSequence(
        **arrays,
        mirrored_input=False,
        metadata=research_metadata(clip_id, signer, label),
    )
    if base_dir is not None:
        relative_path = relative_path or f"sequences/{signer}/{label}/{clip_id}.npz"
        sequence.save(Path(base_dir) / relative_path)
    return sequence


def hand_points(
    wrist: tuple[float, float] = (0.5, 0.7),
    scale: float = 1.0,
    mirror: bool = False,
) -> tuple[Landmark, ...]:
    wx, wy = wrist
    offsets = [
        (0.00, 0.00),   # 0 wrist
        (-0.030, -0.020),  # thumb 1
        (-0.050, -0.040),  # 2
        (-0.060, -0.060),  # 3
        (-0.070, -0.080),  # 4
        (-0.020, -0.100),  # index 5
        (-0.020, -0.140),  # 6
        (-0.020, -0.170),  # 7
        (-0.020, -0.200),  # 8
        (0.000, -0.110),   # middle 9
        (0.000, -0.160),   # 10
        (0.000, -0.200),   # 11
        (0.000, -0.230),   # 12
        (0.020, -0.100),   # ring 13
        (0.020, -0.150),   # 14
        (0.020, -0.180),   # 15
        (0.020, -0.210),   # 16
        (0.040, -0.080),   # pinky 17
        (0.040, -0.120),   # 18
        (0.040, -0.150),   # 19
        (0.040, -0.170),   # 20
    ]
    points = []
    for dx, dy in offsets:
        x = wx + dx * scale
        if mirror:
            x = 1.0 - x
        points.append(Landmark(x=x, y=wy + dy * scale, z=0.0))
    return tuple(points)


def make_hand(
    handedness: str | None = "right",
    wrist: tuple[float, float] = (0.5, 0.7),
    scale: float = 1.0,
    mirror: bool = False,
    validity: tuple[bool, ...] | None = None,
) -> HandObservation:
    return HandObservation(
        handedness=handedness,
        landmarks=hand_points(wrist=wrist, scale=scale, mirror=mirror),
        handedness_score=0.95 if handedness else None,
        landmark_validity=validity,
    )
