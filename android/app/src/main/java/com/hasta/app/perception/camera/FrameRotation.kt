package com.hasta.app.perception.camera

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * Applies the camera rotation to the analysis bitmap before inference.
 *
 * CameraX reports the clockwise rotation needed to make an `ImageProxy` upright but
 * leaves the buffer in sensor orientation. The MediaPipe Hand Landmarker task is
 * asked for no rotation of its own (see [com.hasta.app.perception.tracker.HandLandmarkerTracker.detect]),
 * because on the moto g64 5G the task returned landmarks in the raw sensor frame:
 * with `rotationDegrees = 180` a right hand held fingers-up produced a middle
 * fingertip *below* the wrist, i.e. a frame rotated 180 degrees from the upright
 * image the overlay assumed.
 *
 * Rotating here keeps a single source of truth: the bitmap handed to MediaPipe is
 * already upright, so normalized landmarks are upright by construction and cannot be
 * rotated twice. Camera frames are still **never mirrored** before inference, so
 * handedness remains subject-relative.
 */
object FrameRotation {

    /** Normalizes any degree value into `0`, `90`, `180` or `270`. */
    fun normalize(degrees: Int): Int = ((degrees % 360) + 360) % 360

    /**
     * Returns [source] rotated clockwise by [degrees].
     *
     * A zero rotation returns [source] unchanged so the common case allocates nothing.
     * The caller keeps the returned bitmap alive until MediaPipe reports the result.
     */
    fun rotate(source: Bitmap, degrees: Int): Bitmap {
        val normalized = normalize(degrees)
        if (normalized == 0) return source
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            /* filter = */ false,
        )
    }
}
