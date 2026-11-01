package com.hasta.app.domain.features

import com.hasta.app.domain.model.HandFrame
import com.hasta.app.domain.model.HandObservation
import com.hasta.app.domain.model.Handedness
import com.hasta.app.domain.model.Landmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HandFrameFeatureAdapterTest {

    @Test
    fun `adapter preserves handedness and landmark values without mirroring`() {
        val landmarks = List(21) { index ->
            Landmark(x = 0.1f * index, y = 0.2f, z = 0.0f)
        }
        val domain = HandFrame.create(
            timestampMs = 1234L,
            imageWidth = 640,
            imageHeight = 480,
            rotationDegrees = 90,
            isFrontCamera = true,
            hands = listOf(
                HandObservation(
                    handedness = Handedness.LEFT,
                    handednessScore = 0.9f,
                    landmarks = landmarks,
                    worldLandmarks = null,
                ),
            ),
        )
        assertNotNull(domain)

        val features = HandFrameFeatureAdapter.adapt(domain!!)
        assertEquals(1234L, features.timestampMs)
        assertEquals(640, features.width)
        assertEquals(480, features.height)
        assertEquals(false, features.mirroredInput)
        assertEquals(FeatureHandedness.LEFT, features.hands[0].handedness)
        assertEquals(0.9, features.hands[0].handednessScore!!, 1e-7)
        assertEquals(0.1, features.hands[0].landmarks!![1].x, 1e-7)
        assertTrue(features.hands[0].landmarkValidity == null)
    }
}
