package com.hasta.app.perception.camera

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the direction and dimension behaviour of the bitmap rotation that makes the
 * analysis frame upright.
 *
 * This is the regression test for the misaligned overlay: the frame must rotate
 * clockwise by `ImageInfo.rotationDegrees`, which is CameraX's documented meaning.
 */
@RunWith(AndroidJUnit4::class)
class FrameRotationInstrumentedTest {

    private fun quadrants(): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
        // A B
        // C D
        setPixel(0, 0, Color.RED)
        setPixel(1, 0, Color.GREEN)
        setPixel(0, 1, Color.BLUE)
        setPixel(1, 1, Color.WHITE)
    }

    @Test
    fun zeroDegreesReturnsTheSameBitmap() {
        val source = quadrants()
        assertSame(source, FrameRotation.rotate(source, 0))
    }

    @Test
    fun ninetyDegreesRotatesClockwise() {
        val rotated = FrameRotation.rotate(quadrants(), 90)

        assertEquals(2, rotated.width)
        assertEquals(2, rotated.height)
        // C A
        // D B
        assertEquals(Color.BLUE, rotated.getPixel(0, 0))
        assertEquals(Color.RED, rotated.getPixel(1, 0))
        assertEquals(Color.WHITE, rotated.getPixel(0, 1))
        assertEquals(Color.GREEN, rotated.getPixel(1, 1))
    }

    @Test
    fun ninetyDegreesSwapsNonSquareDimensions() {
        val source = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        val rotated = FrameRotation.rotate(source, 90)
        assertEquals(2, rotated.width)
        assertEquals(3, rotated.height)
    }

    @Test
    fun oneHundredEightyDegreesFlipsBothAxes() {
        val rotated = FrameRotation.rotate(quadrants(), 180)

        assertEquals(Color.WHITE, rotated.getPixel(0, 0))
        assertEquals(Color.BLUE, rotated.getPixel(1, 0))
        assertEquals(Color.GREEN, rotated.getPixel(0, 1))
        assertEquals(Color.RED, rotated.getPixel(1, 1))
    }

    @Test
    fun negativeRotationIsTreatedAsItsPositiveEquivalent() {
        val clockwise = FrameRotation.rotate(quadrants(), 270)
        val counterClockwise = FrameRotation.rotate(quadrants(), -90)

        for (x in 0 until 2) {
            for (y in 0 until 2) {
                assertEquals(clockwise.getPixel(x, y), counterClockwise.getPixel(x, y))
            }
        }
    }
}
