package com.hasta.app.perception.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class ExposurePolicyTest {

    private val policy = ExposurePolicy()

    @Test
    fun `dim scenes request positive compensation`() {
        assertEquals(2, policy.targetEv(luma = 20, currentEv = 0))
        assertEquals(1, policy.targetEv(luma = 60, currentEv = 0))
        assertEquals(0, policy.targetEv(luma = 150, currentEv = 0))
    }

    @Test
    fun `compensation steps down with hysteresis`() {
        assertEquals(1, policy.targetEv(luma = 60, currentEv = 2))
        assertEquals(1, policy.targetEv(luma = 75, currentEv = 1))
        assertEquals(0, policy.targetEv(luma = 90, currentEv = 1))
    }

    @Test
    fun `a mid scene does not introduce compensation`() {
        assertEquals(0, policy.targetEv(luma = 85, currentEv = 0))
    }

    @Test
    fun `ev is converted to device steps`() {
        assertEquals(4, ExposurePolicy.indexForEv(2, stepEv = 0.5f, currentIndex = 0, minIndex = -8, maxIndex = 8))
        assertEquals(2, ExposurePolicy.indexForEv(1, stepEv = 0.5f, currentIndex = 0, minIndex = -8, maxIndex = 8))
    }

    @Test
    fun `device range and unsupported compensation are respected`() {
        assertEquals(0, ExposurePolicy.indexForEv(2, stepEv = 0.5f, currentIndex = 0, minIndex = 0, maxIndex = 0))
        assertEquals(2, ExposurePolicy.indexForEv(2, stepEv = 1f, currentIndex = 0, minIndex = 0, maxIndex = 3))
        assertEquals(3, ExposurePolicy.indexForEv(5, stepEv = 1f, currentIndex = 0, minIndex = 0, maxIndex = 3))
        assertEquals(1, ExposurePolicy.indexForEv(2, stepEv = 0f, currentIndex = 1, minIndex = 0, maxIndex = 4))
    }
}
