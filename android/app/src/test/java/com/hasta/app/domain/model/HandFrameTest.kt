package com.hasta.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HandFrameTest {

    @Test
    fun `empty result is a valid frame`() {
        val frame = frame(hands = emptyList())
        assertNotNull(frame)
        assertTrue(frame!!.hands.isEmpty())
    }

    @Test
    fun `one hand is valid`() {
        val frame = frame(hands = listOf(hand(Handedness.LEFT)))
        assertEquals(1, frame!!.hands.size)
        assertEquals(21, frame.hands[0].landmarks.size)
    }

    @Test
    fun `two hands are valid and order is preserved`() {
        val frame = frame(
            hands = listOf(hand(Handedness.RIGHT), hand(Handedness.LEFT)),
        )
        assertEquals(2, frame!!.hands.size)
        assertEquals(Handedness.RIGHT, frame.hands[0].handedness)
        assertEquals(Handedness.LEFT, frame.hands[1].handedness)
    }

    @Test
    fun `more than two hands is rejected`() {
        val hands = List(3) { hand(Handedness.LEFT) }
        assertNull(frame(hands = hands))
    }

    @Test
    fun `invalid dimensions are rejected`() {
        assertNull(frame(width = 0))
        assertNull(frame(height = -1))
    }

    @Test
    fun `unsupported rotation is rejected`() {
        assertNull(frame(rotationDegrees = 45))
    }

    @Test
    fun `negative timestamp is rejected`() {
        assertNull(frame(timestampMs = -1))
    }

    @Test
    fun `incomplete landmark list is rejected`() {
        assertNull(frame(hands = listOf(hand(Handedness.LEFT, landmarkCount = 20))))
    }

    @Test
    fun `non finite landmark is rejected`() {
        val landmarks = landmarks(21).toMutableList()
        landmarks[7] = Landmark(Float.NaN, 0.5f, 0f)
        assertNull(frame(hands = listOf(hand(Handedness.LEFT, landmarks = landmarks))))
    }

    @Test
    fun `missing world landmarks are allowed`() {
        val frame = frame(hands = listOf(hand(Handedness.LEFT, worldLandmarks = null)))
        assertNotNull(frame)
        assertNull(frame!!.hands[0].worldLandmarks)
    }

    @Test
    fun `incomplete world landmarks are rejected`() {
        assertNull(frame(hands = listOf(hand(Handedness.LEFT, worldLandmarks = landmarks(20)))))
    }

    @Test
    fun `out of range handedness score is rejected`() {
        assertNull(frame(hands = listOf(hand(Handedness.LEFT, score = 1.5f))))
        assertNull(frame(hands = listOf(hand(Handedness.LEFT, score = Float.NaN))))
    }

    @Test
    fun `all supported rotations are accepted`() {
        listOf(0, 90, 180, 270).forEach { rotation ->
            assertNotNull("rotation $rotation", frame(rotationDegrees = rotation))
        }
    }

    private fun frame(
        timestampMs: Long = 1_000L,
        width: Int = 640,
        height: Int = 480,
        rotationDegrees: Int = 90,
        hands: List<HandObservation> = emptyList(),
    ) = HandFrame.create(
        timestampMs = timestampMs,
        imageWidth = width,
        imageHeight = height,
        rotationDegrees = rotationDegrees,
        isFrontCamera = true,
        hands = hands,
    )

    private fun hand(
        handedness: Handedness,
        score: Float = 0.95f,
        landmarkCount: Int = 21,
        landmarks: List<Landmark> = landmarks(landmarkCount),
        worldLandmarks: List<Landmark>? = landmarks(landmarkCount),
    ) = HandObservation(
        handedness = handedness,
        handednessScore = score,
        landmarks = landmarks,
        worldLandmarks = worldLandmarks,
    )

    private fun landmarks(count: Int): List<Landmark> =
        List(count) { index -> Landmark(index / 100f, index / 200f, 0f) }
}
