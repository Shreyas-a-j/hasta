package com.hasta.app.perception.tracker

import com.hasta.app.domain.model.HandFrame
import com.hasta.app.domain.model.HandObservation
import com.hasta.app.domain.model.Handedness
import com.hasta.app.domain.model.Landmark
import com.hasta.app.perception.overlay.OverlayTransform

/**
 * Converts tracker output into HASTA's validated domain frame.
 *
 * MediaPipe reports normalized landmarks against the upright image, so the sensor
 * dimensions must be swapped for 90/270 degree rotations before validation.
 */
object HandResultMapper {

    fun map(
        timestampMs: Long,
        sensorWidth: Int,
        sensorHeight: Int,
        rotationDegrees: Int,
        isFrontCamera: Boolean,
        hands: List<RawHand>,
    ): HandFrame? {
        val upright = OverlayTransform.uprightSize(sensorWidth, sensorHeight, rotationDegrees)
            ?: return null
        val observations = hands.map { it.toObservation() }
        return HandFrame.create(
            timestampMs = timestampMs,
            imageWidth = upright.width,
            imageHeight = upright.height,
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
