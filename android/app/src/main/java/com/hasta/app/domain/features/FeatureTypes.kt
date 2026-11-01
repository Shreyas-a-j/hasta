package com.hasta.app.domain.features

/**
 * HASTA-owned feature input types.
 *
 * These are deliberately independent of MediaPipe and of the Android UI. The
 * perception boundary maps a [com.hasta.app.domain.model.HandFrame] into this
 * shape; no MediaPipe type escapes `perception/`.
 */
enum class FeatureHandedness {
    LEFT,
    RIGHT,
    UNKNOWN,
}

data class FeatureLandmark(
    val x: Double,
    val y: Double,
    val z: Double = 0.0,
) {
    val isFinite: Boolean
        get() = x.isFinite() && y.isFinite() && z.isFinite()
}

data class FeatureHand(
    val handedness: FeatureHandedness = FeatureHandedness.UNKNOWN,
    val handednessScore: Double? = null,
    val landmarks: List<FeatureLandmark>? = null,
    val landmarkValidity: List<Boolean>? = null,
) {
    companion object {
        const val LANDMARK_COUNT = 21
    }
}

data class FeatureFrame(
    val timestampMs: Long,
    val width: Int,
    val height: Int,
    val hands: List<FeatureHand> = emptyList(),
    val mirroredInput: Boolean = false,
)
