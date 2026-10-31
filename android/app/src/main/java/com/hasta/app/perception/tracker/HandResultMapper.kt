package com.hasta.app.perception.tracker

import com.hasta.app.domain.model.HandFrame
import com.hasta.app.domain.model.HandObservation
import com.hasta.app.domain.model.Handedness
import com.hasta.app.domain.model.Landmark

/**
 * Converts tracker output into HASTA's validated domain frame.
 *
 * MediaPipe receives an upright bitmap, so its normalized landmarks already refer to
 * the image of size [imageWidth] x [imageHeight]. No rotation is re-applied here;
 * [rotationDegrees] is carried as metadata only.
 */
object HandResultMapper {

    fun map(
        timestampMs: Long,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        isFrontCamera: Boolean,
        hands: List<RawHand>,
    ): HandFrame? {
        val observations = hands.map { it.toObservation() }
        return HandFrame.create(
            timestampMs = timestampMs,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            rotationDegrees = rotationDegrees,
            isFrontCamera = isFrontCamera,
            hands = observations,
        )
    }

    private fun RawHand.toObservation() = HandObservation(
        handedness = parseHandedness(handednessLabel),
        handednessScore = handednessScore,
        landmarks = imageLandmarks.map { Landmark(it.x, it.y, it.z) },
        worldLandmarks = worldLandmarks
            ?.takeIf { it.isNotEmpty() }
            ?.map { Landmark(it.x, it.y, it.z) },
    )

    private fun parseHandedness(label: String?): Handedness = when (label?.lowercase()) {
        "left" -> Handedness.LEFT
        "right" -> Handedness.RIGHT
        else -> Handedness.UNKNOWN
    }
}
