"""Deterministic features-v1 golden-vector generation.

The golden vectors are generated from the Python reference implementation and
committed. They are never hand-edited. The Kotlin source is generated from the
same case list so the Android parity test needs no JSON dependency.
"""

from __future__ import annotations

import json
import math
from pathlib import Path

from .contract import ATOL, CONTRACT_NAME, FEATURE_DIM, RTOL, VERSION
from .extractor import FeatureExtractor
from .types import FrameObservation, HandObservation, Landmark

Point = tuple[float, float]

REPO_ROOT = Path(__file__).resolve().parents[2]
JSON_PATH = REPO_ROOT / "contracts" / "golden_vectors" / "features_v1" / "cases.json"
KOTLIN_PATH = (
    REPO_ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "com"
    / "hasta"
    / "app"
    / "domain"
    / "features"
    / "FeatureGoldenVectors.kt"
)

_BASE_OFFSETS: tuple[tuple[float, float], ...] = (
    (0.00, 0.00),
    (-0.030, -0.020),
    (-0.050, -0.040),
    (-0.060, -0.060),
    (-0.070, -0.080),
    (-0.020, -0.100),
    (-0.020, -0.140),
    (-0.020, -0.170),
    (-0.020, -0.200),
    (0.000, -0.110),
    (0.000, -0.160),
    (0.000, -0.200),
    (0.000, -0.230),
    (0.020, -0.100),
    (0.020, -0.150),
    (0.020, -0.180),
    (0.020, -0.210),
    (0.040, -0.080),
    (0.040, -0.120),
    (0.040, -0.150),
    (0.040, -0.170),
)


def synthetic_points(
    wrist: Point = (0.5, 0.7),
    scale: float = 1.0,
    rotation: float = 0.0,
) -> list[list[float]]:
    cosine = math.cos(rotation)
    sine = math.sin(rotation)
    points: list[list[float]] = []
    for dx, dy in _BASE_OFFSETS:
        rotated_x = dx * cosine - dy * sine
        rotated_y = dx * sine + dy * cosine
        points.append(
            [
                wrist[0] + rotated_x * scale,
                wrist[1] + rotated_y * scale,
                0.0,
            ]
        )
    return points


def _hand(
    handedness: str | None,
    points: list[list[float]],
    validity: list[bool] | None = None,
) -> dict:
    return {
        "handedness": handedness,
        "handednessScore": 0.95 if handedness is not None else None,
        "landmarks": points,
        "landmarkValidity": validity,
    }


def _frame(
    timestamp_ms: int,
    hands: list[dict],
    width: int = 640,
    height: int = 480,
    mirrored_input: bool = False,
) -> dict:
    return {
        "timestampMs": timestamp_ms,
        "width": width,
        "height": height,
        "mirroredInput": mirrored_input,
        "hands": hands,
    }


def _validity_with(index: int, valid: bool) -> list[bool]:
    validity = [True] * 21
    validity[index] = valid
    return validity


def _build_case(name: str, frames: list[dict]) -> dict:
    extractor = FeatureExtractor()
    expected: list[list[float]] = []
    for frame in frames:
        observation = FrameObservation(
            timestamp_ms=frame["timestampMs"],
            width=frame["width"],
            height=frame["height"],
            mirrored_input=frame["mirroredInput"],
            hands=tuple(
                HandObservation(
                    handedness=hand["handedness"],
                    landmarks=tuple(
                        Landmark(x=landmark[0], y=landmark[1], z=landmark[2])
                        for landmark in hand["landmarks"]
                    ),
                    handedness_score=hand["handednessScore"],
                    landmark_validity=(
                        tuple(hand["landmarkValidity"])
                        if hand["landmarkValidity"] is not None
                        else None
                    ),
                )
                for hand in frame["hands"]
            ),
        )
        vector = extractor.extract(observation)
        if vector.shape != (FEATURE_DIM,):
            raise AssertionError("unexpected feature dimension")
        expected.append([float(value) for value in vector])
    return {"name": name, "frames": frames, "expected": expected}


def build_cases() -> list[dict]:
    cases: list[dict] = []

    cases.append(
        _build_case(
            "first_frame_right",
            [_frame(1000, [_hand("right", synthetic_points())])],
        )
    )

    cases.append(
        _build_case(
            "steady_motion",
            [
                _frame(0, [_hand("right", synthetic_points(wrist=(0.40, 0.70)))]),
                _frame(100, [_hand("right", synthetic_points(wrist=(0.50, 0.70)))]),
                _frame(200, [_hand("right", synthetic_points(wrist=(0.60, 0.70)))]),
            ],
        )
    )

    cases.append(
        _build_case(
            "acceleration",
            [
                _frame(0, [_hand("right", synthetic_points(wrist=(0.40, 0.70)))]),
                _frame(100, [_hand("right", synthetic_points(wrist=(0.50, 0.70)))]),
                _frame(200, [_hand("right", synthetic_points(wrist=(0.70, 0.70)))]),
            ],
        )
    )

    cases.append(
        _build_case(
            "gap",
            [
                _frame(0, [_hand("right", synthetic_points(wrist=(0.40, 0.70)))]),
                _frame(100, [_hand("right", synthetic_points(wrist=(0.50, 0.70)))]),
                _frame(800, [_hand("right", synthetic_points(wrist=(0.60, 0.70)))]),
            ],
        )
    )

    cases.append(
        _build_case(
            "non_monotonic",
            [
                _frame(1000, [_hand("right", synthetic_points())]),
                _frame(900, [_hand("right", synthetic_points(wrist=(0.55, 0.70)))]),
            ],
        )
    )

    cases.append(
        _build_case(
            "left_only",
            [_frame(0, [_hand("left", synthetic_points(wrist=(0.30, 0.70)))])],
        )
    )

    cases.append(
        _build_case(
            "right_only",
            [_frame(0, [_hand("right", synthetic_points(wrist=(0.70, 0.70)))])],
        )
    )

    cases.append(
        _build_case(
            "both_hands",
            [
                _frame(
                    0,
                    [
                        _hand("left", synthetic_points(wrist=(0.30, 0.70))),
                        _hand("right", synthetic_points(wrist=(0.70, 0.70))),
                    ],
                )
            ],
        )
    )

    cases.append(
        _build_case(
            "absent_hand",
            [
                _frame(0, [_hand("right", synthetic_points())]),
                _frame(100, []),
            ],
        )
    )

    cases.append(
        _build_case(
            "invalid_landmark",
            [
                _frame(
                    0,
                    [
                        _hand(
                            "right",
                            synthetic_points(),
                            validity=_validity_with(20, False),
                        )
                    ],
                )
            ],
        )
    )

    cases.append(
        _build_case(
            "degenerate_palm_scale",
            [
                _frame(
                    0,
                    [_hand("right", [[0.5, 0.5, 0.0] for _ in range(21)])],
                )
            ],
        )
    )

    cases.append(
        _build_case(
            "mirrored_input",
            [
                _frame(
                    0,
                    [_hand("left", synthetic_points(wrist=(0.80, 0.70)))],
                    mirrored_input=True,
                )
            ],
        )
    )

    cases.append(
        _build_case(
            "ambiguous_slots",
            [
                _frame(
                    0,
                    [
                        _hand("left", synthetic_points(wrist=(0.30, 0.70))),
                        _hand("left", synthetic_points(wrist=(0.70, 0.70))),
                    ],
                ),
                _frame(
                    100,
                    [
                        _hand("left", synthetic_points(wrist=(0.70, 0.70))),
                        _hand("left", synthetic_points(wrist=(0.30, 0.70))),
                    ],
                ),
            ],
        )
    )

    cases.append(
        _build_case(
            "unknown_handedness",
            [_frame(0, [_hand(None, synthetic_points(wrist=(0.40, 0.70)))])],
        )
    )

    cases.append(
        _build_case(
            "landscape_aspect",
            [
                _frame(
                    0,
                    [_hand("right", synthetic_points(wrist=(0.50, 0.50)))],
                    width=1280,
                    height=720,
                )
            ],
        )
    )

    cases.append(
        _build_case(
            "rotating_hand",
            [
                _frame(0, [_hand("right", synthetic_points(rotation=0.0))]),
                _frame(100, [_hand("right", synthetic_points(rotation=0.4))]),
            ],
        )
    )

    cases.append(
        _build_case(
            "hand_absence_clears_temporal",
            [
                _frame(0, [_hand("right", synthetic_points(wrist=(0.40, 0.70)))]),
                _frame(100, [_hand("right", synthetic_points(wrist=(0.50, 0.70)))]),
                _frame(200, []),
                _frame(300, [_hand("right", synthetic_points(wrist=(0.60, 0.70)))]),
            ],
        )
    )

    cases.append(
        _build_case(
            "invalid_wrist_landmark",
            [
                _frame(0, [_hand("right", synthetic_points())]),
                _frame(
                    100,
                    [
                        _hand(
                            "right",
                            synthetic_points(wrist=(0.55, 0.70)),
                            validity=_validity_with(0, False),
                        )
                    ],
                ),
            ],
        )
    )

    return cases


def build_document() -> dict:
    return {
        "contract": CONTRACT_NAME,
        "version": VERSION,
        "atol": ATOL,
        "rtol": RTOL,
        "cases": build_cases(),
    }


def canonical_json(value: object) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


def render_json(document: dict | None = None) -> str:
    payload = build_document() if document is None else document
    return canonical_json(payload) + "\n"


def _kotlin_double(value: float) -> str:
    if not math.isfinite(value):
        raise ValueError("golden vectors must be finite")
    text = repr(float(value))
    if "e" in text:
        mantissa, exponent = text.split("e")
        if "." not in mantissa:
            mantissa = mantissa + ".0"
        text = f"{mantissa}e{exponent}"
    return text


def _kotlin_bool_list(values: list[bool] | None) -> str:
    if values is None:
        return "null"
    return "listOf(" + ", ".join("true" if value else "false" for value in values) + ")"


def _kotlin_hand(hand: dict) -> list[str]:
    lines = ["Hand("]
    lines.append(
        f'    handedness = {json.dumps(hand["handedness"]) if hand["handedness"] is not None else "null"},'
    )
    score = hand["handednessScore"]
    lines.append(
        "    handednessScore = "
        + ("null" if score is None else _kotlin_double(score))
        + ","
    )
    lines.append("    landmarks = listOf(")
    for landmark in hand["landmarks"]:
        lines.append(
            "        Landmark("
            + ", ".join(_kotlin_double(value) for value in landmark)
            + "),"
        )
    lines.append("    ),")
    lines.append(f"    landmarkValidity = {_kotlin_bool_list(hand['landmarkValidity'])},")
    lines.append("),")
    return lines


def _kotlin_frame(frame: dict) -> list[str]:
    lines = ["Frame("]
    lines.append(f'    timestampMs = {frame["timestampMs"]}L,')
    lines.append(f'    width = {frame["width"]},')
    lines.append(f'    height = {frame["height"]},')
    lines.append(
        f'    mirroredInput = {"true" if frame["mirroredInput"] else "false"},'
    )
    lines.append("    hands = listOf(")
    for hand in frame["hands"]:
        lines.extend("        " + line for line in _kotlin_hand(hand))
    lines.append("    ),")
    lines.append("),")
    return lines


def _kotlin_vector(values: list[float]) -> list[str]:
    lines = ["doubleArrayOf("]
    for start in range(0, len(values), 8):
        chunk = values[start : start + 8]
        lines.append("    " + ", ".join(_kotlin_double(value) for value in chunk) + ",")
    lines.append("),")
    return lines


def _kotlin_case(case: dict) -> list[str]:
    lines = ["Case("]
    lines.append(f'    name = {json.dumps(case["name"])},')
    lines.append("    frames = listOf(")
    for frame in case["frames"]:
        lines.extend("        " + line for line in _kotlin_frame(frame))
    lines.append("    ),")
    lines.append("    expected = listOf(")
    for vector in case["expected"]:
        lines.extend("        " + line for line in _kotlin_vector(vector))
    lines.append("    ),")
    lines.append("),")
    return lines


def render_kotlin(document: dict | None = None) -> str:
    payload = build_document() if document is None else document
    lines = [
        "// Generated by research/scripts/generate_golden_vectors.py.",
        "// Do not edit by hand. Regenerate and review the diff instead.",
        "package com.hasta.app.domain.features",
        "",
        "object FeatureGoldenVectors {",
        f"    const val ATOL = {_kotlin_double(payload['atol'])}",
        f"    const val RTOL = {_kotlin_double(payload['rtol'])}",
        "",
        "    data class Landmark(val x: Double, val y: Double, val z: Double)",
        "",
        "    data class Hand(",
        "        val handedness: String?,",
        "        val handednessScore: Double?,",
        "        val landmarks: List<Landmark>?,",
        "        val landmarkValidity: List<Boolean>?,",
        "    )",
        "",
        "    data class Frame(",
        "        val timestampMs: Long,",
        "        val width: Int,",
        "        val height: Int,",
        "        val mirroredInput: Boolean,",
        "        val hands: List<Hand>,",
        "    )",
        "",
        "    data class Case(",
        "        val name: String,",
        "        val frames: List<Frame>,",
        "        val expected: List<DoubleArray>,",
        "    )",
        "",
        "    val cases: List<Case> = listOf(",
    ]
    for case in payload["cases"]:
        lines.extend("        " + line for line in _kotlin_case(case))
    lines.extend(["    )", "}", ""])
    return "\n".join(lines)


def write_outputs() -> tuple[Path, Path]:
    document = build_document()
    JSON_PATH.parent.mkdir(parents=True, exist_ok=True)
    KOTLIN_PATH.parent.mkdir(parents=True, exist_ok=True)
    JSON_PATH.write_text(render_json(document), encoding="utf-8")
    KOTLIN_PATH.write_text(render_kotlin(document), encoding="utf-8")
    return JSON_PATH, KOTLIN_PATH
