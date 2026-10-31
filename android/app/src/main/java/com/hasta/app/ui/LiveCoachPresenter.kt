package com.hasta.app.ui

import androidx.compose.runtime.Immutable
import com.hasta.app.domain.tracking.TrackingState
import com.hasta.app.ui.theme.HastaTone

/**
 * Pure presentation model. Strings are resolved in Compose so this mapping can be
 * unit-tested without Android resources.
 */
@Immutable
data class CoachUiModel(
    val kind: CoachPanelKind,
    val tone: HastaTone,
    val handVisibility: HandVisibility,
    val framingGuide: Boolean,
    val primaryAction: CoachAction?,
    val primaryEnabled: Boolean = true,
    val secondaryAction: CoachAction? = null,
    val progress: Float? = null,
    val remainingSeconds: Int? = null,
    val statusLabel: StatusLabel,
)

object LiveCoachPresenter {

    fun present(state: LiveCoachUiState): CoachUiModel {
        // A camera or tracker failure is always more important than an in-flight
        // practice window, and must never look like an assessment result.
        return when (state.tracking) {
            TrackingState.CameraUnavailable -> errorModel(
                kind = CoachPanelKind.CameraUnavailable,
                action = CoachAction.RetryCamera,
                status = StatusLabel.CameraUnavailable,
            )

            TrackingState.TrackingError -> errorModel(
                kind = CoachPanelKind.TrackingPaused,
                action = CoachAction.RetryTracking,
                status = StatusLabel.TrackingPaused,
            )

            TrackingState.PermissionRequired -> CoachUiModel(
                kind = CoachPanelKind.PermissionRequired,
                tone = HastaTone.Neutral,
                handVisibility = HandVisibility.None,
                framingGuide = false,
                primaryAction = CoachAction.AllowCamera,
                statusLabel = StatusLabel.PermissionRequired,
            )

            TrackingState.Initializing -> CoachUiModel(
                kind = CoachPanelKind.Starting,
                tone = HastaTone.Neutral,
                handVisibility = HandVisibility.None,
                framingGuide = false,
                primaryAction = null,
                statusLabel = StatusLabel.StartingCamera,
            )

            else -> when (state.practice.phase) {
                PracticePhase.Running -> practising(state)
                PracticePhase.Complete -> complete(state)
                PracticePhase.Idle -> idle(state.tracking)
            }
        }
    }

    private fun idle(tracking: TrackingState): CoachUiModel = when (tracking) {
        TrackingState.CameraReady -> CoachUiModel(
            kind = CoachPanelKind.Ready,
            tone = HastaTone.Neutral,
            handVisibility = HandVisibility.None,
            framingGuide = true,
            primaryAction = CoachAction.StartPractice,
            statusLabel = StatusLabel.CameraReady,
        )

        TrackingState.NoHands -> CoachUiModel(
            kind = CoachPanelKind.NoHands,
            tone = HastaTone.Neutral,
            handVisibility = HandVisibility.None,
            framingGuide = true,
            primaryAction = CoachAction.StartPractice,
            statusLabel = StatusLabel.NoHands,
        )

        TrackingState.OneHand -> CoachUiModel(
            kind = CoachPanelKind.OneHand,
            tone = HastaTone.Warning,
            handVisibility = HandVisibility.One,
            framingGuide = false,
            primaryAction = CoachAction.StartPractice,
            statusLabel = StatusLabel.OneHand,
        )

        TrackingState.TwoHands -> CoachUiModel(
            kind = CoachPanelKind.TwoHands,
            tone = HastaTone.Success,
            handVisibility = HandVisibility.Two,
            framingGuide = false,
            primaryAction = CoachAction.StartPractice,
            statusLabel = StatusLabel.TwoHands,
        )

        else -> CoachUiModel(
            kind = CoachPanelKind.Starting,
            tone = HastaTone.Neutral,
            handVisibility = HandVisibility.None,
            framingGuide = false,
            primaryAction = null,
            statusLabel = StatusLabel.StartingCamera,
        )
    }

    private fun practising(state: LiveCoachUiState): CoachUiModel {
        val hands = handVisibility(state.tracking)
        val handsMissing = hands == HandVisibility.None
        return CoachUiModel(
            kind = CoachPanelKind.Practising,
            tone = if (handsMissing) HastaTone.Warning else HastaTone.Success,
            handVisibility = hands,
            framingGuide = handsMissing,
            primaryAction = CoachAction.StopPractice,
            progress = state.practice.progress,
            remainingSeconds = state.practice.remainingSeconds,
            statusLabel = if (handsMissing) StatusLabel.HandsOutOfView else StatusLabel.Practising,
        )
    }

    private fun complete(state: LiveCoachUiState): CoachUiModel = CoachUiModel(
        kind = CoachPanelKind.PracticeComplete,
        tone = HastaTone.Neutral,
        handVisibility = handVisibility(state.tracking),
        framingGuide = false,
        primaryAction = CoachAction.PractiseAgain,
        progress = 1f,
        remainingSeconds = 0,
        statusLabel = StatusLabel.WindowComplete,
    )

    private fun errorModel(
        kind: CoachPanelKind,
        action: CoachAction,
        status: StatusLabel,
    ): CoachUiModel = CoachUiModel(
        kind = kind,
        tone = HastaTone.Error,
        handVisibility = HandVisibility.None,
        framingGuide = false,
        primaryAction = action,
        statusLabel = status,
    )

    private fun handVisibility(tracking: TrackingState): HandVisibility = when (tracking) {
        TrackingState.OneHand -> HandVisibility.One
        TrackingState.TwoHands -> HandVisibility.Two
        else -> HandVisibility.None
    }
}
