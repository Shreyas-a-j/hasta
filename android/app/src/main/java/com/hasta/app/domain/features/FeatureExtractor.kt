package com.hasta.app.domain.features

import com.hasta.app.domain.features.FeatureContract.GAP_THRESHOLD_MS
import com.hasta.app.domain.features.FeatureContract.HEADER_OFFSET
import com.hasta.app.domain.features.FeatureContract.INTER_HAND_OFFSET
import com.hasta.app.domain.features.FeatureContract.LEFT_GLOBAL_OFFSET
import com.hasta.app.domain.features.FeatureContract.LEFT_LOCAL_OFFSET
import com.hasta.app.domain.features.FeatureContract.LEFT_TEMPORAL_OFFSET
import com.hasta.app.domain.features.FeatureContract.PALM_SCALE_EPSILON
import com.hasta.app.domain.features.FeatureContract.RIGHT_GLOBAL_OFFSET
import com.hasta.app.domain.features.FeatureContract.RIGHT_LOCAL_OFFSET
import com.hasta.app.domain.features.FeatureContract.RIGHT_TEMPORAL_OFFSET
import com.hasta.app.domain.features.FeatureContract.VALIDITY_OFFSET
import com.hasta.app.domain.features.FeatureContract.VECTOR_EPSILON
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Stateful, causal features-v1 extractor.
 *
 * One instance owns the temporal and slot-continuity state for one frame stream.
 * Call [reset] between independent recordings. The implementation mirrors
 * `research/features/extractor.py` exactly; parity is enforced by
 * `FeatureParityTest`.
 */
class FeatureExtractor {

    private var previousTimestampMs: Long? = null
    private var segmentStartMs: Long = 0L
    private var previousSlots: Map<String, TemporalSample> = emptyMap()
    private var previousAssignment: Map<String, FeaturePoint> = emptyMap()

    fun reset() {
        previousTimestampMs = null
        segmentStartMs = 0L
        previousSlots = emptyMap()
        previousAssignment = emptyMap()
    }

    fun extract(frame: FeatureFrame): DoubleArray {
        validate(frame)

        val firstFrame = previousTimestampMs == null
        var gap = false
        val dtS: Double
        val elapsedS: Double
        val segmentStart: Long

        if (firstFrame) {
            dtS = 0.0
            elapsedS = 0.0
            segmentStart = frame.timestampMs
        } else {
            val dtMs = frame.timestampMs - previousTimestampMs!!
            gap = dtMs > GAP_THRESHOLD_MS || dtMs <= 0L
            if (gap) {
                dtS = 0.0
                elapsedS = 0.0
                segmentStart = frame.timestampMs
                previousSlots = emptyMap()
                previousAssignment = emptyMap()
            } else {
                dtS = dtMs / 1000.0
                elapsedS = (frame.timestampMs - segmentStartMs) / 1000.0
                segmentStart = segmentStartMs
            }
        }

        val vector = DoubleArray(FeatureContract.DIMENSION)
        vector[HEADER_OFFSET] = dtS
        vector[HEADER_OFFSET + 1] = elapsedS
        vector[HEADER_OFFSET + 2] = if (gap) 1.0 else 0.0
        vector[HEADER_OFFSET + 3] = if (firstFrame) 1.0 else 0.0

        val prepared = prepareHands(frame)
        val candidates = prepared.map { hand ->
            HandCandidate(hand.handedness, hand.wrist, hand.index)
        }
        val assignment = assignSlots(candidates, previousAssignment)
        vector[HEADER_OFFSET + 7] = if (assignment.ambiguous) 1.0 else 0.0

        val globalsBySlot = mutableMapOf<String, GlobalDescriptor>()
        val nextSlots = mutableMapOf<String, TemporalSample>()
        val nextAssignment = mutableMapOf<String, FeaturePoint>()

        for ((slotIndex, slot) in SLOTS.withIndex()) {
            val candidate = assignment.slots[slot]
            val hand = candidate?.let { chosen ->
                prepared.first { it.index == chosen.index }
            }
            val present = hand != null
            vector[HEADER_OFFSET + 4 + slotIndex] = if (present) 1.0 else 0.0
            if (hand == null) continue
            vector[HEADER_OFFSET + 8 + slotIndex] =
                if (hand.handedness != FeatureHandedness.UNKNOWN) 1.0 else 0.0

            val validityBase = VALIDITY_OFFSET + if (slot == "left") 0 else 21
            for (index in 0 until FeatureContract.LANDMARK_COUNT) {
                vector[validityBase + index] = if (hand.validity[index]) 1.0 else 0.0
            }

            val scale = palmScale(hand.points)
            val localValid = hand.validity.all { it } && scale >= PALM_SCALE_EPSILON
            vector[HEADER_OFFSET + 10 + slotIndex] = if (localValid) 1.0 else 0.0
            if (localValid) {
                localFeatures(hand.points, scale).copyInto(
                    vector,
                    if (slot == "left") LEFT_LOCAL_OFFSET else RIGHT_LOCAL_OFFSET,
                )
            }

            val globalRequired = intArrayOf(0, 5, 9, 13, 17)
            val globalValid = globalRequired.all { hand.validity[it] }
            vector[HEADER_OFFSET + 12 + slotIndex] = if (globalValid) 1.0 else 0.0
            val descriptor = if (globalValid) {
                globalFeatures(hand.points, scale).also { globalsBySlot[slot] = it }
            } else {
                null
            }
            if (descriptor != null) {
                descriptor.values().copyInto(
                    vector,
                    if (slot == "left") LEFT_GLOBAL_OFFSET else RIGHT_GLOBAL_OFFSET,
                )
            }

            val previousSample = previousSlots[slot]
            if (!firstFrame && !gap && previousSample != null && hand.validity[0]) {
                val result = temporalFeatures(
                    wrist = hand.points[0],
                    orientationAngle = descriptor?.orientationAngle ?: 0.0,
                    orientationDefined = descriptor?.orientationDefined ?: false,
                    previous = previousSample,
                    dtS = dtS,
                    timestampMs = frame.timestampMs,
                )
                result.values.copyInto(
                    vector,
                    if (slot == "left") LEFT_TEMPORAL_OFFSET else RIGHT_TEMPORAL_OFFSET,
                )
                vector[HEADER_OFFSET + 14 + slotIndex] = if (result.valid) 1.0 else 0.0
                nextSlots[slot] = result.sample
            } else if (hand.validity[0]) {
                nextSlots[slot] = TemporalSample(
                    wrist = hand.points[0],
                    orientationAngle = descriptor?.orientationAngle ?: 0.0,
                    orientationDefined = descriptor?.orientationDefined ?: false,
                    velocity = null,
                    timestampMs = frame.timestampMs,
                )
            }

            if (hand.validity[0]) {
                nextAssignment[slot] = hand.points[0]
            }
        }

        vector[HEADER_OFFSET + 6] =
            if (vector[HEADER_OFFSET + 4] == 1.0 && vector[HEADER_OFFSET + 5] == 1.0) 1.0 else 0.0

        val leftGlobal = globalsBySlot["left"]
        val rightGlobal = globalsBySlot["right"]
        if (leftGlobal != null && rightGlobal != null) {
            interHandFeatures(leftGlobal, rightGlobal).copyInto(vector, INTER_HAND_OFFSET)
        }

        previousTimestampMs = frame.timestampMs
        segmentStartMs = segmentStart
        previousSlots = nextSlots
        previousAssignment = nextAssignment
        return vector
    }

    private fun validate(frame: FeatureFrame) {
        require(frame.width > 0 && frame.height > 0) {
            "frame width and height must be positive"
        }
        require(frame.hands.size <= 2) {
            "features-v1 accepts at most two hands per frame"
        }
        frame.hands.forEach { hand ->
            val landmarks = hand.landmarks ?: return@forEach
            require(landmarks.size == FeatureHand.LANDMARK_COUNT) {
                "a hand must have exactly 21 landmarks"
            }
            require(hand.landmarkValidity == null ||
                hand.landmarkValidity.size == FeatureHand.LANDMARK_COUNT) {
                "landmarkValidity must have exactly 21 entries"
            }
            require(hand.handednessScore == null || hand.handednessScore.isFinite()) {
                "handednessScore must be finite"
            }
            require(landmarks.all { it.isFinite }) {
                "landmark coordinates must be finite"
            }
        }
    }

    private fun prepareHands(frame: FeatureFrame): List<PreparedHand> =
        frame.hands.mapIndexedNotNull { index, hand ->
            val landmarks = hand.landmarks ?: return@mapIndexedNotNull null
            val validity = hand.landmarkValidity ?: List(landmarks.size) { true }
            val points = landmarks.map { landmark ->
                val x = if (frame.mirroredInput) 1.0 - landmark.x else landmark.x
                FeaturePoint(
                    u = x,
                    v = landmark.y * (frame.height.toDouble() / frame.width.toDouble()),
                )
            }
            val handedness = if (frame.mirroredInput) swap(hand.handedness) else hand.handedness
            PreparedHand(handedness, points, validity, index)
        }

    private fun swap(handedness: FeatureHandedness): FeatureHandedness = when (handedness) {
        FeatureHandedness.LEFT -> FeatureHandedness.RIGHT
        FeatureHandedness.RIGHT -> FeatureHandedness.LEFT
        FeatureHandedness.UNKNOWN -> FeatureHandedness.UNKNOWN
    }

    private fun palmScale(points: List<FeaturePoint>): Double =
        0.5 * (featureDistance(points[5], points[0]) + featureDistance(points[17], points[0]))

    private fun localFeatures(points: List<FeaturePoint>, scale: Double): DoubleArray {
        val values = DoubleArray(81)
        var cursor = 0
        val wrist = points[0]

        for (index in 1 until FeatureContract.LANDMARK_COUNT) {
            values[cursor++] = (points[index].u - wrist.u) / scale
            values[cursor++] = (points[index].v - wrist.v) / scale
        }

        for ((_, chain) in FINGER_CHAINS) {
            val first = chain[0]
            val second = chain[1]
            val third = chain[2]
            val fourth = chain[3]
            values[cursor++] = angleBetween(points[0], points[first], points[second], VECTOR_EPSILON)
            values[cursor++] = angleBetween(points[first], points[second], points[third], VECTOR_EPSILON)
            values[cursor++] = angleBetween(points[second], points[third], points[fourth], VECTOR_EPSILON)
        }

        for ((_, chain) in FINGER_CHAINS) {
            for (offset in 1..3) {
                values[cursor++] =
                    featureDistance(points[chain[offset]], points[chain[offset - 1]]) / scale
            }
        }

        for ((_, first, second) in TIP_SPREAD_PAIRS) {
            values[cursor++] = featureDistance(points[first], points[second]) / scale
        }

        for ((_, chain) in FINGER_CHAINS) {
            val start = chain[0]
            val tip = chain[3]
            var chainLength = 0.0
            for (offset in 1..3) {
                chainLength += featureDistance(points[chain[offset]], points[chain[offset - 1]])
            }
            values[cursor++] = if (chainLength < VECTOR_EPSILON) {
                0.0
            } else {
                featureDistance(points[tip], points[start]) / chainLength
            }
        }

        values[cursor++] = featureDistance(points[4], points[8]) / scale

        val palmAxis = featureDistance(points[0], points[9])
        values[cursor++] = if (palmAxis < VECTOR_EPSILON) {
            0.0
        } else {
            featureDistance(points[5], points[17]) / palmAxis
        }
        return values
    }

    private fun globalFeatures(points: List<FeaturePoint>, scale: Double): GlobalDescriptor {
        val centre = FeaturePoint(
            u = (points[0].u + points[5].u + points[9].u + points[13].u + points[17].u) / 5.0,
            v = (points[0].v + points[5].v + points[9].v + points[13].v + points[17].v) / 5.0,
        )
        val dx = points[9].u - points[0].u
        val dy = points[9].v - points[0].v
        val length = sqrt(dx * dx + dy * dy)
        val defined = length >= VECTOR_EPSILON
        return GlobalDescriptor(
            centre = centre,
            wrist = points[0],
            orientationAngle = if (defined) atan2(dy, dx) else 0.0,
            orientationDefined = defined,
            palmScale = scale,
        )
    }

    private fun interHandFeatures(
        left: GlobalDescriptor,
        right: GlobalDescriptor,
    ): DoubleArray {
        val du = right.centre.u - left.centre.u
        val dv = right.centre.v - left.centre.v
        val relativeAngle = if (left.orientationDefined && right.orientationDefined) {
            wrapAngle(right.orientationAngle - left.orientationAngle)
        } else {
            0.0
        }
        val logRatio = if (left.palmScale < PALM_SCALE_EPSILON ||
            right.palmScale < PALM_SCALE_EPSILON
        ) {
            0.0
        } else {
            ln(right.palmScale / left.palmScale)
        }
        return doubleArrayOf(
            du,
            dv,
            sqrt(du * du + dv * dv),
            cos(relativeAngle),
            sin(relativeAngle),
            logRatio,
        )
    }

    private fun temporalFeatures(
        wrist: FeaturePoint,
        orientationAngle: Double,
        orientationDefined: Boolean,
        previous: TemporalSample,
        dtS: Double,
        timestampMs: Long,
    ): TemporalResult {
        require(dtS > 0.0) { "temporal features require a positive dt_s" }

        val dispU = wrist.u - previous.wrist.u
        val dispV = wrist.v - previous.wrist.v
        val velU = dispU / dtS
        val velV = dispV / dtS
        val previousVelocity = previous.velocity
        val accU = if (previousVelocity == null) 0.0 else (velU - previousVelocity.u) / dtS
        val accV = if (previousVelocity == null) 0.0 else (velV - previousVelocity.v) / dtS
        val angularVelocity = if (orientationDefined && previous.orientationDefined) {
            wrapAngle(orientationAngle - previous.orientationAngle) / dtS
        } else {
            0.0
        }
        return TemporalResult(
            values = doubleArrayOf(
                dispU,
                dispV,
                velU,
                velV,
                accU,
                accV,
                angularVelocity,
                1.0,
            ),
            valid = true,
            sample = TemporalSample(
                wrist = wrist,
                orientationAngle = orientationAngle,
                orientationDefined = orientationDefined,
                velocity = FeaturePoint(velU, velV),
                timestampMs = timestampMs,
            ),
        )
    }

    private fun assignSlots(
        candidates: List<HandCandidate>,
        previous: Map<String, FeaturePoint>,
    ): SlotAssignment {
        require(candidates.size <= 2) { "features-v1 accepts at most two hands per frame" }
        if (candidates.isEmpty()) {
            return SlotAssignment(
                mapOf<String, HandCandidate?>("left" to null, "right" to null),
                ambiguous = false,
            )
        }
        if (candidates.size == 1) {
            val candidate = candidates[0]
            val slots = mutableMapOf<String, HandCandidate?>("left" to null, "right" to null)
            return when (candidate.handedness) {
                FeatureHandedness.LEFT -> {
                    slots["left"] = candidate
                    SlotAssignment(slots, ambiguous = false)
                }
                FeatureHandedness.RIGHT -> {
                    slots["right"] = candidate
                    SlotAssignment(slots, ambiguous = false)
                }
                FeatureHandedness.UNKNOWN -> {
                    val slot = if (previous.size == 1) previous.keys.first() else "left"
                    slots[slot] = candidate
                    SlotAssignment(slots, ambiguous = true)
                }
            }
        }

        val first = candidates[0]
        val second = candidates[1]
        val labels = setOf(first.handedness, second.handedness)
        if (labels == setOf(FeatureHandedness.LEFT, FeatureHandedness.RIGHT)) {
            val slots = mutableMapOf<String, HandCandidate?>("left" to null, "right" to null)
            for (candidate in candidates) {
                slots[if (candidate.handedness == FeatureHandedness.LEFT) "left" else "right"] =
                    candidate
            }
            return SlotAssignment(slots, ambiguous = false)
        }

        val direct = mapOf<String, HandCandidate?>("left" to first, "right" to second)
        val swapped = mapOf<String, HandCandidate?>("left" to second, "right" to first)
        if (previous.isNotEmpty()) {
            val directCost = assignmentCost(direct, previous)
            val swappedCost = assignmentCost(swapped, previous)
            if (swappedCost < directCost) {
                return SlotAssignment(swapped, ambiguous = true)
            }
        }
        return SlotAssignment(direct, ambiguous = true)
    }

    private fun assignmentCost(
        slots: Map<String, HandCandidate?>,
        previous: Map<String, FeaturePoint>,
    ): Double {
        var total = 0.0
        for ((slot, candidate) in slots) {
            val previousWrist = previous[slot]
            val wrist = candidate?.wrist
            if (previousWrist == null || wrist == null) continue
            val du = wrist.u - previousWrist.u
            val dv = wrist.v - previousWrist.v
            total += du * du + dv * dv
        }
        return total
    }

    private data class PreparedHand(
        val handedness: FeatureHandedness,
        val points: List<FeaturePoint>,
        val validity: List<Boolean>,
        val index: Int,
    ) {
        val wrist: FeaturePoint?
            get() = if (validity[0]) points[0] else null
    }

    private data class HandCandidate(
        val handedness: FeatureHandedness,
        val wrist: FeaturePoint?,
        val index: Int,
    )

    private data class SlotAssignment(
        val slots: Map<String, HandCandidate?>,
        val ambiguous: Boolean,
    )

    private data class GlobalDescriptor(
        val centre: FeaturePoint,
        val wrist: FeaturePoint,
        val orientationAngle: Double,
        val orientationDefined: Boolean,
        val palmScale: Double,
    ) {
        fun values(): DoubleArray = doubleArrayOf(
            centre.u,
            centre.v,
            wrist.u,
            wrist.v,
            if (orientationDefined) cos(orientationAngle) else 1.0,
            if (orientationDefined) sin(orientationAngle) else 0.0,
            palmScale,
        )
    }

    private data class TemporalSample(
        val wrist: FeaturePoint,
        val orientationAngle: Double,
        val orientationDefined: Boolean,
        val velocity: FeaturePoint?,
        val timestampMs: Long,
    )

    private data class TemporalResult(
        val values: DoubleArray,
        val valid: Boolean,
        val sample: TemporalSample,
    )

    private companion object {
        val SLOTS = listOf("left", "right")

        val FINGER_CHAINS: List<Pair<String, IntArray>> = listOf(
            "thumb" to intArrayOf(1, 2, 3, 4),
            "index" to intArrayOf(5, 6, 7, 8),
            "middle" to intArrayOf(9, 10, 11, 12),
            "ring" to intArrayOf(13, 14, 15, 16),
            "pinky" to intArrayOf(17, 18, 19, 20),
        )

        val TIP_SPREAD_PAIRS: List<Triple<String, Int, Int>> = listOf(
            Triple("thumb_index", 4, 8),
            Triple("index_middle", 8, 12),
            Triple("middle_ring", 12, 16),
            Triple("ring_pinky", 16, 20),
        )
    }
}
