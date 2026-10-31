package com.hasta.app.perception.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.hasta.app.perception.PipelineStats
import com.hasta.app.perception.tracker.HandLandmarkerTracker

/**
 * Converts camera frames to bitmaps and submits them to the tracker.
 *
 * The bitmap is rotated upright with [FrameRotation] before inference, so the tracker
 * always receives an upright image and landmarks need no further rotation.
 *
 * The ImageProxy is always closed in a finally block. A frame is skipped while the
 * tracker is busy, which bounds the pipeline to one in-flight frame.
 */
class FrameAnalyzer(
    private val tracker: HandLandmarkerTracker,
    private val stats: PipelineStats,
    private val isFrontCamera: Boolean,
    private val onError: (String) -> Unit,
    private val onLuma: (Int) -> Unit = {},
) : ImageAnalysis.Analyzer {

    private var lumaFrameCounter = 0

    override fun analyze(image: ImageProxy) {
        stats.onSubmitted()
        try {
            if (tracker.isBusy()) {
                stats.onSkipped()
                return
            }

            if (++lumaFrameCounter % LUMA_SAMPLE_EVERY == 0) {
                LumaMeter.average(image)?.let(onLuma)
            }

            val rotationDegrees = image.imageInfo.rotationDegrees
            val captureElapsedMs = image.imageInfo.timestamp / NANOS_PER_MILLI
            val upright = FrameRotation.rotate(image.toBitmap(), rotationDegrees)

            val accepted = tracker.detect(
                bitmap = upright,
                imageWidth = upright.width,
                imageHeight = upright.height,
                rotationDegrees = rotationDegrees,
                isFrontCamera = isFrontCamera,
                captureElapsedMs = captureElapsedMs,
            )
            if (!accepted) {
                stats.onSkipped()
            }
        } catch (error: Throwable) {
            stats.onError()
            onError("Frame analysis failed: ${error.message ?: error.javaClass.simpleName}")
        } finally {
            image.close()
        }
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
        const val LUMA_SAMPLE_EVERY = 15
    }
}
