"""Coordinate conversion into the isotropic feature space."""

from __future__ import annotations

from .types import Landmark

Point = tuple[float, float]


def to_feature_point(
    landmark: Landmark,
    width: int,
    height: int,
    mirrored_input: bool,
) -> Point:
    """Map an upright image-normalized landmark to image-width fractions.

    ``u = x`` and ``v = y * height / width``. When the source is explicitly
    mirrored, ``x`` is un-mirrored first. No clipping is applied.
    """
    if width <= 0 or height <= 0:
        raise ValueError("frame width and height must be positive")
    x = 1.0 - landmark.x if mirrored_input else landmark.x
    return (x, landmark.y * (float(height) / float(width)))
