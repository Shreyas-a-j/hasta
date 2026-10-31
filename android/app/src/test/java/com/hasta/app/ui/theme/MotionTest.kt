package com.hasta.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {

    @Test
    fun motionDurationsAreShortAndPurposeful() {
        assertTrue(HastaMotionSpec.fastMs in 80..150)
        assertTrue(HastaMotionSpec.standardMs in 150..220)
        assertTrue(HastaMotionSpec.emphasizedMs in 220..300)
        assertEquals(HastaMotionSpec.fastMs, HastaMotionSpec.durationFor(HastaMotionSpec.fastMs))
    }

    @Test
    fun reducedMotionCollapsesDurationsInsteadOfLeavingLoopsRunning() {
        assertTrue(HastaReducedMotion.reduced)
        assertEquals(0, HastaReducedMotion.durationFor(HastaMotionSpec.emphasizedMs))
        assertEquals(0, HastaReducedMotion.durationFor(HastaMotionSpec.standardMs))
    }

    @Test
    fun animatorScaleZeroIsTreatedAsReducedMotion() {
        assertTrue(isReducedMotion(animatorScale = 0f))
        assertFalse(isReducedMotion(animatorScale = 1f))
        assertFalse(isReducedMotion(animatorScale = 0.5f))
    }
}
