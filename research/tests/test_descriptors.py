import math

import pytest

from features.descriptors import (
    FINGER_CHAINS,
    global_features,
    inter_hand_features,
    local_features,
    palm_scale,
)
from features.geometry import angle_between, distance, subtract

from conftest import hand_points


def points():
    return [(p.x, p.y) for p in hand_points()]


def test_finger_chains_are_the_mediapipe_order():
    assert [name for name, _ in FINGER_CHAINS] == [
        "thumb",
        "index",
        "middle",
        "ring",
        "pinky",
    ]
    assert FINGER_CHAINS[0][1] == (1, 2, 3, 4)
    assert FINGER_CHAINS[-1][1] == (17, 18, 19, 20)


def test_palm_scale_is_the_mean_of_two_palm_distances():
    p = points()
    expected = 0.5 * (distance(p[5], p[0]) + distance(p[17], p[0]))
    assert palm_scale(p) == pytest.approx(expected)


def test_local_group_has_eighty_one_finite_channels():
    p = points()
    values = local_features(p, palm_scale(p))
    assert len(values) == 81
    assert all(math.isfinite(value) for value in values)


def test_wrist_relative_coordinates_are_palm_normalized():
    p = points()
    s = palm_scale(p)
    values = local_features(p, s)
    assert values[0] == pytest.approx((p[1][0] - p[0][0]) / s)
    assert values[1] == pytest.approx((p[1][1] - p[0][1]) / s)
    # landmark 20 is the last wrist-relative pair
    assert values[38] == pytest.approx((p[20][0] - p[0][0]) / s)
    assert values[39] == pytest.approx((p[20][1] - p[0][1]) / s)


def test_joint_angles_are_in_range():
    p = points()
    values = local_features(p, palm_scale(p))
    angles = values[40:55]
    assert all(0.0 <= angle <= math.pi for angle in angles)
    # this synthetic hand is straight, so the index PIP and DIP angles are near pi
    assert angles[4] == pytest.approx(math.pi, abs=1e-3)


def test_index_mcp_angle_uses_the_wrist_not_the_thumb():
    p = points()
    values = local_features(p, palm_scale(p))
    expected = angle_between(
        subtract(p[0], p[5]),
        subtract(p[6], p[5]),
        1e-9,
    )
    assert values[43] == pytest.approx(expected)


def test_bone_lengths_are_positive_and_normalized():
    p = points()
    s = palm_scale(p)
    values = local_features(p, s)
    bones = values[55:70]
    assert all(bone > 0.0 for bone in bones)
    assert bones[3] == pytest.approx(distance(p[6], p[5]) / s)


def test_tip_spreads_match_the_named_pairs():
    p = points()
    s = palm_scale(p)
    values = local_features(p, s)
    spreads = values[70:74]
    assert spreads[0] == pytest.approx(distance(p[4], p[8]) / s)
    assert spreads[3] == pytest.approx(distance(p[16], p[20]) / s)


def test_extensions_are_near_one_for_a_straight_hand():
    p = points()
    values = local_features(p, palm_scale(p))
    extensions = values[74:79]
    assert all(0.95 <= extension <= 1.05 for extension in extensions)


def test_opposition_and_palm_aspect_are_defined():
    p = points()
    s = palm_scale(p)
    values = local_features(p, s)
    assert values[79] == pytest.approx(distance(p[4], p[8]) / s)
    assert values[80] == pytest.approx(distance(p[5], p[17]) / distance(p[0], p[9]))


def test_global_group_has_seven_channels_and_consistent_orientation():
    p = points()
    s = palm_scale(p)
    result = global_features(p, s)
    values = result.values
    assert len(values) == 7
    assert values[0] == pytest.approx(sum(p[i][0] for i in (0, 5, 9, 13, 17)) / 5)
    assert values[1] == pytest.approx(sum(p[i][1] for i in (0, 5, 9, 13, 17)) / 5)
    assert values[2] == pytest.approx(p[0][0])
    assert values[3] == pytest.approx(p[0][1])
    assert values[4] ** 2 + values[5] ** 2 == pytest.approx(1.0)
    assert values[6] == pytest.approx(s)


def test_degenerate_orientation_falls_back_to_one_zero():
    p = list(points())
    p[9] = p[0]
    result = global_features(p, palm_scale(p))
    assert result.orientation_defined is False
    assert result.values[4] == 1.0
    assert result.values[5] == 0.0


def test_inter_hand_group_is_symmetric_in_distance():
    left = global_features(points(), palm_scale(points()))
    right = global_features(
        [(x + 0.2, y) for x, y in points()],
        palm_scale([(x + 0.2, y) for x, y in points()]),
    )
    values = inter_hand_features(left, right)
    assert len(values) == 6
    assert values[0] == pytest.approx(0.2)
    assert values[1] == pytest.approx(0.0)
    assert values[2] == pytest.approx(0.2)
    assert values[3] ** 2 + values[4] ** 2 == pytest.approx(1.0)
    assert values[5] == pytest.approx(0.0)


def test_inter_hand_log_scale_ratio_uses_palm_scales():
    small = global_features(points(), 0.1)
    large = global_features(points(), 0.2)
    values = inter_hand_features(small, large)
    assert values[5] == pytest.approx(math.log(2.0))
