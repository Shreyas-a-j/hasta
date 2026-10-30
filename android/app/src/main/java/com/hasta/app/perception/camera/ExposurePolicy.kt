package com.hasta.app.perception.camera

import kotlin.math.roundToInt

/**
 * Chooses an exposure-compensation target from average scene luma.
 *
 * Dim/backlit scenes get up to +2 EV. Hysteresis prevents the compensation from
 * oscillating around a threshold, and a brighter scene steps back down one level at a
 * time. This is intentionally conservative: it biases auto-exposure, it does not
 * replace it.
 */
class ExposurePolicy(
    private val veryDimLuma: Int = 40,
    private val dimLuma: Int = 70,
    private val brightLuma: Int = 110,
    private val hysteresis: Int = 8,
) {

    fun targetEv(luma: Int, currentEv: Int): Int {
        val clampedCurrent = currentEv.coerceIn(MIN_EV, MAX_EV)
        return when {
            luma <= veryDimLuma -> 2
            luma <= dimLuma -> 1
            clampedCurrent > 1 && luma >= veryDimLuma + hysteresis -> 1
            clampedCurrent > 0 && luma >= dimLuma + hysteresis -> 0
            luma >= brightLuma -> 0
            else -> clampedCurrent
        }
    }

    companion object {
        const val MIN_EV = 0
        const val MAX_EV = 2

        /**
         * Converts an EV target to CameraX's step index, clamped to the device range.
         * A device that does not support compensation reports a zero step, in which
         * case the returned index is the current index.
         */
        fun indexForEv(
            ev: Int,
            stepEv: Float,
            currentIndex: Int,
            minIndex: Int,
            maxIndex: Int,
        ): Int {
            if (stepEv <= 0f || minIndex == maxIndex) return currentIndex
            val index = (ev / stepEv).roundToInt()
            return index.coerceIn(minIndex, maxIndex)
        }
    }
}
