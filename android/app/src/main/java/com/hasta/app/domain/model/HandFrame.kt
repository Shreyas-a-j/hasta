package com.hasta.app.domain.model

/**
 * Immutable snapshot of one tracker result.
 *
 * [imageWidth] and [imageHeight] describe the upright analysis image that the
 * normalized landmarks refer to. Rotation has already been applied by the tracker.
 */
data class HandFrame(
    val timestampMs: Long,
    val imageWidth: Int,
    val imageHeight: Int,
    val rotationDegrees: Int,
    val isFrontCamera: Boolean,
    val hands: List<HandObservation>,
) {
    companion object {
        const val MAX_HANDS = 2
        private val VALID_ROTATIONS = setOf(0, 90, 180, 270)

        /**
         * Validates a tracker result before it can reach the UI.
         *
         * Returns null for anything that cannot be drawn honestly: invalid frame
         * dimensions, unsupported rotation, more than two hands, incomplete landmark
         * lists, non-finite coordinates or out-of-range handedness scores.
         */
        fun create(
            timestampMs: Long,
            imageWidth: Int,
            imageHeight: Int,
            rotationDegrees: Int,
            isFrontCamera: Boolean,
            hands: List<HandObservation>,
        ): HandFrame? {
            if (timestampMs < 0) return null
            if (imageWidth <= 0 || imageHeight <= 0) return null
            if (rotationDegrees !in VALID_ROTATIONS) return null
            if (hands.size > MAX_HANDS) return null
            if (hands.any { !it.isValid() }) return null
            return HandFrame(
                timestampMs = timestampMs,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                rotationDegrees = rotationDegrees,
                isFrontCamera = isFrontCamera,
                hands = hands,
            )
        }
    }
}

private fun HandObservation.isValid(): Boolean {
    if (!handednessScore.isFinite() || handednessScore < 0f || handednessScore > 1f) return false
    if (landmarks.size != HandObservation.LANDMARK_COUNT) return false
    if (landmarks.any { !it.isFinite }) return false
    val world = worldLandmarks ?: return true
    if (world.size != HandObservation.LANDMARK_COUNT) return false
    return world.all { it.isFinite }
}
