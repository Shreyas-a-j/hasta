package com.hasta.app.domain.tracking

/**
 * User-visible tracking states for the M0 screen. These are deliberately small;
 * recognition states do not exist in M0.
 */
sealed interface TrackingState {
    data object Initializing : TrackingState
    data object PermissionRequired : TrackingState
    data object CameraReady : TrackingState
    data object NoHands : TrackingState
    data object OneHand : TrackingState
    data object TwoHands : TrackingState
    data object TrackingError : TrackingState
    data object CameraUnavailable : TrackingState

    companion object {
        /** Maps a validated hand count (0..2) to a tracking state. */
        fun fromHandCount(count: Int): TrackingState = when (count) {
            0 -> NoHands
            1 -> OneHand
            2 -> TwoHands
            else -> throw IllegalArgumentException("Unsupported hand count: $count")
        }
    }
}
