import math

import pytest

from features.coords import to_feature_point
from features.geometry import angle_between, distance, wrap_angle
from features.types import Landmark


def test_square_image_is_identity():
    assert to_feature_point(Landmark(0.25, 0.75), 100, 100, False) == (0.25, 0.75)


def test_landscape_image_keeps_isotropic_scale():
    assert to_feature_point(Landmark(0.25, 0.5), 200, 100, False) == (0.25, 0.25)


def test_mirrored_input_flips_x_only():
    assert to_feature_point(Landmark(0.25, 0.5), 100, 200, True) == (0.75, 1.0)


def test_invalid_dimensions_are_rejected():
    with pytest.raises(ValueError):
        to_feature_point(Landmark(0.1, 0.1), 0, 100, False)
    with pytest.raises(ValueError):
        to_feature_point(Landmark(0.1, 0.1), 100, -1, False)


def test_distance_is_euclidean():
    assert distance((0.0, 0.0), (3.0, 4.0)) == pytest.approx(5.0)


def test_angle_between_is_unsigned_and_bounded():
    assert angle_between((1.0, 0.0), (0.0, 1.0), 1e-9) == pytest.approx(math.pi / 2)
    assert angle_between((1.0, 0.0), (-1.0, 0.0), 1e-9) == pytest.approx(math.pi)
    assert angle_between((1.0, 0.0), (1.0, 0.0), 1e-9) == pytest.approx(0.0)


def test_angle_with_degenerate_vector_is_zero():
    assert angle_between((0.0, 0.0), (1.0, 1.0), 1e-9) == 0.0


def test_wrap_angle_uses_principal_branch():
    assert wrap_angle(3 * math.pi) == pytest.approx(math.pi)
    assert wrap_angle(-3 * math.pi) == pytest.approx(-math.pi)
    assert wrap_angle(0.25) == pytest.approx(0.25)
