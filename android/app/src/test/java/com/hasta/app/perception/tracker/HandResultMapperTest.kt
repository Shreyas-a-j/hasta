package com.hasta.app.perception.tracker

import com.hasta.app.domain.model.Handedness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HandResultMapperTest {

    @Test
    fun `empty tracker result is a valid empty frame`() {
        val frame = map(hands = emptyList())
        assertNotNull(frame)
        assertEquals(0, frame!!.hands.size)
        assertEquals(480, frame.imageWidth)
        assertEquals(640, frame.imageHeight)
    }

    @Test
    fun `one hand keeps handedness and all landmarks`() {
        val frame = map(hands = listOf(hand("Left")))
        assertNotNull(frame)
        assertEquals(1, frame!!.hands.size)
        assertEquals(Handedness.LEFT, frame.hands[0].handedness)
        assertEquals(21, frame.hands[0].landmarks.size)
        assertEquals(21, frame.hands[0].worldLandmarks?.size)
    }

    @Test
    fun `two hands keep tracker order`() {
        val frame = map(hands = listOf(hand("Right"), hand("Left")))
        assertEquals(2, frame!!.hands.size)
        assertEquals(Handedness.RIGHT, frame.hands[0].handedness)
        assertEquals(Handedness.LEFT, frame.hands[1].handedness)
    }

    @Test
    fun `front camera does not invert handedness`() {
        val frame = map(hands = listOf(hand("Left")), isFrontCamera = true)
        assertEquals(Handedness.LEFT, frame!!.hands[0].handedness)
    }

    @Test
    fun `unknown label becomes unknown`() {
        val frame = map(hands = listOf(hand(null)))
        assertEquals(Handedness.UNKNOWN, frame!!.hands[0].handedness)
    }

    @Test
    fun `missing world landmarks are allowed`() {
        val frame = map(hands = listOf(hand("Left", world = null)))
        assertNotNull(frame)
        assertNull(frame!!.hands[0].worldLandmarks)
    }

    @Test
    fun `empty world landmarks are treated as missing`() {
        val frame = map(hands = listOf(hand("Left", world = emptyList())))
        assertNull(frame!!.hands[0].worldLandmarks)
    }

    @Test
    fun `incomplete landmarks reject the frame`() {
        assertNull(map(hands = listOf(hand("Left", imageCount = 20))))
    }

    @Test
    fun `more than two hands reject the frame`() {
        assertNull(map(hands = List(3) { hand("Left") }))
    }

    @Test
    fun `non finite coordinates reject the frame`() {
        val landmarks = landmarks(21).toMutableList()
        landmarks[0] = RawLandmark(Float.POSITIVE_INFINITY, 0.5f, 0f)
        assertNull(map(hands = listOf(hand("Left", image = landmarks))))
    }

    @Test
    fun `rotation 270 swaps image dimensions`() {
        val frame = map(hands = emptyList(), rotation = 270)
        assertEquals(480, frame!!.imageWidth)
        assertEquals(640, frame.imageHeight)
    }

    private fun map(
        hands: List<RawHand>,
        rotation: Int = 90,
        isFrontCamera: Boolean = true,
    ) = HandResultMapper.map(
        timestampMs = 1234L,
        sensorWidth = 640,
        sensorHeight = 480,
        rotationDegrees = rotation,
        isFrontCamera = isFrontCamera,
        hands = hands,
    )

    private fun hand(
        label: String?,
        imageCount: Int = 21,
        image: List<RawLandmark> = landmarks(imageCount),
        world: List<RawLandmark>? = landmarks(21),
    ) = RawHand(
        handednessLabel = label,
        handednessScore = 0.93f,
        imageLandmarks = image,
        worldLandmarks = world,
    )

    private fun landmarks(count: Int): List<RawLandmark> =
        List(count) { index -> RawLandmark(index / 100f, index / 200f, 0f) }
}
