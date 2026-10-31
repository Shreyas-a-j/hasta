package com.hasta.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hasta.app.R

internal data class CoachText(
    val title: String,
    val body: String,
    val footnote: String?,
)

@Composable
internal fun coachText(kind: CoachPanelKind, handsMissing: Boolean): CoachText = when (kind) {
    CoachPanelKind.Starting -> CoachText(
        title = stringResource(R.string.coach_starting_title),
        body = stringResource(R.string.coach_starting_body),
        footnote = null,
    )

    CoachPanelKind.Ready -> CoachText(
        title = stringResource(R.string.coach_ready_title),
        body = stringResource(R.string.coach_ready_body),
        footnote = stringResource(R.string.coach_ready_footnote),
    )

    CoachPanelKind.NoHands -> CoachText(
        title = stringResource(R.string.coach_no_hands_title),
        body = stringResource(R.string.coach_no_hands_body),
        footnote = null,
    )

    CoachPanelKind.OneHand -> CoachText(
        title = stringResource(R.string.coach_one_hand_title),
        body = stringResource(R.string.coach_one_hand_body),
        footnote = null,
    )

    CoachPanelKind.TwoHands -> CoachText(
        title = stringResource(R.string.coach_two_hands_title),
        body = stringResource(R.string.coach_two_hands_body),
        footnote = null,
    )

    CoachPanelKind.Practising -> CoachText(
        title = stringResource(R.string.coach_practising_title),
        body = stringResource(
            if (handsMissing) R.string.coach_practising_body_no_hands
            else R.string.coach_practising_body,
        ),
        footnote = stringResource(R.string.coach_practising_footnote),
    )

    CoachPanelKind.PracticeComplete -> CoachText(
        title = stringResource(R.string.coach_complete_title),
        body = stringResource(R.string.coach_complete_body),
        footnote = stringResource(R.string.coach_complete_footnote),
    )

    CoachPanelKind.TrackingPaused -> CoachText(
        title = stringResource(R.string.coach_tracking_paused_title),
        body = stringResource(R.string.coach_tracking_paused_body),
        footnote = null,
    )

    CoachPanelKind.CameraUnavailable -> CoachText(
        title = stringResource(R.string.coach_camera_unavailable_title),
        body = stringResource(R.string.coach_camera_unavailable_body),
        footnote = null,
    )

    CoachPanelKind.PermissionRequired -> CoachText(
        title = stringResource(R.string.coach_permission_title),
        body = stringResource(R.string.coach_permission_body),
        footnote = null,
    )

    CoachPanelKind.Evaluating -> CoachText(
        title = stringResource(R.string.future_evaluating_title),
        body = stringResource(R.string.future_evaluating_body),
        footnote = null,
    )

    CoachPanelKind.Recognized -> CoachText(
        title = stringResource(R.string.future_recognized_title),
        body = stringResource(R.string.future_recognized_body),
        footnote = null,
    )

    CoachPanelKind.Feedback -> CoachText(
        title = stringResource(R.string.future_feedback_title),
        body = stringResource(R.string.future_feedback_body),
        footnote = null,
    )

    CoachPanelKind.LowConfidence -> CoachText(
        title = stringResource(R.string.future_low_confidence_title),
        body = stringResource(R.string.future_low_confidence_body),
        footnote = null,
    )
}

@Composable
internal fun statusText(label: StatusLabel): String = stringResource(
    when (label) {
        StatusLabel.StartingCamera -> R.string.status_starting_camera
        StatusLabel.CameraReady -> R.string.status_camera_ready
        StatusLabel.NoHands -> R.string.status_no_hands
        StatusLabel.OneHand -> R.string.status_one_hand
        StatusLabel.TwoHands -> R.string.status_two_hands
        StatusLabel.Practising -> R.string.status_practising
        StatusLabel.HandsOutOfView -> R.string.status_hands_out_of_view
        StatusLabel.WindowComplete -> R.string.status_window_complete
        StatusLabel.TrackingPaused -> R.string.status_tracking_paused
        StatusLabel.CameraUnavailable -> R.string.status_camera_unavailable
        StatusLabel.PermissionRequired -> R.string.status_permission_required
    },
)

@Composable
internal fun actionText(action: CoachAction): String = stringResource(
    when (action) {
        CoachAction.StartPractice -> R.string.action_start_practice
        CoachAction.StopPractice -> R.string.action_stop_practice
        CoachAction.PractiseAgain -> R.string.action_practise_again
        CoachAction.RetryTracking -> R.string.action_retry_tracking
        CoachAction.RetryCamera -> R.string.action_retry_camera
        CoachAction.AllowCamera -> R.string.action_allow_camera
        CoachAction.OpenSettings -> R.string.action_open_settings
    },
)

@Composable
internal fun actionAccessibilityText(action: CoachAction): String? = when (action) {
    CoachAction.StartPractice -> stringResource(R.string.a11y_start_practice)
    CoachAction.StopPractice -> stringResource(R.string.a11y_stop_practice)
    CoachAction.RetryTracking -> stringResource(R.string.a11y_retry_tracking)
    CoachAction.RetryCamera -> stringResource(R.string.a11y_retry_camera)
    CoachAction.AllowCamera -> stringResource(R.string.a11y_allow_camera)
    CoachAction.OpenSettings -> stringResource(R.string.a11y_open_settings)
    CoachAction.PractiseAgain -> null
}
