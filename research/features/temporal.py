"""Causal per-slot temporal features."""

from __future__ import annotations

from dataclasses import dataclass

from .geometry import wrap_angle

Point = tuple[float, float]
Vector = tuple[float, float]


@dataclass(frozen=True)
class TemporalSample:
    wrist: Point
    orientation_angle: float
    orientation_defined: bool
    velocity: Vector | None
    timestamp_ms: int


@dataclass(frozen=True)
class TemporalFeatures:
    values: tuple[float, ...]
    valid: bool
    sample: TemporalSample


def temporal_features(
    wrist: Point,
    orientation_angle: float,
    orientation_defined: bool,
    previous: TemporalSample | None,
    dt_s: float,
    timestamp_ms: int,
) -> TemporalFeatures:
    if previous is None:
        return TemporalFeatures(
            values=(0.0,) * 8,
            valid=False,
            sample=TemporalSample(
                wrist=wrist,
                orientation_angle=orientation_angle,
                orientation_defined=orientation_defined,
                velocity=None,
                timestamp_ms=timestamp_ms,
            ),
        )

    if dt_s <= 0.0:
        raise ValueError("temporal_features requires a positive dt_s")

    disp_u = wrist[0] - previous.wrist[0]
    disp_v = wrist[1] - previous.wrist[1]
    vel_u = disp_u / dt_s
    vel_v = disp_v / dt_s
    if previous.velocity is None:
        acc_u = 0.0
        acc_v = 0.0
    else:
        acc_u = (vel_u - previous.velocity[0]) / dt_s
        acc_v = (vel_v - previous.velocity[1]) / dt_s

    if orientation_defined and previous.orientation_defined:
        angular_velocity = (
            wrap_angle(orientation_angle - previous.orientation_angle) / dt_s
        )
    else:
        angular_velocity = 0.0

    values = (
        disp_u,
        disp_v,
        vel_u,
        vel_v,
        acc_u,
        acc_v,
        angular_velocity,
        1.0,
    )
    return TemporalFeatures(
        values=values,
        valid=True,
        sample=TemporalSample(
            wrist=wrist,
            orientation_angle=orientation_angle,
            orientation_defined=orientation_defined,
            velocity=(vel_u, vel_v),
            timestamp_ms=timestamp_ms,
        ),
    )
