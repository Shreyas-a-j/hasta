package com.hasta.app.domain.features

import com.hasta.app.domain.model.HandFrame
import com.hasta.app.domain.model.Handedness

/**
 * Maps the validated M0 domain frame into features-v1 input.
 *
 * M0 does not mirror the inference frame, so [FeatureFrame.mirroredInput] is
 * always false here. A future mirrored dataset must set the flag explicitly
 * rather than relying on this adapter.
 */
object HandFrameFeatureAdapter {

    fun adapt(frame: HandFrame): FeatureFrame = FeatureFrame(
        timestampMs = frame.timestampMs,
        width = frame.imageWidth,
        height = frame.imageHeight,
        mirroredInput = false,
        hands = frame.hands.map { hand ->
            FeatureHand(
                handedness = when (hand.handedness) {
                    Handedness.LEFT -> FeatureHandedness.LEFT
                    Handedness.RIGHT -> FeatureHandedness.RIGHT
                    Handedness.UNKNOWN -> FeatureHandedness.UNKNOWN
                },
                handednessScore = hand.handednessScore.toDouble(),
                landmarks = hand.landmarks.map { landmark ->
                    FeatureLandmark(
                        x = landmark.x.toDouble(),
                        y = landmark.y.toDouble(),
                        z = landmark.z.toDouble(),
                    )
                },
                landmarkValidity = null,
            )
        },
    )
}
