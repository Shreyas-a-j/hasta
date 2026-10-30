package com.hasta.app.perception.camera

import androidx.camera.core.ImageProxy

/**
 * Cheap average-luma estimate from the Y plane of a YUV_420_888 frame.
 *
 * Only the central 60% of the image is sampled, at a coarse stride, so the cost is a
 * few hundred byte reads. This is a metering hint for exposure compensation, not an
 * image-processing step: the frame passed to MediaPipe is untouched.
 */
object LumaMeter {

    fun average(image: ImageProxy, sampleStep: Int = DEFAULT_SAMPLE_STEP): Int? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        if (!buffer.hasRemaining()) return null

        val left = image.width * 2 / 10
        val right = image.width * 8 / 10
        val top = image.height * 2 / 10
        val bottom = image.height * 8 / 10
        if (right <= left || bottom <= top) return null

        var sum = 0L
        var count = 0
        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val offset = y * plane.rowStride + x * plane.pixelStride
                if (offset in 0 until buffer.limit()) {
                    sum += buffer.get(offset).toInt() and 0xFF
                    count++
                }
                x += sampleStep
            }
            y += sampleStep
        }
        return if (count == 0) null else (sum / count).toInt()
    }

    const val DEFAULT_SAMPLE_STEP = 8
}
