package com.hasta.app.domain.features

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FeatureParityTest {

    @Test
    fun `kotlin reproduces every golden case`() {
        FeatureGoldenVectors.cases.forEach { case ->
            val extractor = FeatureExtractor()
            case.frames.forEachIndexed { frameIndex, frame ->
                val actual = extractor.extract(frame.toFeatureFrame())
                val expected = case.expected[frameIndex]
                val label = "${case.name} frame $frameIndex"

                assertEquals("$label dimension", FeatureContract.DIMENSION, actual.size)
                assertEquals("$label expected dimension", FeatureContract.DIMENSION, expected.size)

                expected.indices.forEach { index ->
                    assertTrue("$label channel $index is not finite", actual[index].isFinite())
                    if (isMaskChannel(index)) {
                        assertEquals(
                            "$label mask channel $index",
                            expected[index],
                            actual[index],
                            0.0,
                        )
                    } else {
                        assertEquals(
                            "$label channel $index",
                            expected[index],
                            actual[index],
                            FeatureGoldenVectors.ATOL +
                                FeatureGoldenVectors.RTOL * abs(expected[index]),
                        )
                    }
                }
            }
        }
    }

    private fun isMaskChannel(index: Int): Boolean =
        index in 2..15 || index in 16..57 || index == 153 || index == 249
}

private fun FeatureGoldenVectors.Frame.toFeatureFrame(): FeatureFrame = FeatureFrame(
    timestampMs = timestampMs,
    width = width,
    height = height,
    mirroredInput = mirroredInput,
    hands = hands.map { hand ->
        FeatureHand(
            handedness = when (hand.handedness) {
                "left" -> FeatureHandedness.LEFT
                "right" -> FeatureHandedness.RIGHT
                else -> FeatureHandedness.UNKNOWN
            },
            handednessScore = hand.handednessScore,
            landmarks = hand.landmarks?.map { landmark ->
                FeatureLandmark(landmark.x, landmark.y, landmark.z)
            },
            landmarkValidity = hand.landmarkValidity,
        )
    },
)
