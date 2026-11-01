import json

import numpy as np
import pytest

from features.contract import ATOL, RTOL
from features.extractor import FeatureExtractor
from features.golden import (
    JSON_PATH,
    KOTLIN_PATH,
    build_document,
    render_json,
    render_kotlin,
)
from features.types import FrameObservation, HandObservation, Landmark

REQUIRED_CASES = {
    "first_frame_right",
    "steady_motion",
    "acceleration",
    "gap",
    "non_monotonic",
    "left_only",
    "right_only",
    "both_hands",
    "absent_hand",
    "invalid_landmark",
    "degenerate_palm_scale",
    "extreme_small_scale",
    "extreme_large_scale",
    "mirrored_input",
    "ambiguous_slots",
    "unknown_handedness",
    "landscape_aspect",
    "rotating_hand",
    "hand_absence_clears_temporal",
    "invalid_wrist_landmark",
}


def replay(case):
    extractor = FeatureExtractor()
    actual = []
    for frame in case["frames"]:
        observation = FrameObservation(
            timestamp_ms=frame["timestampMs"],
            width=frame["width"],
            height=frame["height"],
            mirrored_input=frame["mirroredInput"],
            hands=tuple(
                HandObservation(
                    handedness=hand["handedness"],
                    landmarks=tuple(
                        Landmark(landmark[0], landmark[1], landmark[2])
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
        actual.append(extractor.extract(observation))
    return actual


def test_golden_document_is_deterministic():
    assert build_document() == build_document()


def test_golden_cases_cover_the_required_scenarios():
    names = {case["name"] for case in build_document()["cases"]}
    assert REQUIRED_CASES <= names


def test_golden_json_is_committed_and_canonical():
    assert JSON_PATH.exists(), "run scripts/generate_golden_vectors.py"
    stored = JSON_PATH.read_text(encoding="utf-8")
    assert stored == render_json(json.loads(stored))
    assert json.loads(stored) == build_document()


def test_golden_kotlin_is_committed_and_current():
    assert KOTLIN_PATH.exists(), "run scripts/generate_golden_vectors.py"
    stored = KOTLIN_PATH.read_text(encoding="utf-8")
    assert stored == render_kotlin(build_document())


def test_golden_vectors_are_finite_and_correctly_sized():
    document = build_document()
    for case in document["cases"]:
        for vector in case["expected"]:
            assert len(vector) == 256
            assert all(np.isfinite(value) for value in vector)


@pytest.mark.parametrize("case", build_document()["cases"], ids=lambda case: case["name"])
def test_python_reference_reproduces_every_golden_case(case):
    expected = [np.asarray(vector, dtype=np.float64) for vector in case["expected"]]
    actual = replay(case)
    assert len(actual) == len(expected)
    for frame_index, (got, want) in enumerate(zip(actual, expected)):
        assert np.allclose(got, want, atol=ATOL, rtol=RTOL), (
            f"{case['name']} frame {frame_index}"
        )
