package com.hasta.app.perception.overlay

import kotlin.math.max

/**
 * Maps normalized landmark coordinates from the upright analysis image to the
 * overlay view.
 *
 * The preview and analysis use the same 4:3 aspect ratio and the overlay has the
 * same bounds as the preview, which uses `FILL_CENTER`. That means the mapping is a
 * centre-crop scale followed by an optional display-only horizontal mirror for the
 * front camera.
 *
 * This class is deliberately free of Android types so the maths is unit tested.
 */
class OverlayTransform private constructor(
    private val sourceWidth: Int,
    private val sourceHeight: Int,
    private val viewWidth: Float,
    private val viewHeight: Float,
    private val mirrorHorizontally: Boolean,
    private val scale: Float,
    private val offsetX: Float,
    private val offsetY: Float,
) {

    fun mapNormalized(x: Float, y: Float): OverlayPoint {
        val scaledX = offsetX + x * sourceWidth * scale
        val scaledY = offsetY + y * sourceHeight * scale
        val viewX = if (mirrorHorizontally) viewWidth - scaledX else scaledX
        return OverlayPoint(viewX, scaledY)
    }

    companion object {
        private val VALID_ROTATIONS = setOf(0, 90, 180, 270)

        /**
         * @param sensorWidth raw ImageProxy width before rotation
         * @param sensorHeight raw ImageProxy height before rotation
         * @param rotationDegrees ImageAnalysis rotation metadata (clockwise)
         * @param viewWidth overlay view width in pixels
         * @param viewHeight overlay view height in pixels
         * @param mirrorHorizontally true for the front camera (display mirror only)
         */
        fun create(
            sensorWidth: Int,
            sensorHeight: Int,
            rotationDegrees: Int,
            viewWidth: Float,
            viewHeight: Float,
            mirrorHorizontally: Boolean,
        ): OverlayTransform? {
            val upright = uprightSize(sensorWidth, sensorHeight, rotationDegrees) ?: return null
            return createForUprightImage(
                uprightWidth = upright.width,
                uprightHeight = upright.height,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                mirrorHorizontally = mirrorHorizontally,
            )
        }

        /**
         * Variant for callers that already hold the upright image dimensions (for
         * example [com.hasta.app.domain.model.HandFrame]).
         */
        fun createForUprightImage(
            uprightWidth: Int,
            uprightHeight: Int,
            viewWidth: Float,
            viewHeight: Float,
            mirrorHorizontally: Boolean,
        ): OverlayTransform? {
            if (uprightWidth <= 0 || uprightHeight <= 0) return null
            if (!viewWidth.isFinite() || !viewHeight.isFinite()) return null
            if (viewWidth <= 0f || viewHeight <= 0f) return null

            val scale = max(viewWidth / uprightWidth, viewHeight / uprightHeight)
            val offsetX = (viewWidth - uprightWidth * scale) / 2f
            val offsetY = (viewHeight - uprightHeight * scale) / 2f
            return OverlayTransform(
                sourceWidth = uprightWidth,
                sourceHeight = uprightHeight,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                mirrorHorizontally = mirrorHorizontally,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
            )
        }

        /**
         * Size of the image after the rotation metadata has been applied. MediaPipe
         * reports normalized landmarks against this size, not the raw sensor size.
         */
        fun uprightSize(width: Int, height: Int, rotationDegrees: Int): UprightSize? {
            if (width <= 0 || height <= 0) return null
            if (rotationDegrees !in VALID_ROTATIONS) return null
            return if (rotationDegrees == 90 || rotationDegrees == 270) {
                UprightSize(width = height, height = width)
            } else {
                UprightSize(width = width, height = height)
            }
        }
    }
}

data class UprightSize(val width: Int, val height: Int)

data class OverlayPoint(val x: Float, val y: Float)
