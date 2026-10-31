package com.hasta.app.domain.features

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class FeaturePoint(val u: Double, val v: Double)

internal fun featureDistance(a: FeaturePoint, b: FeaturePoint): Double {
    val du = a.u - b.u
    val dv = a.v - b.v
    return sqrt(du * du + dv * dv)
}

internal fun angleBetween(
    first: FeaturePoint,
    joint: FeaturePoint,
    second: FeaturePoint,
    epsilon: Double,
): Double {
    val ax = first.u - joint.u
    val ay = first.v - joint.v
    val bx = second.u - joint.u
    val by = second.v - joint.v
    val aLength = sqrt(ax * ax + ay * ay)
    val bLength = sqrt(bx * bx + by * by)
    if (aLength < epsilon || bLength < epsilon) return 0.0
    val cross = abs(ax * by - ay * bx)
    val dot = ax * bx + ay * by
    return atan2(cross, dot)
}

internal fun wrapAngle(value: Double): Double = atan2(sin(value), cos(value))
