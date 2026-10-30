package com.hasta.app.perception.tracker

/**
 * MediaPipe-independent representation of one detected hand, used so the mapping
 * logic can be unit tested without Android or MediaPipe classes.
 */
data class RawHand(
    val handednessLabel: String?,
    val handednessScore: Float,
    val imageLandmarks: List<RawLandmark>,
    val worldLandmarks: List<RawLandmark>?,
)

data class RawLandmark(
    val x: Float,
    val y: Float,
    val z: Float,
)
