package com.hasta.app.perception.smoothing

import com.hasta.app.domain.model.HandFrame
import com.hasta.app.domain.model.HandObservation
import com.hasta.app.domain.model.Handedness
import com.hasta.app.domain.model.Landmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class LandmarkSmootherTest {

    private val smoother = LandmarkSmoother()

    @Test
    fun `jitter at rest is reduced`() {
        val raw = mutableListOf<Double>()
        val filtered = mutableListOf<Double>()

        repeat(60) { index ->
            val wobble = if (index % 2 == 0) 0.02f else -0.02f
            val frame = frame(
                timestampMs = 1_000L + index * 100L,
                hands = listOf(hand(Handedness.RIGHT, wristX = 0.5f + wobble)),
            )
            val smoothed = smoother.smooth(frame)
            if (index >= 20) {
                raw.add(frame.hands[0].landmarks[0].x.toDouble())
                filtered.add(smoothed.hands[0].landmarks[0].x.toDouble())
            }
        }

        assertTrue(
            "smoothed std ${stdDev(filtered)} should be well below raw ${stdDev(raw)}",
            stdDev(filtered) < stdDev(raw) * 0.6,
        )
    }

    @Test
    fun `fast movement is followed without a long trail`() {
        repeat(10) { index ->
            smoother.smooth(
                frame(
                    timestampMs = 1_000L + index * 100L,
                    hands = listOf(hand(Handedness.LEFT, wristX = 0.3f)),
                ),
            )
        }

        var last = 0f
        repeat(3) { index ->
            val smoothed = smoother.smooth(
                frame(
                    timestampMs = 2_000L + index * 100L,
                    hands = listOf(hand(Handedness.LEFT, wristX = 0.7f)),
                ),
            )
            last = smoothed.hands[0].landmarks[0].x
        }

        assertTrue("expected the filter to move toward 0.7, got $last", last > 0.6f)
    }

    @Test
    fun `handedness and world landmarks are never rewritten`() {
        val world = List(21) { Landmark(it / 100f, it / 200f, it / 300f) }
        val hand = hand(Handedness.LEFT, wristX = 0.4f).copy(worldLandmarks = world)

        val smoothed = smoother.smooth(frame(timestampMs = 1_000L, hands = listOf(hand)))

        assertEquals(Handedness.LEFT, smoothed.hands[0].handedness)
        assertEquals(world, smoothed.hands[0].worldLandmarks)
        assertEquals(hand.landmarks[5].z, smoothed.hands[0].landmarks[5].z)
    }

    @Test
    fun `a swapped hand order does not swap filter state`() {
        repeat(20) { index ->
            val left = hand(Handedness.LEFT, wristX = 0.2f)
            val right = hand(Handedness.RIGHT, wristX = 0.8f)
            val hands = if (index % 2 == 0) listOf(left, right) else listOf(right, left)
            smoother.smooth(frame(1_000L + index * 100L, hands))
        }

        val smoothed = smoother.smooth(
            frame(
                timestampMs = 3_100L,
                hands = listOf(
                    hand(Handedness.RIGHT, wristX = 0.8f),
                    hand(Handedness.LEFT, wristX = 0.2f),
                ),
            ),
        )

        val left = smoothed.hands.first { it.handedness == Handedness.LEFT }
        val right = smoothed.hands.first { it.handedness == Handedness.RIGHT }
        assertTrue("left drifted to ${left.landmarks[0].x}", left.landmarks[0].x < 0.3f)
        assertTrue("right drifted to ${right.landmarks[0].x}", right.landmarks[0].x > 0.7f)
    }

    @Test
    fun `a long gap resets the filter instead of dragging the old position`() {
        smoother.smooth(frame(1_000L, listOf(hand(Handedness.LEFT, wristX = 0.2f))))

        val smoothed = smoother.smooth(
            frame(3_000L, listOf(hand(Handedness.LEFT, wristX = 0.8f))),
        )

        assertEquals(0.8f, smoothed.hands[0].landmarks[0].x, 0.0001f)
    }

    @Test
    fun `an empty frame is harmless`() {
        smoother.smooth(frame(1_000L, emptyList()))
        smoother.smooth(frame(1_100L, listOf(hand(Handedness.LEFT, wristX = 0.5f))))
    }

    private fun frame(timestampMs: Long, hands: List<HandObservation>): HandFrame =
        requireNotNull(
            HandFrame.create(
                timestampMs = timestampMs,
                imageWidth = 640,
                imageHeight = 480,
                rotationDegrees = 270,
                isFrontCamera = true,
                hands = hands,
            ),
        )

    private fun hand(
        handedness: Handedness,
        wristX: Float,
        wristY: Float = 0.5f,
    ): HandObservation = HandObservation(
        handedness = handedness,
        handednessScore = 0.95f,
        landmarks = List(HandObservation.LANDMARK_COUNT) { index ->
            Landmark(
                x = wristX + index * 0.001f,
                y = wristY + index * 0.001f,
                z = 0f,
            )
        },
        worldLandmarks = null,
    )

    private fun stdDev(values: List<Double>): Double {
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }
}
