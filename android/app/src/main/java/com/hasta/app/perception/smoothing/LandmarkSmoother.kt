package com.hasta.app.perception.smoothing

import com.hasta.app.domain.model.HandFrame
import com.hasta.app.domain.model.HandObservation
import com.hasta.app.domain.model.Handedness
import com.hasta.app.domain.model.Landmark
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Adaptive One Euro smoothing for image landmarks.
 *
 * At rest the cutoff is low, so high-frequency landmark jitter is suppressed. As the
 * hand moves, the cutoff rises with the estimated speed, so the skeleton does not
 * trail fast motion. World landmarks, handedness and scores are never modified.
 *
 * Hands are associated with previous tracks by handedness and wrist proximity. Without
 * that association, a swap in MediaPipe's hand order would copy filter state from one
 * hand to the other and produce a visible jump.
 */
class LandmarkSmoother(
    private val minCutoff: Double = 0.5,
    private val beta: Double = 0.5,
    private val derivativeCutoff: Double = 1.0,
    private val maxGapMs: Long = 600L,
    private val matchDistance: Double = 0.35,
) {

    private var tracks: MutableList<Track> = mutableListOf()

    fun smooth(frame: HandFrame): HandFrame {
        val now = frame.timestampMs
        tracks.removeAll { now - it.lastSeenMs > maxGapMs }

        if (frame.hands.isEmpty()) return frame

        val available = tracks.toMutableList()
        val matches = matchHands(frame.hands, available)
        val createdTracks = mutableListOf<Track>()

        val smoothedHands = frame.hands.mapIndexed { handIndex, hand ->
            val track = matches[handIndex]
            if (track == null) {
                val created = Track.create(hand, now, minCutoff, beta, derivativeCutoff)
                createdTracks.add(created)
                hand
            } else {
                available.remove(track)
                track.apply(hand, now, minCutoff, beta, derivativeCutoff)
            }
        }

        // Keep unmatched tracks for the grace period so a one-frame dropout does not
        // discard the filter and cause a jump when the hand returns.
        tracks = (available + matches.filterNotNull() + createdTracks)
            .distinct()
            .toMutableList()

        return frame.copy(hands = smoothedHands)
    }

    /**
     * Greedy nearest-wrist assignment, with handedness as a hard preference. At most
     * two hands are ever present, so an exhaustive greedy pass is sufficient.
     */
    private fun matchHands(
        hands: List<HandObservation>,
        tracks: List<Track>,
    ): Array<Track?> {
        data class Candidate(val hand: Int, val track: Int, val distance: Double)

        val candidates = buildList {
            hands.forEachIndexed { handIndex, hand ->
                tracks.forEachIndexed { trackIndex, track ->
                    if (!compatible(hand.handedness, track.handedness)) return@forEachIndexed
                    val wrist = hand.landmarks.firstOrNull() ?: return@forEachIndexed
                    val distance = hypot(wrist.x - track.lastX, wrist.y - track.lastY)
                    if (distance <= matchDistance) {
                        add(Candidate(handIndex, trackIndex, distance))
                    }
                }
            }
        }.sortedBy { it.distance }

        val result = arrayOfNulls<Track>(hands.size)
        val usedTracks = mutableSetOf<Int>()
        candidates.forEach { candidate ->
            if (result[candidate.hand] != null) return@forEach
            if (candidate.track in usedTracks) return@forEach
            result[candidate.hand] = tracks[candidate.track]
            usedTracks.add(candidate.track)
        }
        return result
    }

    private fun compatible(hand: Handedness, track: Handedness): Boolean =
        hand == Handedness.UNKNOWN || track == Handedness.UNKNOWN || hand == track

    private class Track(
        var handedness: Handedness,
        var lastSeenMs: Long,
        var lastX: Double,
        var lastY: Double,
        private val xFilters: Array<OneEuroFilter>,
        private val yFilters: Array<OneEuroFilter>,
    ) {

        fun apply(
            hand: HandObservation,
            nowMs: Long,
            minCutoff: Double,
            beta: Double,
            derivativeCutoff: Double,
        ): HandObservation {
            val dtSeconds = (nowMs - lastSeenMs) / 1000.0
            if (dtSeconds <= 0.0 || dtSeconds > MAX_FILTER_GAP_SECONDS) {
                reset(hand)
                lastSeenMs = nowMs
                return hand
            }

            val landmarks = hand.landmarks.mapIndexed { index, raw ->
                Landmark(
                    x = xFilters[index].filter(raw.x.toDouble(), dtSeconds).toFloat(),
                    y = yFilters[index].filter(raw.y.toDouble(), dtSeconds).toFloat(),
                    z = raw.z,
                )
            }

            val wrist = landmarks.firstOrNull()
            if (wrist != null) {
                lastX = wrist.x.toDouble()
                lastY = wrist.y.toDouble()
            }
            handedness = hand.handedness
            lastSeenMs = nowMs

            return hand.copy(landmarks = landmarks)
        }

        fun reset(hand: HandObservation) {
            hand.landmarks.forEachIndexed { index, landmark ->
                xFilters[index].reset(landmark.x.toDouble())
                yFilters[index].reset(landmark.y.toDouble())
            }
            lastX = hand.landmarks.firstOrNull()?.x?.toDouble() ?: lastX
            lastY = hand.landmarks.firstOrNull()?.y?.toDouble() ?: lastY
            handedness = hand.handedness
        }

        companion object {
            fun create(
                hand: HandObservation,
                nowMs: Long,
                minCutoff: Double,
                beta: Double,
                derivativeCutoff: Double,
            ): Track {
                val xFilters = Array(HandObservation.LANDMARK_COUNT) {
                    OneEuroFilter(minCutoff, beta, derivativeCutoff)
                }
                val yFilters = Array(HandObservation.LANDMARK_COUNT) {
                    OneEuroFilter(minCutoff, beta, derivativeCutoff)
                }
                val track = Track(
                    handedness = hand.handedness,
                    lastSeenMs = nowMs,
                    lastX = hand.landmarks.firstOrNull()?.x?.toDouble() ?: 0.0,
                    lastY = hand.landmarks.firstOrNull()?.y?.toDouble() ?: 0.0,
                    xFilters = xFilters,
                    yFilters = yFilters,
                )
                track.reset(hand)
                return track
            }
        }
    }

    private companion object {
        const val MAX_FILTER_GAP_SECONDS = 0.6
    }
}

/**
 * Standard One Euro filter for one scalar. Kept internal and free of Android types so
 * the behaviour is unit tested.
 */
internal class OneEuroFilter(
    private val minCutoff: Double,
    private val beta: Double,
    private val derivativeCutoff: Double,
) {
    private var initialized = false
    private var lastRaw = 0.0
    private var lastFiltered = 0.0
    private var lastDerivative = 0.0

    fun reset(value: Double) {
        initialized = true
        lastRaw = value
        lastFiltered = value
        lastDerivative = 0.0
    }

    fun filter(value: Double, dtSeconds: Double): Double {
        if (!initialized || dtSeconds <= 0.0) {
            reset(value)
            return value
        }

        val derivative = (value - lastRaw) / dtSeconds
        val derivativeAlpha = alpha(dtSeconds, derivativeCutoff)
        val smoothedDerivative =
            derivativeAlpha * derivative + (1.0 - derivativeAlpha) * lastDerivative

        val cutoff = minCutoff + beta * abs(smoothedDerivative)
        val valueAlpha = alpha(dtSeconds, cutoff)
        val filtered = valueAlpha * value + (1.0 - valueAlpha) * lastFiltered

        lastRaw = value
        lastFiltered = filtered
        lastDerivative = smoothedDerivative
        return filtered
    }

    private fun alpha(dtSeconds: Double, cutoff: Double): Double {
        val tau = 1.0 / (2.0 * PI * cutoff)
        return 1.0 / (1.0 + tau / dtSeconds)
    }
}
