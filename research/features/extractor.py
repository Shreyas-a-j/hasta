"""The stateful features-v1 extractor.

One ``FeatureExtractor`` instance owns the causal state for one frame stream.
Call ``reset()`` between independent recordings.
"""

from __future__ import annotations

import math
from dataclasses import dataclass

import numpy as np

from .contract import (
    FEATURE_DIM,
    FEATURE_GROUPS,
    GAP_THRESHOLD_MS,
    PALM_SCALE_EPSILON,
)
from .coords import to_feature_point
from .descriptors import (
    GlobalDescriptor,
    global_features,
    inter_hand_features,
    local_features,
    palm_scale,
)
from .slots import HandCandidate, assign_slots
from .temporal import TemporalSample, temporal_features
from .types import FrameObservation, HandObservation

Point = tuple[float, float]

_GROUP_OFFSET = {group["name"]: group["offset"] for group in FEATURE_GROUPS}
_HEADER = _GROUP_OFFSET["header"]
_VALIDITY = _GROUP_OFFSET["validity"]
_SLOTS = ("left", "right")


@dataclass(frozen=True)
class _PreparedHand:
    handedness: str | None
    points: tuple[Point, ...]
    validity: tuple[bool, ...]
    index: int

    @property
    def wrist(self) -> Point | None:
        return self.points[0] if self.validity[0] else None


def _validate_hand(hand: HandObservation) -> None:
    if hand.handedness not in (None, "left", "right"):
        raise ValueError(f"invalid handedness {hand.handedness!r}")
    if hand.landmarks is None:
        return
    if len(hand.landmarks) != 21:
        raise ValueError("a hand must have exactly 21 landmarks")
    if hand.landmark_validity is not None and len(hand.landmark_validity) != 21:
        raise ValueError("landmark_validity must have exactly 21 entries")
    if hand.handedness_score is not None and not math.isfinite(hand.handedness_score):
        raise ValueError("handedness_score must be finite")
    for landmark in hand.landmarks:
        if not (
            math.isfinite(landmark.x)
            and math.isfinite(landmark.y)
            and math.isfinite(landmark.z)
        ):
            raise ValueError("landmark coordinates must be finite")


class FeatureExtractor:
    def __init__(self) -> None:
        self.reset()

    def reset(self) -> None:
        self._previous_timestamp_ms: int | None = None
        self._segment_start_ms: int | None = None
        self._previous_slots: dict[str, TemporalSample] = {}
        self._previous_assignment: dict[str, Point] = {}

    def extract(self, frame: FrameObservation) -> np.ndarray:
        self._validate_frame(frame)

        first_frame = self._previous_timestamp_ms is None
        gap = False
        if first_frame:
            dt_s = 0.0
            elapsed_s = 0.0
            segment_start_ms = frame.timestamp_ms
        else:
            dt_ms = frame.timestamp_ms - self._previous_timestamp_ms
            gap = dt_ms > GAP_THRESHOLD_MS or dt_ms <= 0
            if gap:
                dt_s = 0.0
                elapsed_s = 0.0
                segment_start_ms = frame.timestamp_ms
                self._previous_slots.clear()
                self._previous_assignment.clear()
            else:
                dt_s = dt_ms / 1000.0
                elapsed_s = (frame.timestamp_ms - self._segment_start_ms) / 1000.0
                segment_start_ms = self._segment_start_ms

        vector = np.zeros(FEATURE_DIM, dtype=np.float64)

        vector[_HEADER + 0] = dt_s
        vector[_HEADER + 1] = elapsed_s
        vector[_HEADER + 2] = 1.0 if gap else 0.0
        vector[_HEADER + 3] = 1.0 if first_frame else 0.0

        prepared = self._prepare_hands(frame)
        candidates = [
            HandCandidate(
                handedness=hand.handedness,
                wrist=hand.wrist,
                index=hand.index,
            )
            for hand in prepared
        ]
        assignment = assign_slots(candidates, self._previous_assignment)

        vector[_HEADER + 7] = 1.0 if assignment.ambiguous else 0.0

        globals_by_slot: dict[str, GlobalDescriptor] = {}
        next_slots: dict[str, TemporalSample] = {}
        next_assignment: dict[str, Point] = {}

        for slot_number, slot in enumerate(_SLOTS):
            hand = self._hand_for_slot(prepared, assignment.slots[slot])
            present = hand is not None
            vector[_HEADER + 4 + slot_number] = 1.0 if present else 0.0
            vector[_HEADER + 8 + slot_number] = (
                1.0 if present and hand.handedness is not None else 0.0
            )
            if not present:
                continue

            validity_base = _VALIDITY + (0 if slot == "left" else 21)
            for index, valid in enumerate(hand.validity):
                vector[validity_base + index] = 1.0 if valid else 0.0

            scale = palm_scale(hand.points)
            local_valid = all(hand.validity) and scale >= PALM_SCALE_EPSILON
            vector[_HEADER + 10 + slot_number] = 1.0 if local_valid else 0.0
            if local_valid:
                values = local_features(hand.points, scale)
                base = _GROUP_OFFSET[f"{slot}.local"]
                vector[base : base + len(values)] = values

            global_required = (0, 5, 9, 13, 17)
            global_valid = all(hand.validity[index] for index in global_required)
            vector[_HEADER + 12 + slot_number] = 1.0 if global_valid else 0.0
            descriptor: GlobalDescriptor | None = None
            if global_valid:
                descriptor = global_features(hand.points, scale)
                globals_by_slot[slot] = descriptor
                base = _GROUP_OFFSET[f"{slot}.global"]
                vector[base : base + 7] = descriptor.values

            previous_sample = self._previous_slots.get(slot)
            if (
                not first_frame
                and not gap
                and previous_sample is not None
                and hand.validity[0]
            ):
                result = temporal_features(
                    wrist=hand.points[0],
                    orientation_angle=(
                        descriptor.orientation_angle if descriptor is not None else 0.0
                    ),
                    orientation_defined=(
                        descriptor.orientation_defined if descriptor is not None else False
                    ),
                    previous=previous_sample,
                    dt_s=dt_s,
                    timestamp_ms=frame.timestamp_ms,
                )
                base = _GROUP_OFFSET[f"{slot}.temporal"]
                vector[base : base + len(result.values)] = result.values
                vector[_HEADER + 14 + slot_number] = 1.0 if result.valid else 0.0
                next_slots[slot] = result.sample
            elif hand.validity[0]:
                next_slots[slot] = TemporalSample(
                    wrist=hand.points[0],
                    orientation_angle=(
                        descriptor.orientation_angle if descriptor is not None else 0.0
                    ),
                    orientation_defined=(
                        descriptor.orientation_defined if descriptor is not None else False
                    ),
                    velocity=None,
                    timestamp_ms=frame.timestamp_ms,
                )

            if hand.validity[0]:
                next_assignment[slot] = hand.points[0]

        vector[_HEADER + 6] = (
            1.0
            if vector[_HEADER + 4] == 1.0 and vector[_HEADER + 5] == 1.0
            else 0.0
        )

        if "left" in globals_by_slot and "right" in globals_by_slot:
            values = inter_hand_features(
                globals_by_slot["left"],
                globals_by_slot["right"],
            )
            base = _GROUP_OFFSET["inter_hand"]
            vector[base : base + len(values)] = values

        self._previous_timestamp_ms = frame.timestamp_ms
        self._segment_start_ms = segment_start_ms
        self._previous_slots = next_slots
        self._previous_assignment = next_assignment
        return vector

    @staticmethod
    def _validate_frame(frame: FrameObservation) -> None:
        if not isinstance(frame.timestamp_ms, int) or isinstance(frame.timestamp_ms, bool):
            raise ValueError("timestamp_ms must be an integer")
        if frame.width <= 0 or frame.height <= 0:
            raise ValueError("frame width and height must be positive")
        if len(frame.hands) > 2:
            raise ValueError("features-v1 accepts at most two hands per frame")
        for hand in frame.hands:
            _validate_hand(hand)

    @staticmethod
    def _prepare_hands(frame: FrameObservation) -> list[_PreparedHand]:
        prepared: list[_PreparedHand] = []
        for index, hand in enumerate(frame.hands):
            if hand.landmarks is None:
                continue
            validity = hand.validity()
            points = tuple(
                to_feature_point(
                    landmark,
                    frame.width,
                    frame.height,
                    frame.mirrored_input,
                )
                for landmark in hand.landmarks
            )
            handedness = hand.handedness
            if frame.mirrored_input and handedness is not None:
                handedness = "right" if handedness == "left" else "left"
            prepared.append(
                _PreparedHand(
                    handedness=handedness,
                    points=points,
                    validity=tuple(validity),
                    index=index,
                )
            )
        return prepared

    @staticmethod
    def _hand_for_slot(
        prepared: list[_PreparedHand],
        candidate: HandCandidate | None,
    ) -> _PreparedHand | None:
        if candidate is None:
            return None
        for hand in prepared:
            if hand.index == candidate.index:
                return hand
        raise AssertionError("slot assignment referenced an unknown hand")


__all__ = ["FeatureExtractor"]
