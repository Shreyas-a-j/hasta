"""Frame assembly shared by the webcam recorder and dataset extractor.

This module has no MediaPipe or OpenCV import. It converts already-detected hands
into the landmarks-v1 arrays, which makes the missingness rules testable without a
camera or a dataset.
"""

from __future__ import annotations

from dataclasses import dataclass

import numpy as np

from .sequence import (
    FORMAT,
    LANDMARK_COUNT,
    MAX_HANDS,
    VERSION,
    LandmarkSequence,
    empty_arrays,
    encode_handedness,
)


@dataclass(frozen=True)
class DetectedHand:
    handedness: str | None
    handedness_score: float
    landmarks: np.ndarray  # shape (21, 3), image-normalized, upright, unmirrored
    validity: np.ndarray | None = None  # shape (21,), default all true

    def __post_init__(self) -> None:
        landmarks = np.asarray(self.landmarks, dtype=np.float64)
        if landmarks.shape != (LANDMARK_COUNT, 3):
            raise ValueError(f"landmarks must have shape (21, 3), got {landmarks.shape}")
        if not np.all(np.isfinite(landmarks)):
            raise ValueError("landmarks must be finite")
        object.__setattr__(self, "landmarks", landmarks)
        if self.handedness not in (None, "left", "right"):
            raise ValueError(f"invalid handedness {self.handedness!r}")
        if not 0.0 <= self.handedness_score <= 1.0:
            raise ValueError("handedness_score must be in [0, 1]")
        validity = (
            np.ones(LANDMARK_COUNT, dtype=np.bool_)
            if self.validity is None
            else np.asarray(self.validity, dtype=np.bool_)
        )
        if validity.shape != (LANDMARK_COUNT,):
            raise ValueError("validity must have shape (21,)")
        object.__setattr__(self, "validity", validity)


@dataclass(frozen=True)
class CaptureMetadata:
    clip_id: str
    signer_id: str
    label: str
    source_kind: str
    source_ref: str
    consent_reference: str
    license_name: str
    license_url: str
    notes: str = ""

    def validate(self) -> None:
        if not self.clip_id.strip():
            raise ValueError("clip_id must be non-empty")
        if not self.signer_id.strip():
            raise ValueError("signer_id must be non-empty")
        if not self.label.strip():
            raise ValueError("label must be non-empty")
        if self.source_kind not in ("webcam", "dataset"):
            raise ValueError("source_kind must be webcam or dataset")
        if not self.source_ref.strip():
            raise ValueError("source_ref must be non-empty")
        if not self.consent_reference.strip():
            raise ValueError("consent_reference must be non-empty")
        if not self.license_name.strip():
            raise ValueError("license_name must be non-empty")
        if not self.license_url.startswith(("http://", "https://")):
            raise ValueError("license_url must be an http(s) URL")

    def to_metadata(self) -> dict:
        self.validate()
        return {
            "format": FORMAT,
            "version": VERSION,
            "clip_id": self.clip_id,
            "signer_id": self.signer_id,
            "label": self.label,
            "source": {"kind": self.source_kind, "ref": self.source_ref},
            "consent": {"granted": True, "reference": self.consent_reference},
            "license": {"name": self.license_name, "url": self.license_url},
            "notes": self.notes,
        }


class SequenceBuilder:
    def __init__(self, metadata: CaptureMetadata, mirrored_input: bool = False) -> None:
        metadata.validate()
        self.metadata = metadata
        self.mirrored_input = mirrored_input
        self._frames: list[dict] = []

    def add_frame(
        self,
        timestamp_ms: int,
        width: int,
        height: int,
        hands: list[DetectedHand],
    ) -> None:
        if len(hands) > MAX_HANDS:
            raise ValueError("at most two hands can be stored per frame")
        if width <= 0 or height <= 0:
            raise ValueError("frame dimensions must be positive")
        self._frames.append(
            {
                "timestamp_ms": int(timestamp_ms),
                "width": int(width),
                "height": int(height),
                "hands": list(hands),
            }
        )

    def build(self) -> LandmarkSequence:
        if not self._frames:
            raise ValueError("cannot build an empty sequence")
        arrays = empty_arrays(len(self._frames))
        for frame_index, frame in enumerate(self._frames):
            arrays["timestamp_ms"][frame_index] = frame["timestamp_ms"]
            arrays["width"][frame_index] = frame["width"]
            arrays["height"][frame_index] = frame["height"]
            for hand_index, hand in enumerate(frame["hands"]):
                arrays["present"][frame_index, hand_index] = True
                arrays["hands"][frame_index, hand_index] = hand.landmarks
                arrays["validity"][frame_index, hand_index] = hand.validity
                arrays["handedness"][frame_index, hand_index] = encode_handedness(
                    hand.handedness
                )
                arrays["handedness_score"][frame_index, hand_index] = hand.handedness_score
        sequence = LandmarkSequence(
            **arrays,
            mirrored_input=self.mirrored_input,
            metadata=self.metadata.to_metadata(),
        )
        sequence.validate()
        return sequence
