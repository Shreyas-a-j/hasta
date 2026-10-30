package com.hasta.app.domain.model

/**
 * Subject-relative handedness, as reported by the tracker for unmirrored input.
 * Never derive this from screen position.
 */
enum class Handedness {
    LEFT,
    RIGHT,
    UNKNOWN,
}
