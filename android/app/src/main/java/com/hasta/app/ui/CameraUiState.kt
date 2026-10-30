package com.hasta.app.ui

import com.hasta.app.domain.model.HandObservation
import com.hasta.app.domain.tracking.TrackingState

/**
 * Minimal immutable UI state. Only what the screen draws.
 */
data class CameraUiState(
    val tracking: TrackingState = TrackingState.Initializing,
    val hands: List<HandObservation> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val message: String? = null,
)
