"""Deterministic hand-to-slot assignment."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Mapping

from .geometry import distance

Point = tuple[float, float]
SLOTS = ("left", "right")


@dataclass(frozen=True)
class HandCandidate:
    handedness: str | None
    wrist: Point | None
    index: int


@dataclass(frozen=True)
class SlotAssignment:
    slots: dict[str, HandCandidate | None]
    ambiguous: bool


def _cost(
    candidates: list[HandCandidate],
    slots: dict[str, HandCandidate | None],
    previous: Mapping[str, Point],
) -> float:
    total = 0.0
    for slot, candidate in slots.items():
        previous_wrist = previous.get(slot)
        if candidate is None or candidate.wrist is None or previous_wrist is None:
            continue
        total += distance(candidate.wrist, previous_wrist) ** 2
    return total


def assign_slots(
    candidates: list[HandCandidate],
    previous: Mapping[str, Point] | None,
) -> SlotAssignment:
    """Assign candidates to ``left``/``right`` using the documented rules.

    ``previous`` maps a slot to its previous wrist position in feature space.
    Slots absent from the mapping were empty or were cleared by a gap.
    """
    if len(candidates) > 2:
        raise ValueError("features-v1 accepts at most two hands per frame")

    if not candidates:
        return SlotAssignment({"left": None, "right": None}, ambiguous=False)

    if len(candidates) == 1:
        candidate = candidates[0]
        if candidate.handedness in SLOTS:
            slots: dict[str, HandCandidate | None] = {"left": None, "right": None}
            slots[candidate.handedness] = candidate
            return SlotAssignment(slots, ambiguous=False)
        if previous and len(previous) == 1:
            slot = next(iter(previous))
            slots = {"left": None, "right": None}
            slots[slot] = candidate
            return SlotAssignment(slots, ambiguous=True)
        return SlotAssignment({"left": candidate, "right": None}, ambiguous=True)

    labels = {candidate.handedness for candidate in candidates}
    if labels == {"left", "right"}:
        slots = {"left": None, "right": None}
        for candidate in candidates:
            slots[candidate.handedness] = candidate
        return SlotAssignment(slots, ambiguous=False)

    first, second = candidates
    direct = {"left": first, "right": second}
    swapped = {"left": second, "right": first}
    if previous:
        direct_cost = _cost(candidates, direct, previous)
        swapped_cost = _cost(candidates, swapped, previous)
        if swapped_cost < direct_cost:
            return SlotAssignment(swapped, ambiguous=True)
    return SlotAssignment(direct, ambiguous=True)
