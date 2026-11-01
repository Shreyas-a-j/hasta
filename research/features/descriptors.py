"""Per-hand and inter-hand feature descriptors."""

from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Sequence

from .contract import PALM_SCALE_EPSILON, VECTOR_EPSILON
from .geometry import angle_between, distance, subtract, wrap_angle

Point = tuple[float, float]
Vector = tuple[float, float]

FINGER_CHAINS: tuple[tuple[str, tuple[int, int, int, int]], ...] = (
    ("thumb", (1, 2, 3, 4)),
    ("index", (5, 6, 7, 8)),
    ("middle", (9, 10, 11, 12)),
    ("ring", (13, 14, 15, 16)),
    ("pinky", (17, 18, 19, 20)),
)

TIP_SPREAD_PAIRS: tuple[tuple[str, int, int], ...] = (
    ("thumb_index", 4, 8),
    ("index_middle", 8, 12),
    ("middle_ring", 12, 16),
    ("ring_pinky", 16, 20),
)


def palm_scale(points: Sequence[Point]) -> float:
    return 0.5 * (distance(points[5], points[0]) + distance(points[17], points[0]))


def _joint_angle(first: Point, joint: Point, second: Point) -> float:
    return angle_between(subtract(first, joint), subtract(second, joint), VECTOR_EPSILON)


def local_features(points: Sequence[Point], scale: float) -> list[float]:
    """Return the 81 local channels for a fully valid hand.

    The caller is responsible for checking that all 21 landmarks are valid and
    that ``scale >= PALM_SCALE_EPSILON``.
    """
    values: list[float] = []
    wrist = points[0]

    for index in range(1, 21):
        values.append((points[index][0] - wrist[0]) / scale)
        values.append((points[index][1] - wrist[1]) / scale)

    for _, chain in FINGER_CHAINS:
        first, second, third, fourth = chain
        values.append(_joint_angle(points[0], points[first], points[second]))
        values.append(_joint_angle(points[first], points[second], points[third]))
        values.append(_joint_angle(points[second], points[third], points[fourth]))

    for _, chain in FINGER_CHAINS:
        for offset in (1, 2, 3):
            values.append(
                distance(points[chain[offset]], points[chain[offset - 1]]) / scale
            )

    for _, first, second in TIP_SPREAD_PAIRS:
        values.append(distance(points[first], points[second]) / scale)

    for _, chain in FINGER_CHAINS:
        start, tip = chain[0], chain[3]
        chain_length = sum(
            distance(points[chain[offset]], points[chain[offset - 1]])
            for offset in (1, 2, 3)
        )
        values.append(
            0.0 if chain_length < VECTOR_EPSILON else distance(points[tip], points[start]) / chain_length
        )

    values.append(distance(points[4], points[8]) / scale)

    palm_axis = distance(points[0], points[9])
    values.append(
        0.0 if palm_axis < VECTOR_EPSILON else distance(points[5], points[17]) / palm_axis
    )
    return values


@dataclass(frozen=True)
class GlobalDescriptor:
    centre: Point
    wrist: Point
    orientation_angle: float
    orientation_defined: bool
    palm_scale: float

    @property
    def values(self) -> tuple[float, ...]:
        if self.orientation_defined:
            cosine = math.cos(self.orientation_angle)
            sine = math.sin(self.orientation_angle)
        else:
            cosine, sine = 1.0, 0.0
        return (
            self.centre[0],
            self.centre[1],
            self.wrist[0],
            self.wrist[1],
            cosine,
            sine,
            self.palm_scale,
        )


def global_features(points: Sequence[Point], scale: float) -> GlobalDescriptor:
    centre = (
        (points[0][0] + points[5][0] + points[9][0] + points[13][0] + points[17][0]) / 5.0,
        (points[0][1] + points[5][1] + points[9][1] + points[13][1] + points[17][1]) / 5.0,
    )
    dx = points[9][0] - points[0][0]
    dy = points[9][1] - points[0][1]
    length = math.sqrt(dx * dx + dy * dy)
    defined = length >= VECTOR_EPSILON
    angle = math.atan2(dy, dx) if defined else 0.0
    return GlobalDescriptor(
        centre=centre,
        wrist=points[0],
        orientation_angle=angle,
        orientation_defined=defined,
        palm_scale=scale,
    )


def inter_hand_features(
    left: GlobalDescriptor,
    right: GlobalDescriptor,
) -> list[float]:
    du = right.centre[0] - left.centre[0]
    dv = right.centre[1] - left.centre[1]
    relative_angle = 0.0
    if left.orientation_defined and right.orientation_defined:
        relative_angle = wrap_angle(right.orientation_angle - left.orientation_angle)
    if (
        left.palm_scale < PALM_SCALE_EPSILON
        or right.palm_scale < PALM_SCALE_EPSILON
    ):
        log_ratio = 0.0
    else:
        log_ratio = math.log(right.palm_scale / left.palm_scale)
    return [
        du,
        dv,
        math.sqrt(du * du + dv * dv),
        math.cos(relative_angle),
        math.sin(relative_angle),
        log_ratio,
    ]
