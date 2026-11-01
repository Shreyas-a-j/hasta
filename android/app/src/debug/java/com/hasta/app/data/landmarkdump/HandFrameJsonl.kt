package com.hasta.app.data.landmarkdump

import com.hasta.app.domain.model.HandFrame
import com.hasta.app.domain.model.Landmark

/**
 * Compact JSON Lines encoder for one [HandFrame]: one line per frame.
 *
 * Pure Kotlin rather than org.json so it is unit-testable on the JVM, adds no
 * dependency and keeps formatting control explicit. The Python extraction path
 * parses these fields into a landmarks-v1 observation.
 */
object HandFrameJsonl {

    fun encode(frame: HandFrame): String = buildString {
        append("{\"timestampMs\":").append(frame.timestampMs)
        append(",\"imageWidth\":").append(frame.imageWidth)
        append(",\"imageHeight\":").append(frame.imageHeight)
        append(",\"rotationDegrees\":").append(frame.rotationDegrees)
        append(",\"frontCamera\":").append(frame.isFrontCamera)
        append(",\"hands\":[")
        frame.hands.forEachIndexed { index, hand ->
            if (index > 0) append(',')
            append("{\"handedness\":\"").append(hand.handedness.name).append('"')
            append(",\"handednessScore\":").append(hand.handednessScore)
            append(",\"landmarks\":[")
            hand.landmarks.forEachIndexed { point, landmark ->
                if (point > 0) append(',')
                appendLandmark(landmark)
            }
            append("]}")
        }
        append("]}")
    }

    private fun StringBuilder.appendLandmark(landmark: Landmark) {
        append('[')
            .append(landmark.x).append(',')
            .append(landmark.y).append(',')
            .append(landmark.z)
            .append(']')
    }
}
