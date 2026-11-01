import math

import pytest

from features.contract import build_layout
from features.extractor import FeatureExtractor
from features.types import FrameObservation, HandObservation, Landmark

from conftest import make_hand

INDEX = {channel["name"]: channel["index"] for channel in build_layout()}


def at(name: str, vector) -> float:
    return float(vector[INDEX[name]])


def frame(timestamp_ms=1000, hands=(), width=100, height=100, mirrored=False):
    return FrameObservation(
        timestamp_ms=timestamp_ms,
        width=width,
        height=height,
        hands=tuple(hands),
        mirrored_input=mirrored,
    )


def test_first_frame_header_and_group_validity():
    extractor = FeatureExtractor()
    vector = extractor.extract(frame(hands=[make_hand("right")]))

    assert vector.shape == (256,)
    assert vector.dtype.name == "float64"
    assert all(math.isfinite(value) for value in vector)

    assert at("header.dt_s", vector) == 0.0
    assert at("header.elapsed_s", vector) == 0.0
    assert at("header.frame_gap", vector) == 0.0
    assert at("header.first_frame", vector) == 1.0
    assert at("header.left_present", vector) == 0.0
    assert at("header.right_present", vector) == 1.0
    assert at("header.both_present", vector) == 0.0
    assert at("header.slot_assignment_ambiguous", vector) == 0.0
    assert at("header.right_handedness_known", vector) == 1.0
    assert at("header.right_local_valid", vector) == 1.0
    assert at("header.right_global_valid", vector) == 1.0
    assert at("header.right_temporal_valid", vector) == 0.0

    assert at("validity.right.00", vector) == 1.0
    assert at("validity.right.20", vector) == 1.0
    assert at("validity.left.00", vector) == 0.0

    assert at("right.global.palm_scale", vector) > 0.0
    assert at("right.temporal.has_previous", vector) == 0.0
    assert at("inter_hand.distance", vector) == 0.0


def test_absent_hand_is_zero_and_masked():
    extractor = FeatureExtractor()
    vector = extractor.extract(frame(hands=[]))

    for channel in (
        "header.left_present",
        "header.right_present",
        "header.both_present",
        "header.left_local_valid",
        "header.right_local_valid",
        "header.left_global_valid",
        "header.right_global_valid",
        "header.left_temporal_valid",
        "header.right_temporal_valid",
    ):
        assert at(channel, vector) == 0.0

    for index in range(21):
        assert at(f"validity.left.{index:02d}", vector) == 0.0
        assert at(f"validity.right.{index:02d}", vector) == 0.0

    for group in ("left.local", "right.local", "left.global", "right.global",
                  "left.temporal", "right.temporal", "inter_hand"):
        for channel in INDEX:
            if channel.startswith(group + "."):
                assert at(channel, vector) == 0.0, channel


def test_invalid_landmark_zeroes_local_but_keeps_global():
    validity = [True] * 21
    validity[20] = False
    hand = make_hand("right", validity=tuple(validity))

    vector = FeatureExtractor().extract(frame(hands=[hand]))

    assert at("validity.right.20", vector) == 0.0
    assert at("validity.right.19", vector) == 1.0
    assert at("header.right_local_valid", vector) == 0.0
    assert at("header.right_global_valid", vector) == 1.0
    assert at("right.local.wrist_rel.20.u", vector) == 0.0
    assert at("right.global.palm_scale", vector) > 0.0


def test_gap_resets_temporal_state():
    extractor = FeatureExtractor()
    extractor.extract(frame(1000, [make_hand("right", wrist=(0.4, 0.7))]))
    second = extractor.extract(frame(1100, [make_hand("right", wrist=(0.5, 0.7))]))
    assert at("header.right_temporal_valid", second) == 1.0
    assert at("header.dt_s", second) == pytest.approx(0.1)

    third = extractor.extract(frame(1800, [make_hand("right", wrist=(0.6, 0.7))]))
    assert at("header.frame_gap", third) == 1.0
    assert at("header.dt_s", third) == 0.0
    assert at("header.elapsed_s", third) == 0.0
    assert at("header.right_temporal_valid", third) == 0.0
    assert at("right.temporal.has_previous", third) == 0.0


def test_non_monotonic_timestamp_is_a_gap():
    extractor = FeatureExtractor()
    extractor.extract(frame(1000, [make_hand("right")]))
    vector = extractor.extract(frame(900, [make_hand("right")]))

    assert at("header.frame_gap", vector) == 1.0
    assert at("header.first_frame", vector) == 0.0
    assert at("header.right_temporal_valid", vector) == 0.0


def test_mirrored_input_flips_x_and_swaps_labels():
    hand = make_hand("left", wrist=(0.8, 0.7))
    vector = FeatureExtractor().extract(frame(hands=[hand], mirrored=True))

    assert at("header.left_present", vector) == 0.0
    assert at("header.right_present", vector) == 1.0
    assert at("header.right_handedness_known", vector) == 1.0
    assert at("right.global.wrist.u", vector) == pytest.approx(0.2)
    assert at("right.global.wrist.v", vector) == pytest.approx(0.7)


def test_two_hands_produce_inter_hand_geometry():
    left = make_hand("left", wrist=(0.3, 0.7))
    right = make_hand("right", wrist=(0.7, 0.7))
    vector = FeatureExtractor().extract(frame(hands=[left, right]))

    assert at("header.both_present", vector) == 1.0
    assert at("header.slot_assignment_ambiguous", vector) == 0.0
    assert at("inter_hand.rel_centre.u", vector) == pytest.approx(0.4)
    assert at("inter_hand.rel_centre.v", vector) == pytest.approx(0.0)
    assert at("inter_hand.distance", vector) == pytest.approx(0.4)
    assert at("inter_hand.log_scale_ratio", vector) == pytest.approx(0.0)


def test_unknown_handedness_is_ambiguous_but_assigned():
    hand = make_hand(None, wrist=(0.4, 0.7))
    vector = FeatureExtractor().extract(frame(hands=[hand]))

    assert at("header.left_present", vector) == 1.0
    assert at("header.slot_assignment_ambiguous", vector) == 1.0
    assert at("header.left_handedness_known", vector) == 0.0


def test_slot_continuity_survives_detector_order_swaps():
    extractor = FeatureExtractor()
    first = make_hand("left", wrist=(0.3, 0.7))
    second = make_hand("left", wrist=(0.7, 0.7))
    extractor.extract(frame(1000, [first, second]))

    # Same ambiguous labels, but the detector now returns them in reverse order.
    third = make_hand("left", wrist=(0.7, 0.7))
    fourth = make_hand("left", wrist=(0.3, 0.7))
    vector = extractor.extract(frame(1100, [third, fourth]))

    assert at("header.slot_assignment_ambiguous", vector) == 1.0
    assert at("left.global.wrist.u", vector) == pytest.approx(0.3)
    assert at("right.global.wrist.u", vector) == pytest.approx(0.7)


def test_hand_absence_clears_temporal_history():
    extractor = FeatureExtractor()
    extractor.extract(frame(1000, [make_hand("right", wrist=(0.4, 0.7))]))
    extractor.extract(frame(1100, [make_hand("right", wrist=(0.5, 0.7))]))
    extractor.extract(frame(1200, []))

    vector = extractor.extract(frame(1300, [make_hand("right", wrist=(0.6, 0.7))]))
    assert at("header.right_temporal_valid", vector) == 0.0
    assert at("right.temporal.has_previous", vector) == 0.0


def test_elapsed_time_tracks_the_current_segment():
    extractor = FeatureExtractor()
    first = extractor.extract(frame(1000, [make_hand("right")]))
    second = extractor.extract(frame(1100, [make_hand("right")]))
    third = extractor.extract(frame(1300, [make_hand("right")]))

    assert at("header.elapsed_s", first) == 0.0
    assert at("header.elapsed_s", second) == pytest.approx(0.1)
    assert at("header.elapsed_s", third) == pytest.approx(0.3)


def test_degenerate_palm_scale_disables_local_geometry_only():
    point = Landmark(0.5, 0.5)
    hand = HandObservation(
        handedness="right",
        landmarks=tuple(point for _ in range(21)),
    )
    vector = FeatureExtractor().extract(frame(hands=[hand]))

    assert at("header.right_local_valid", vector) == 0.0
    assert at("header.right_global_valid", vector) == 1.0
    assert at("right.global.palm_scale", vector) == 0.0
    assert at("right.global.orientation.cos", vector) == 1.0
    assert at("right.global.orientation.sin", vector) == 0.0


def test_aspect_ratio_is_applied_to_feature_space():
    hand = make_hand("right", wrist=(0.5, 0.5))
    vector = FeatureExtractor().extract(frame(hands=[hand], width=200, height=100))
    assert at("right.global.wrist.u", vector) == pytest.approx(0.5)
    assert at("right.global.wrist.v", vector) == pytest.approx(0.25)


def test_non_finite_landmarks_are_rejected():
    hand = make_hand("right")
    bad = list(hand.landmarks)
    bad[3] = Landmark(float("nan"), 0.5)
    with pytest.raises(ValueError):
        FeatureExtractor().extract(
            frame(hands=[HandObservation("right", tuple(bad))])
        )


def test_more_than_two_hands_are_rejected():
    hands = [make_hand("left"), make_hand("right"), make_hand(None)]
    with pytest.raises(ValueError):
        FeatureExtractor().extract(frame(hands=hands))


def test_bad_landmark_count_is_rejected():
    with pytest.raises(ValueError):
        FeatureExtractor().extract(
            frame(hands=[HandObservation("right", (Landmark(0.1, 0.1),))])
        )


def test_bad_validity_length_is_rejected():
    with pytest.raises(ValueError):
        FeatureExtractor().extract(
            frame(hands=[make_hand("right", validity=(True, False))])
        )


def test_acceleration_is_computed_across_three_frames():
    extractor = FeatureExtractor()
    extractor.extract(frame(1000, [make_hand("right", wrist=(0.4, 0.7))]))
    second = extractor.extract(frame(1100, [make_hand("right", wrist=(0.5, 0.7))]))
    third = extractor.extract(frame(1200, [make_hand("right", wrist=(0.7, 0.7))]))

    assert at("right.temporal.vel.u", second) == pytest.approx(1.0)
    assert at("right.temporal.vel.u", third) == pytest.approx(2.0)
    assert at("right.temporal.acc.u", third) == pytest.approx(10.0)
