package com.hasta.app.ui

import androidx.compose.runtime.Immutable
import com.hasta.app.domain.model.HandObservation

/**
 * High-frequency frame state. Only the preview/overlay subtree collects this, so
 * landmark updates do not recompose the coach panel.
 */
@Immutable
data class FrameUiState(
    val hands: List<HandObservation> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
)
