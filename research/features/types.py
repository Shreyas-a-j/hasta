"""HASTA-owned input types for features-v1.

These types are deliberately independent of MediaPipe. The Android adapter maps
``HandObservation``/``HandFrame`` into this shape at the perception boundary.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Literal

Handedness = Literal["left", "right"]


@dataclass(frozen=True)
class Landmark:
    x: float
    y: float
    z: float = 0.0


@dataclass(frozen=True)
class HandObservation:
    """One detected hand in an upright, unmirrored analysis image."""

    handedness: Handedness | None
    landmarks: tuple[Landmark, ...] | None
    handedness_score: float | None = None
    landmark_validity: tuple[bool, ...] | None = None

    def validity(self) -> tuple[bool, ...]:
        if self.landmarks is None:
            return tuple(False for _ in range(21))
        if self.landmark_validity is None:
            return tuple(True for _ in range(len(self.landmarks)))
        return self.landmark_validity


@dataclass(frozen=True)
class FrameObservation:
    timestamp_ms: int
    width: int
    height: int
    hands: tuple[HandObservation, ...] = field(default_factory=tuple)
    mirrored_input: bool = False
