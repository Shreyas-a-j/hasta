package com.hasta.app.domain.model

/**
 * One detected hand in one frame.
 *
 * [landmarks] is always complete (21 points). A hand whose landmark list is
 * incomplete is rejected rather than padded, so no artificial points can be drawn.
 * [worldLandmarks] is optional because a tracker result may omit it.
 */
data class HandObservation(
    val handedness: Handedness,
    val handednessScore: Float,
    val landmarks: List<Landmark>,
    val worldLandmarks: List<Landmark>?,
) {
    companion object {
        const val LANDMARK_COUNT = 21
    }
}
