package com.hasta.app.perception.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayTransformTest {

    private val eps = 0.001f

    @Test
    fun `square source and square view map one to one`() {
        val transform = requireTransform(100, 100, 0, 100f, 100f, mirror = false)

        assertPoint(0f, 0f, transform.mapNormalized(0f, 0f), 0f, 0f)
        assertPoint(0.5f, 0.5f, transform.mapNormalized(0.5f, 0.5f), 50f, 50f)
        assertPoint(1f, 1f, transform.mapNormalized(1f, 1f), 100f, 100f)
    }

    @Test
    fun `portrait view centre crops a landscape source`() {
        // 4:3 source, 9:16 view: scale by height, crop equally on both sides.
        val transform = requireTransform(640, 480, 0, 1080f, 1920f, mirror = false)

        assertPoint(0f, 0f, transform.mapNormalized(0f, 0f), -740f, 0f)
        assertPoint(0.5f, 0.5f, transform.mapNormalized(0.5f, 0.5f), 540f, 960f)
        assertPoint(1f, 1f, transform.mapNormalized(1f, 1f), 1820f, 1920f)
    }

    @Test
    fun `landscape view centre crops a portrait source`() {
        // Upright 480x640 in a 1920x1080 view: scale by width.
        val transform = requireTransform(480, 640, 0, 1920f, 1080f, mirror = false)

        assertPoint(0f, 0.5f, transform.mapNormalized(0f, 0.5f), 0f, 540f)
        assertPoint(1f, 0.5f, transform.mapNormalized(1f, 0.5f), 1920f, 540f)
    }

    @Test
    fun `rotation 90 swaps source dimensions`() {
        val upright = OverlayTransform.uprightSize(640, 480, 90)
        assertEquals(480, upright!!.width)
        assertEquals(640, upright.height)

        val transform = requireTransform(640, 480, 90, 1080f, 1920f, mirror = false)
        assertPoint(0.5f, 0.5f, transform.mapNormalized(0.5f, 0.5f), 540f, 960f)
    }

    @Test
    fun `rotation 270 swaps source dimensions`() {
        val upright = OverlayTransform.uprightSize(640, 480, 270)
        assertEquals(480, upright!!.width)
        assertEquals(640, upright.height)
    }

    @Test
    fun `rotation 180 keeps source dimensions`() {
        val upright = OverlayTransform.uprightSize(640, 480, 180)
        assertEquals(640, upright!!.width)
        assertEquals(480, upright.height)
    }

    @Test
    fun `front camera mirrors x but never y`() {
        val transform = requireTransform(100, 100, 0, 100f, 100f, mirror = true)

        assertPoint(0f, 0.25f, transform.mapNormalized(0f, 0.25f), 100f, 25f)
        assertPoint(1f, 0.75f, transform.mapNormalized(1f, 0.75f), 0f, 75f)
        assertPoint(0.5f, 0.5f, transform.mapNormalized(0.5f, 0.5f), 50f, 50f)
    }

    @Test
    fun `mirror is applied after centre crop`() {
        val transform = requireTransform(640, 480, 0, 1080f, 1920f, mirror = true)

        // Unmirrored x=0 maps to -740; mirrored maps to 1080 - (-740) = 1820.
        assertPoint(0f, 0.5f, transform.mapNormalized(0f, 0.5f), 1820f, 960f)
        assertPoint(1f, 0.5f, transform.mapNormalized(1f, 0.5f), -740f, 960f)
    }

    @Test
    fun `out of range normalized coordinates are not clamped`() {
        val transform = requireTransform(100, 100, 0, 100f, 100f, mirror = false)
        assertPoint(-0.1f, 1.1f, transform.mapNormalized(-0.1f, 1.1f), -10f, 110f)
    }

    @Test
    fun `invalid input returns null`() {
        assertNull(OverlayTransform.create(0, 480, 0, 100f, 100f, false))
        assertNull(OverlayTransform.create(640, 0, 0, 100f, 100f, false))
        assertNull(OverlayTransform.create(640, 480, 45, 100f, 100f, false))
        assertNull(OverlayTransform.create(640, 480, 0, 0f, 100f, false))
        assertNull(OverlayTransform.create(640, 480, 0, 100f, -1f, false))
        assertNull(OverlayTransform.create(640, 480, 0, Float.NaN, 100f, false))
    }

    @Test
    fun `upright size rejects invalid values`() {
        assertNull(OverlayTransform.uprightSize(0, 480, 0))
        assertNull(OverlayTransform.uprightSize(640, 480, 45))
    }

    private fun requireTransform(
        width: Int,
        height: Int,
        rotation: Int,
        viewWidth: Float,
        viewHeight: Float,
        mirror: Boolean,
    ): OverlayTransform = requireNotNull(
        OverlayTransform.create(width, height, rotation, viewWidth, viewHeight, mirror),
    )

    private fun assertPoint(
        normX: Float,
        normY: Float,
        actual: OverlayPoint,
        expectedX: Float,
        expectedY: Float,
    ) {
        assertEquals("x for ($normX,$normY)", expectedX, actual.x, eps)
        assertEquals("y for ($normX,$normY)", expectedY, actual.y, eps)
    }
}
