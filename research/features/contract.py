"""The normative, machine-readable features-v1 contract.

The Markdown specification in ``docs/spec/features-v1.md`` defines the
mathematics. This module defines the exact channel order and constants that
both the Python and Kotlin implementations must reproduce.

Changing anything here is an incompatible contract change. It requires a new
version, regenerated golden vectors and passing parity tests.
"""

from __future__ import annotations

import hashlib
import json
import re
from typing import Any

CONTRACT_NAME = "hasta.features"
VERSION = "features-v1"
FEATURE_DIM = 256

LANDMARK_COUNT = 21
HAND_SLOTS = ("left", "right")

# Proposed, fixture-tested normalisation constants. See the spec for rationale.
PALM_SCALE_EPSILON = 1e-6
VECTOR_EPSILON = 1e-9
GAP_THRESHOLD_MS = 600
ATOL = 1e-6
RTOL = 1e-5

FINGERS: tuple[tuple[str, tuple[int, int, int, int]], ...] = (
    ("thumb", (1, 2, 3, 4)),
    ("index", (5, 6, 7, 8)),
    ("middle", (9, 10, 11, 12)),
    ("ring", (13, 14, 15, 16)),
    ("pinky", (17, 18, 19, 20)),
)

TIP_SPREAD_PAIRS: tuple[tuple[str, str], ...] = (
    ("thumb", "index"),
    ("index", "middle"),
    ("middle", "ring"),
    ("ring", "pinky"),
)

_NAME_PATTERN = re.compile(r"^[a-z][a-z0-9_.]*$")


def _header_channels() -> list[str]:
    return [
        "header.dt_s",
        "header.elapsed_s",
        "header.frame_gap",
        "header.first_frame",
        "header.left_present",
        "header.right_present",
        "header.both_present",
        "header.slot_assignment_ambiguous",
        "header.left_handedness_known",
        "header.right_handedness_known",
        "header.left_local_valid",
        "header.right_local_valid",
        "header.left_global_valid",
        "header.right_global_valid",
        "header.left_temporal_valid",
        "header.right_temporal_valid",
    ]


def _validity_channels() -> list[str]:
    channels: list[str] = []
    for slot in HAND_SLOTS:
        channels.extend(f"validity.{slot}.{index:02d}" for index in range(LANDMARK_COUNT))
    return channels


def _local_channels(slot: str) -> list[str]:
    channels: list[str] = []
    for index in range(1, LANDMARK_COUNT):
        channels.append(f"{slot}.local.wrist_rel.{index:02d}.u")
        channels.append(f"{slot}.local.wrist_rel.{index:02d}.v")
    for finger, _ in FINGERS:
        for joint in (1, 2, 3):
            channels.append(f"{slot}.local.angle.{finger}.j{joint}")
    for finger, _ in FINGERS:
        for segment in (1, 2, 3):
            channels.append(f"{slot}.local.bone.{finger}.{segment}")
    for first, second in TIP_SPREAD_PAIRS:
        channels.append(f"{slot}.local.tip_spread.{first}_{second}")
    for finger, _ in FINGERS:
        channels.append(f"{slot}.local.extension.{finger}")
    channels.append(f"{slot}.local.thumb_index_opposition")
    channels.append(f"{slot}.local.palm_aspect")
    return channels


def _global_channels(slot: str) -> list[str]:
    return [
        f"{slot}.global.centre.u",
        f"{slot}.global.centre.v",
        f"{slot}.global.wrist.u",
        f"{slot}.global.wrist.v",
        f"{slot}.global.orientation.cos",
        f"{slot}.global.orientation.sin",
        f"{slot}.global.palm_scale",
    ]


def _temporal_channels(slot: str) -> list[str]:
    return [
        f"{slot}.temporal.disp.u",
        f"{slot}.temporal.disp.v",
        f"{slot}.temporal.vel.u",
        f"{slot}.temporal.vel.v",
        f"{slot}.temporal.acc.u",
        f"{slot}.temporal.acc.v",
        f"{slot}.temporal.angular_velocity",
        f"{slot}.temporal.has_previous",
    ]


def _inter_hand_channels() -> list[str]:
    return [
        "inter_hand.rel_centre.u",
        "inter_hand.rel_centre.v",
        "inter_hand.distance",
        "inter_hand.rel_orientation.cos",
        "inter_hand.rel_orientation.sin",
        "inter_hand.log_scale_ratio",
    ]


def _group(
    name: str,
    channels: list[str],
    offset: int,
    rule: str,
    units: str,
) -> dict[str, Any]:
    return {
        "name": name,
        "offset": offset,
        "length": len(channels),
        "channels": channels,
        "rule": rule,
        "units": units,
    }


def _build_groups() -> tuple[dict[str, Any], ...]:
    groups: list[dict[str, Any]] = []
    cursor = 0

    def add(name: str, channels: list[str], rule: str, units: str) -> None:
        nonlocal cursor
        groups.append(_group(name, channels, cursor, rule, units))
        cursor += len(channels)

    add(
        "header",
        _header_channels(),
        "Per-frame timing, presence, slot and group-validity flags.",
        "seconds / boolean",
    )
    add(
        "validity",
        _validity_channels(),
        "Input landmark validity copied into the feature vector; 1 valid, 0 missing.",
        "boolean",
    )
    for slot in HAND_SLOTS:
        add(
            f"{slot}.local",
            _local_channels(slot),
            "Palm-scale normalised handshape geometry for the canonical slot.",
            "dimensionless",
        )
        add(
            f"{slot}.global",
            _global_channels(slot),
            "Image-position, orientation and palm scale for the canonical slot.",
            "image-width fraction",
        )
        add(
            f"{slot}.temporal",
            _temporal_channels(slot),
            "Causal wrist motion and palm angular velocity for the canonical slot.",
            "image-width fraction / second",
        )
    add(
        "inter_hand",
        _inter_hand_channels(),
        "Relative position, distance, orientation and scale between the two slots.",
        "image-width fraction",
    )
    return tuple(groups)


FEATURE_GROUPS: tuple[dict[str, Any], ...] = _build_groups()
FEATURE_NAMES: tuple[str, ...] = tuple(
    channel for group in FEATURE_GROUPS for channel in group["channels"]
)


def build_layout() -> list[dict[str, Any]]:
    """Return the flat, ordered channel layout."""
    layout: list[dict[str, Any]] = []
    for group in FEATURE_GROUPS:
        for offset, channel in enumerate(group["channels"]):
            layout.append(
                {
                    "index": group["offset"] + offset,
                    "name": channel,
                    "group": group["name"],
                }
            )
    return layout


def build_contract() -> dict[str, Any]:
    """Return the complete machine-readable contract as a plain dictionary."""
    return {
        "contract": CONTRACT_NAME,
        "version": VERSION,
        "status": "locked",
        "language_scope": ["python", "kotlin"],
        "landmark_schema": {
            "name": "mediapipe_hand_21",
            "count": LANDMARK_COUNT,
            "indices": {
                "wrist": 0,
                "thumb": [1, 2, 3, 4],
                "index": [5, 6, 7, 8],
                "middle": [9, 10, 11, 12],
                "ring": [13, 14, 15, 16],
                "pinky": [17, 18, 19, 20],
            },
            "image_coordinate_space": "image_normalized_upright",
            "x_unit": "image_width_fraction",
            "y_unit": "image_height_fraction",
            "z_unit": "wrist_relative_image_x_scale",
            "world_coordinate_space": "metres_hand_geometric_centre",
            "world_landmarks_used_by_features": False,
        },
        "coordinate_conventions": {
            "feature_space": "image_isotropic_upright_unmirrored",
            "u": "x",
            "v": "y * (image_height / image_width)",
            "units": "image_width_fraction",
            "mirroring": "if mirrored_input: x := 1 - x and swap left/right handedness labels",
            "rotation": "landmarks must already be upright; rotation_degrees is provenance only",
            "display_mirror_applied": False,
        },
        "hand_slots": {
            "order": ["left", "right"],
            "identity": "subject_relative_handedness",
            "assignment": "distinct labels first, then continuity, then input order",
            "ambiguous_flag": "header.slot_assignment_ambiguous",
        },
        "normalization": {
            "palm_scale": "0.5 * (||p5 - p0|| + ||p17 - p0||)",
            "palm_scale_epsilon": PALM_SCALE_EPSILON,
            "vector_epsilon": VECTOR_EPSILON,
            "angle": "atan2(abs(cross(a, b)), dot(a, b))",
            "angle_range": [0.0, 3.141592653589793],
            "distance": "sqrt(du * du + dv * dv)",
            "clipping": "none",
            "hand_centre": "mean(p0, p5, p9, p13, p17)",
            "orientation": "unit vector p0 -> p9 as (cos, sin)",
            "extension_ratio": "||tip - chain_start|| / (bone1 + bone2 + bone3)",
            "palm_aspect": "||p5 - p17|| / ||p0 - p9||",
        },
        "temporal_policy": {
            "timestamp_unit": "milliseconds",
            "clock": "monotonic",
            "gap_threshold_ms": GAP_THRESHOLD_MS,
            "derivative": "forward difference over actual dt",
            "acceleration": "(velocity_t - velocity_previous) / dt_t",
            "angular_wrap": "atan2(sin(delta), cos(delta))",
            "irregular_intervals": "use actual dt; no resampling",
            "non_monotonic_timestamps": "treat as gap; derivatives zero; reset state",
            "first_frame": "dt_s=0, elapsed_s=0, derivatives zero, has_previous=0",
        },
        "missingness_policy": {
            "absent_hand": "present=0; geometry zero; masks zero",
            "invalid_landmark": "dependent group zeroed; group validity 0",
            "imputation": "none",
            "nan_or_inf": "forbidden; input rejected",
            "zero_fill_is_valid_only_with_mask": True,
        },
        "numeric_precision": {
            "compute": "float64",
            "golden_storage": "float64",
            "device_storage": "float32 permitted after float64 computation",
            "atol": ATOL,
            "rtol": RTOL,
            "mask_comparison": "exact",
        },
        "feature_dim": FEATURE_DIM,
        "groups": [dict(group) for group in FEATURE_GROUPS],
    }


def canonical_json(value: Any) -> str:
    """Canonical JSON used for hashing and drift detection."""
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


def contract_sha256(contract: dict[str, Any] | None = None) -> str:
    payload = build_contract() if contract is None else contract
    return hashlib.sha256(canonical_json(payload).encode("utf-8")).hexdigest()


def validate_contract(contract: dict[str, Any]) -> None:
    """Semantic validation that JSON Schema cannot express."""
    if contract.get("contract") != CONTRACT_NAME:
        raise ValueError(f"contract must be {CONTRACT_NAME!r}")
    if contract.get("version") != VERSION:
        raise ValueError(f"version must be {VERSION!r}")
    if contract.get("feature_dim") != FEATURE_DIM:
        raise ValueError(f"feature_dim must be {FEATURE_DIM}")

    groups = contract.get("groups")
    if not isinstance(groups, list) or not groups:
        raise ValueError("groups must be a non-empty list")

    seen_names: set[str] = set()
    seen_groups: set[str] = set()
    cursor = 0
    for group in groups:
        name = group.get("name")
        if not isinstance(name, str) or not name:
            raise ValueError("group name must be a non-empty string")
        if name in seen_groups:
            raise ValueError(f"duplicate group {name!r}")
        seen_groups.add(name)

        offset = group.get("offset")
        if offset != cursor:
            raise ValueError(
                f"group {name!r} offset {offset!r} does not continue the layout at {cursor}"
            )
        channels = group.get("channels")
        if not isinstance(channels, list) or not channels:
            raise ValueError(f"group {name!r} must have channels")
        if group.get("length") != len(channels):
            raise ValueError(f"group {name!r} length does not match its channels")
        for channel in channels:
            if not isinstance(channel, str) or not _NAME_PATTERN.match(channel):
                raise ValueError(f"invalid channel name {channel!r} in {name!r}")
            if channel in seen_names:
                raise ValueError(f"duplicate channel {channel!r}")
            seen_names.add(channel)
        cursor += len(channels)

    if cursor != FEATURE_DIM:
        raise ValueError(f"layout covers {cursor} channels, expected {FEATURE_DIM}")


__all__ = [
    "ATOL",
    "CONTRACT_NAME",
    "FEATURE_DIM",
    "FEATURE_GROUPS",
    "FEATURE_NAMES",
    "FINGERS",
    "GAP_THRESHOLD_MS",
    "HAND_SLOTS",
    "LANDMARK_COUNT",
    "PALM_SCALE_EPSILON",
    "RTOL",
    "TIP_SPREAD_PAIRS",
    "VECTOR_EPSILON",
    "VERSION",
    "build_contract",
    "build_layout",
    "canonical_json",
    "contract_sha256",
    "validate_contract",
]
