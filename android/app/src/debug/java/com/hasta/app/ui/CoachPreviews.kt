package com.hasta.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hasta.app.R
import com.hasta.app.domain.tracking.TrackingState
import com.hasta.app.ui.component.CameraCoachPanel
import com.hasta.app.ui.component.LessonCard
import com.hasta.app.ui.theme.HastaCameraTheme
import com.hasta.app.ui.theme.HastaTheme
import com.hasta.app.ui.theme.HastaTone

/**
 * Debug-only design previews. Nothing in this file is reachable from the live
 * pipeline: [LiveCoachPresenter] cannot emit the future recognition states below,
 * and the release build does not include the Android Studio preview tooling.
 */
private fun model(
    kind: CoachPanelKind,
    tone: HastaTone,
    status: StatusLabel,
    hands: HandVisibility = HandVisibility.None,
    action: CoachAction? = null,
    progress: Float? = null,
    remaining: Int? = null,
) = CoachUiModel(
    kind = kind,
    tone = tone,
    handVisibility = hands,
    framingGuide = false,
    primaryAction = action,
    progress = progress,
    remainingSeconds = remaining,
    statusLabel = status,
)

private val PreviewStage: @Composable BoxScope.() -> Unit = {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2B2533), Color(0xFF141218)),
                ),
            ),
    )
}

@Composable
private fun CoachPreview(state: LiveCoachUiState) {
    HastaCameraTheme(reducedMotion = false) {
        LiveCoachScreen(
            state = state,
            onAction = {},
            stage = PreviewStage,
        )
    }
}

@Preview(name = "Ready", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ReadyPreview() {
    CoachPreview(LiveCoachUiState(tracking = TrackingState.CameraReady))
}

@Preview(name = "No hands", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun NoHandsPreview() {
    CoachPreview(LiveCoachUiState(tracking = TrackingState.NoHands))
}

@Preview(name = "One hand", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun OneHandPreview() {
    CoachPreview(LiveCoachUiState(tracking = TrackingState.OneHand))
}

@Preview(name = "Two hands", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun TwoHandsPreview() {
    CoachPreview(LiveCoachUiState(tracking = TrackingState.TwoHands))
}

@Preview(name = "Practising", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PractisingPreview() {
    CoachPreview(
        LiveCoachUiState(
            tracking = TrackingState.TwoHands,
            practice = PracticeUiState(
                phase = PracticePhase.Running,
                elapsedMillis = 2_000,
            ),
        ),
    )
}

@Preview(name = "Landscape", showBackground = true, widthDp = 852, heightDp = 393)
@Composable
private fun LandscapePreview() {
    CoachPreview(
        LiveCoachUiState(
            tracking = TrackingState.TwoHands,
            practice = PracticeUiState(
                phase = PracticePhase.Running,
                elapsedMillis = 4_000,
            ),
        ),
    )
}

@Preview(name = "Tracking paused", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun TrackingPausedPreview() {
    CoachPreview(LiveCoachUiState(tracking = TrackingState.TrackingError))
}

@Preview(name = "Camera unavailable", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun CameraUnavailablePreview() {
    CoachPreview(LiveCoachUiState(tracking = TrackingState.CameraUnavailable))
}

/** Future state, preview only. The live presenter never produces this model. */
@Preview(name = "Future: evaluating", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun EvaluatingPreview() {
    HastaCameraTheme(reducedMotion = false) {
        Box(Modifier.fillMaxSize().background(Color(0xFF141218))) {
            CameraCoachPanel(
                model = model(
                    kind = CoachPanelKind.Evaluating,
                    tone = HastaTone.Brand,
                    status = StatusLabel.Practising,
                ),
                onAction = {},
            )
        }
    }
}

/** Future state, preview only. The live presenter never produces this model. */
@Preview(name = "Future: recognised", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun RecognizedPreview() {
    HastaCameraTheme(reducedMotion = false) {
        Box(Modifier.fillMaxSize().background(Color(0xFF141218))) {
            CameraCoachPanel(
                model = model(
                    kind = CoachPanelKind.Recognized,
                    tone = HastaTone.Success,
                    status = StatusLabel.WindowComplete,
                ),
                onAction = {},
            )
        }
    }
}

/** Future state, preview only. The live presenter never produces this model. */
@Preview(name = "Future: feedback", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun FeedbackPreview() {
    HastaCameraTheme(reducedMotion = false) {
        Box(Modifier.fillMaxSize().background(Color(0xFF141218))) {
            CameraCoachPanel(
                model = model(
                    kind = CoachPanelKind.Feedback,
                    tone = HastaTone.Brand,
                    status = StatusLabel.WindowComplete,
                ),
                onAction = {},
            )
        }
    }
}

/** Future state, preview only. The live presenter never produces this model. */
@Preview(name = "Future: low confidence", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun LowConfidencePreview() {
    HastaCameraTheme(reducedMotion = false) {
        Box(Modifier.fillMaxSize().background(Color(0xFF141218))) {
            CameraCoachPanel(
                model = model(
                    kind = CoachPanelKind.LowConfidence,
                    tone = HastaTone.Warning,
                    status = StatusLabel.WindowComplete,
                ),
                onAction = {},
            )
        }
    }
}

@Preview(name = "Permission", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PermissionPreview() {
    HastaTheme(darkTheme = false, reducedMotion = false) {
        PermissionScreen(
            kind = PermissionScreenKind.Rationale,
            onAllow = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "Permission, dark", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PermissionDarkPreview() {
    HastaTheme(darkTheme = true, reducedMotion = false) {
        PermissionScreen(
            kind = PermissionScreenKind.PermanentlyDenied,
            onAllow = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "Lesson card", showBackground = true, widthDp = 393)
@Composable
private fun LessonCardPreview() {
    HastaTheme(darkTheme = false, reducedMotion = false) {
        Box(Modifier.background(HastaTheme.colors.background)) {
            LessonCard(
                title = stringResource(R.string.lesson_preview_title),
                subtitle = stringResource(R.string.lesson_preview_subtitle),
                levelLabel = stringResource(R.string.lesson_preview_level),
                durationLabel = stringResource(R.string.lesson_preview_duration),
                progress = 0.4f,
                progressLabel = stringResource(R.string.lesson_preview_progress),
                modifier = Modifier.fillMaxSize(0.9f),
            )
        }
    }
}
