package com.hasta.app.domain.model

/**
 * A single hand landmark.
 *
 * Image landmarks use normalized coordinates in the upright analysis image:
 * x and y in [0, 1], z relative to the wrist. World landmarks use metres with the
 * origin at the hand's geometric centre.
 */
data class Landmark(
    val x: Float,
    val y: Float,
    val z: Float,
) {
    val isFinite: Boolean
        get() = x.isFinite() && y.isFinite() && z.isFinite()
}
