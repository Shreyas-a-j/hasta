package com.hasta.app.domain.tracking

/**
 * MediaPipe hand topology (21 landmarks, 21 connections), owned by HASTA so the UI
 * does not depend on MediaPipe types.
 *
 * The indices are fixed by the model contract:
 * 0 wrist; 1-4 thumb; 5-8 index; 9-12 middle; 13-16 ring; 17-20 pinky.
 */
object HandTopology {
    const val LANDMARK_COUNT = 21

    val connections: List<Pair<Int, Int>> = listOf(
        // Thumb
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        // Index
        0 to 5, 5 to 6, 6 to 7, 7 to 8,
        // Middle
        5 to 9, 9 to 10, 10 to 11, 11 to 12,
        // Ring
        9 to 13, 13 to 14, 14 to 15, 15 to 16,
        // Pinky
        13 to 17, 17 to 18, 18 to 19, 19 to 20,
        // Palm base
        0 to 17,
    )
}
