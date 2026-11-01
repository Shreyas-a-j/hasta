"""Small deterministic 2D geometry helpers shared by the feature groups."""

from __future__ import annotations

import math

Point = tuple[float, float]
Vector = tuple[float, float]


def subtract(a: Point, b: Point) -> Vector:
    return (a[0] - b[0], a[1] - b[1])


def distance(a: Point, b: Point) -> float:
    du = a[0] - b[0]
    dv = a[1] - b[1]
    return math.sqrt(du * du + dv * dv)


def norm(vector: Vector) -> float:
    return math.sqrt(vector[0] * vector[0] + vector[1] * vector[1])


def angle_between(a: Vector, b: Vector, epsilon: float) -> float:
    """Unsigned angle in [0, pi]; zero for a degenerate input vector."""
    if norm(a) < epsilon or norm(b) < epsilon:
        return 0.0
    cross = abs(a[0] * b[1] - a[1] * b[0])
    dot = a[0] * b[0] + a[1] * b[1]
    return math.atan2(cross, dot)


def wrap_angle(angle: float) -> float:
    return math.atan2(math.sin(angle), math.cos(angle))
