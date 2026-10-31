import pytest

from features.geometry import wrap_angle
from features.temporal import TemporalSample, temporal_features


def sample(wrist=(0.0, 0.0), angle=0.0, defined=True, velocity=None, ts=0):
    return TemporalSample(
        wrist=wrist,
        orientation_angle=angle,
        orientation_defined=defined,
        velocity=velocity,
        timestamp_ms=ts,
    )


def test_without_previous_everything_is_zero_and_invalid():
    result = temporal_features(
        wrist=(1.0, 2.0),
        orientation_angle=0.5,
        orientation_defined=True,
        previous=None,
        dt_s=0.0,
        timestamp_ms=100,
    )
    assert result.valid is False
    assert result.values == (0.0,) * 8
    assert result.sample.velocity is None


def test_steady_displacement_produces_velocity():
    result = temporal_features(
        wrist=(1.0, 0.0),
        orientation_angle=0.0,
        orientation_defined=True,
        previous=sample(),
        dt_s=0.5,
        timestamp_ms=500,
    )
    assert result.valid is True
    assert result.values[0] == pytest.approx(1.0)
    assert result.values[1] == pytest.approx(0.0)
    assert result.values[2] == pytest.approx(2.0)
    assert result.values[3] == pytest.approx(0.0)
    assert result.values[4] == pytest.approx(0.0)
    assert result.values[5] == pytest.approx(0.0)
    assert result.values[7] == 1.0
    assert result.sample.velocity == pytest.approx((2.0, 0.0))


def test_acceleration_uses_previous_velocity():
    result = temporal_features(
        wrist=(1.0, 0.0),
        orientation_angle=0.0,
        orientation_defined=True,
        previous=sample(velocity=(0.0, 0.0)),
        dt_s=0.5,
        timestamp_ms=500,
    )
    assert result.values[4] == pytest.approx(4.0)


def test_missing_previous_velocity_yields_zero_acceleration():
    result = temporal_features(
        wrist=(1.0, 0.0),
        orientation_angle=0.0,
        orientation_defined=True,
        previous=sample(velocity=None),
        dt_s=0.5,
        timestamp_ms=500,
    )
    assert result.values[4] == 0.0


def test_angular_velocity_wraps_across_pi():
    result = temporal_features(
        wrist=(0.0, 0.0),
        orientation_angle=-3.0,
        orientation_defined=True,
        previous=sample(angle=3.0),
        dt_s=1.0,
        timestamp_ms=1000,
    )
    assert result.values[6] == pytest.approx(wrap_angle(-6.0))


def test_degenerate_orientation_yields_zero_angular_velocity():
    result = temporal_features(
        wrist=(0.0, 0.0),
        orientation_angle=0.0,
        orientation_defined=False,
        previous=sample(angle=1.0, defined=True),
        dt_s=1.0,
        timestamp_ms=1000,
    )
    assert result.values[6] == 0.0


def test_zero_dt_is_rejected():
    with pytest.raises(ValueError):
        temporal_features(
            wrist=(1.0, 0.0),
            orientation_angle=0.0,
            orientation_defined=True,
            previous=sample(),
            dt_s=0.0,
            timestamp_ms=500,
        )
