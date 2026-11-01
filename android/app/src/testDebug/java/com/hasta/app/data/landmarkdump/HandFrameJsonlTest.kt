package com.hasta.app.data.landmarkdump

import com.hasta.app.domain.model.HandFrame
import com.hasta.app.domain.model.HandObservation
import com.hasta.app.domain.model.Handedness
import com.hasta.app.domain.model.Landmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The encoder must round-trip every field the Python landmarks-v1 path needs:
 * upright image size, timing, handedness, score and all 21 landmarks.
 */
class HandFrameJsonlTest {

    private fun landmark(index: Int) = Landmark(
        x = index * 0.01f,
        y = index * -0.02f,
        z = index * 0.005f,
    )

    private fun sampleFrame(hands: Int = 2): HandFrame {
        val observations = List(hands) { slot ->
            HandObservation(
                handedness = if (slot == 0) Handedness.LEFT else Handedness.RIGHT,
                handednessScore = if (slot == 0) 0.91f else 0.87f,
                landmarks = List(HandObservation.LANDMARK_COUNT) { landmark(it) },
                worldLandmarks = null,
            )
        }
        return HandFrame.create(
            timestampMs = 4321L,
            imageWidth = 640,
            imageHeight = 480,
            rotationDegrees = 90,
            isFrontCamera = true,
            hands = observations,
        ) ?: error("sample frame must be valid")
    }

    @Test
    fun encodesHeaderFieldsNeededByThePythonPath() {
        val line = HandFrameJsonl.encode(sampleFrame(hands = 1))
        assertTrue(line.contains("\"timestampMs\":4321"))
        assertTrue(line.contains("\"imageWidth\":640"))
        assertTrue(line.contains("\"imageHeight\":480"))
        assertTrue(line.contains("\"rotationDegrees\":90"))
        assertTrue(line.contains("\"frontCamera\":true"))
    }

    @Test
    fun encodesAllTwentyOneLandmarksPerHand() {
        val line = HandFrameJsonl.encode(sampleFrame(hands = 2))
        // 21 points, each "[x,y,z]" -> count coordinate triples per hand block
        assertEquals(2, Regex("\"handedness\"").findAll(line).count())
        assertEquals(42, Regex("\\[-?[0-9.]+e?-?[0-9]*,-?[0-9.]+e?-?[0-9]*,").findAll(line).count())
    }

    @Test
    fun encodesHandednessAndScore() {
        val line = HandFrameJsonl.encode(sampleFrame(hands = 2))
        assertTrue(line.contains("\"handedness\":\"LEFT\""))
        assertTrue(line.contains("\"handedness\":\"RIGHT\""))
        assertTrue(line.contains("\"handednessScore\":"))
    }

    @Test
    fun encodesSingleHandFramesAndEmptyFrames() {
        val single = HandFrameJsonl.encode(sampleFrame(hands = 1))
        assertTrue(single.endsWith("]}"))
        val empty = HandFrameJsonl.encode(sampleFrame(hands = 0))
        assertTrue(empty.contains("\"hands\":[]"))
    }

    @Test
    fun producedLineIsSingleLineWithNoEmbeddedNewlines() {
        val line = HandFrameJsonl.encode(sampleFrame())
        assertTrue(line.isNotBlank())
        assertEquals(-1, line.indexOf('\n'))
    }
}
