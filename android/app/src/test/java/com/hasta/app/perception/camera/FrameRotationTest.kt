package com.hasta.app.perception.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bitmap rotation itself needs `android.graphics`, so it is covered by
 * `FrameRotationInstrumentedTest` on a device. This test covers the degree
 * normalisation that guards against negative or over-rotated metadata.
 */
class FrameRotationTest {

    @Test
    fun `camera rotations pass through unchanged`() {
        assertEquals(0, FrameRotation.normalize(0))
        assertEquals(90, FrameRotation.normalize(90))
        assertEquals(180, FrameRotation.normalize(180))
        assertEquals(270, FrameRotation.normalize(270))
    }

    @Test
    fun `negative and multi turn rotations normalize into one of four quadrants`() {
        assertEquals(270, FrameRotation.normalize(-90))
        assertEquals(0, FrameRotation.normalize(360))
        assertEquals(90, FrameRotation.normalize(450))
        assertEquals(180, FrameRotation.normalize(-180))
    }
}
