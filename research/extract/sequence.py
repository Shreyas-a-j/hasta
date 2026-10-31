"""Landmark sequence storage for research data.

A sequence stores detector output, not features. Hands stay in detector order with
their subject-relative handedness labels so slot assignment can be recomputed later
under a different policy without re-running MediaPipe.

Missing hands and missing landmarks are explicit: zero coordinates are only valid
together with ``validity == False``. No NaN, no pickle, no raw video.
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

import numpy as np

FORMAT = "hasta.landmarks"
VERSION = "landmarks-v1"
MAX_HANDS = 2
LANDMARK_COUNT = 21
COORDINATES = 3

_HANDEDNESS_TO_INT = {None: -1, "left": 0, "right": 1}
_INT_TO_HANDEDNESS = {-1: None, 0: "left", 1: "right"}

_ARRAY_KEYS = (
    "timestamp_ms",
    "width",
    "height",
    "hands",
    "validity",
    "present",
    "handedness",
    "handedness_score",
)

_REQUIRED_METADATA = (
    "format",
    "version",
    "clip_id",
    "signer_id",
    "label",
    "source",
    "consent",
    "license",
)


@dataclass(frozen=True)
class LandmarkSequence:
    timestamp_ms: np.ndarray
    width: np.ndarray
    height: np.ndarray
    hands: np.ndarray
    validity: np.ndarray
    present: np.ndarray
    handedness: np.ndarray
    handedness_score: np.ndarray
    mirrored_input: bool
    metadata: dict

    @property
    def frame_count(self) -> int:
        return int(self.timestamp_ms.shape[0])

    @property
    def duration_ms(self) -> int:
        if self.frame_count == 0:
            return 0
        return int(self.timestamp_ms[-1] - self.timestamp_ms[0])

    def validate(self) -> None:
        if self.timestamp_ms.ndim != 1 or self.timestamp_ms.shape[0] == 0:
            raise ValueError("timestamp_ms must be a non-empty 1-D array")
        frames = self.timestamp_ms.shape[0]
        expected_shapes = {
            "width": (frames,),
            "height": (frames,),
            "hands": (frames, MAX_HANDS, LANDMARK_COUNT, COORDINATES),
            "validity": (frames, MAX_HANDS, LANDMARK_COUNT),
            "present": (frames, MAX_HANDS),
            "handedness": (frames, MAX_HANDS),
            "handedness_score": (frames, MAX_HANDS),
        }
        for name, shape in expected_shapes.items():
            if getattr(self, name).shape != shape:
                raise ValueError(f"{name} has shape {getattr(self, name).shape}, expected {shape}")
        if np.any(self.width <= 0) or np.any(self.height <= 0):
            raise ValueError("frame dimensions must be positive")
        if not np.all(np.isfinite(self.hands)):
            raise ValueError("landmark coordinates must be finite; use validity for missing")
        if not np.all(np.isfinite(self.handedness_score)):
            raise ValueError("handedness scores must be finite")
        if np.any(self.handedness_score < 0.0) or np.any(self.handedness_score > 1.0):
            raise ValueError("handedness scores must be in [0, 1]")
        if not np.all(np.isin(self.handedness, (-1, 0, 1))):
            raise ValueError("handedness must be encoded as -1, 0 or 1")
        absent = ~self.present
        if np.any(self.validity[absent]):
            raise ValueError("absent hands must have no valid landmarks")
        if np.any(self.present & ~self.validity.any(axis=2)):
            raise ValueError("present hands must have at least one valid landmark")
        if self.timestamp_ms.dtype.kind not in "iu":
            raise ValueError("timestamp_ms must be an integer array")
        for key in _REQUIRED_METADATA:
            if key not in self.metadata:
                raise ValueError(f"metadata is missing {key!r}")
        for key in ("source", "consent", "license"):
            if not isinstance(self.metadata[key], dict):
                raise ValueError(f"metadata {key!r} must be an object")
        if self.metadata["format"] != FORMAT or self.metadata["version"] != VERSION:
            raise ValueError("metadata format/version does not match landmarks-v1")
        if self.metadata["consent"].get("granted") is not True:
            raise ValueError("recording consent must be granted before storage")
        if not str(self.metadata["consent"].get("reference", "")).strip():
            raise ValueError("consent reference must be non-empty")

    def save(self, path: Path) -> Path:
        self.validate()
        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        metadata_json = json.dumps(self.metadata, sort_keys=True, separators=(",", ":"))
        np.savez_compressed(
            path,
            timestamp_ms=np.asarray(self.timestamp_ms, dtype=np.int64),
            width=np.asarray(self.width, dtype=np.int32),
            height=np.asarray(self.height, dtype=np.int32),
            hands=np.asarray(self.hands, dtype=np.float64),
            validity=np.asarray(self.validity, dtype=np.bool_),
            present=np.asarray(self.present, dtype=np.bool_),
            handedness=np.asarray(self.handedness, dtype=np.int8),
            handedness_score=np.asarray(self.handedness_score, dtype=np.float64),
            mirrored_input=np.asarray(self.mirrored_input, dtype=np.bool_),
            metadata=np.asarray(metadata_json),
        )
        return path

    @classmethod
    def load(cls, path: Path) -> "LandmarkSequence":
        path = Path(path)
        with np.load(path, allow_pickle=False) as data:
            missing = set(_ARRAY_KEYS) - set(data.files)
            if missing:
                raise ValueError(f"{path} is missing arrays: {sorted(missing)}")
            sequence = cls(
                timestamp_ms=np.asarray(data["timestamp_ms"], dtype=np.int64),
                width=np.asarray(data["width"], dtype=np.int32),
                height=np.asarray(data["height"], dtype=np.int32),
                hands=np.asarray(data["hands"], dtype=np.float64),
                validity=np.asarray(data["validity"], dtype=np.bool_),
                present=np.asarray(data["present"], dtype=np.bool_),
                handedness=np.asarray(data["handedness"], dtype=np.int8),
                handedness_score=np.asarray(data["handedness_score"], dtype=np.float64),
                mirrored_input=bool(data["mirrored_input"]),
                metadata=json.loads(str(data["metadata"])),
            )
        sequence.validate()
        return sequence


def empty_arrays(frames: int) -> dict[str, np.ndarray]:
    return {
        "timestamp_ms": np.zeros(frames, dtype=np.int64),
        "width": np.zeros(frames, dtype=np.int32),
        "height": np.zeros(frames, dtype=np.int32),
        "hands": np.zeros((frames, MAX_HANDS, LANDMARK_COUNT, COORDINATES), dtype=np.float64),
        "validity": np.zeros((frames, MAX_HANDS, LANDMARK_COUNT), dtype=np.bool_),
        "present": np.zeros((frames, MAX_HANDS), dtype=np.bool_),
        "handedness": np.full((frames, MAX_HANDS), -1, dtype=np.int8),
        "handedness_score": np.zeros((frames, MAX_HANDS), dtype=np.float64),
    }


def encode_handedness(value: str | None) -> int:
    if value not in _HANDEDNESS_TO_INT:
        raise ValueError(f"invalid handedness {value!r}")
    return _HANDEDNESS_TO_INT[value]


def decode_handedness(value: int) -> str | None:
    return _INT_TO_HANDEDNESS[int(value)]
