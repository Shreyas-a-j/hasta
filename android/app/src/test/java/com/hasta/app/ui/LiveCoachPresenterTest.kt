package com.hasta.app.ui

import com.hasta.app.domain.tracking.TrackingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCoachPresenterTest {

    @Test
    fun cameraReadyShowsReadyPanelWithStartAction() {
        val model = present(TrackingState.CameraReady, PracticePhase.Idle)

        assertEquals(CoachPanelKind.Ready, model.kind)
        assertEquals(CoachAction.StartPractice, model.primaryAction)
        assertTrue(model.primaryEnabled)
        assertEquals(StatusLabel.CameraReady, model.statusLabel)
        assertTrue(model.framingGuide)
    }

    @Test
    fun noHandsIsNeutralGuidanceNotAnError() {
        val model = present(TrackingState.NoHands, PracticePhase.Idle)

        assertEquals(CoachPanelKind.NoHands, model.kind)
        assertEquals(com.hasta.app.ui.theme.HastaTone.Neutral, model.tone)
        assertEquals(StatusLabel.NoHands, model.statusLabel)
        assertEquals(HandVisibility.None, model.handVisibility)
        assertTrue(model.framingGuide)
    }

    @Test
    fun oneHandUsesWarningToneAndText() {
        val model = present(TrackingState.OneHand, PracticePhase.Idle)

        assertEquals(CoachPanelKind.OneHand, model.kind)
        assertEquals(com.hasta.app.ui.theme.HastaTone.Warning, model.tone)
        assertEquals(StatusLabel.OneHand, model.statusLabel)
        assertEquals(HandVisibility.One, model.handVisibility)
        assertFalse(model.framingGuide)
    }

    @Test
    fun twoHandsUsesSuccessToneAndText() {
        val model = present(TrackingState.TwoHands, PracticePhase.Idle)

        assertEquals(CoachPanelKind.TwoHands, model.kind)
        assertEquals(com.hasta.app.ui.theme.HastaTone.Success, model.tone)
        assertEquals(StatusLabel.TwoHands, model.statusLabel)
        assertEquals(HandVisibility.Two, model.handVisibility)
    }

    @Test
    fun initializingDoesNotOfferAnActionThatCannotWork() {
        val model = present(TrackingState.Initializing, PracticePhase.Idle)

        assertEquals(CoachPanelKind.Starting, model.kind)
        assertNull(model.primaryAction)
        assertEquals(StatusLabel.StartingCamera, model.statusLabel)
    }

    @Test
    fun trackingPausedOffersARetry() {
        val model = present(TrackingState.TrackingError, PracticePhase.Idle)

        assertEquals(CoachPanelKind.TrackingPaused, model.kind)
        assertEquals(CoachAction.RetryTracking, model.primaryAction)
        assertEquals(StatusLabel.TrackingPaused, model.statusLabel)
    }

    @Test
    fun cameraUnavailableOffersARetry() {
        val model = present(TrackingState.CameraUnavailable, PracticePhase.Idle)

        assertEquals(CoachPanelKind.CameraUnavailable, model.kind)
        assertEquals(CoachAction.RetryCamera, model.primaryAction)
        assertEquals(StatusLabel.CameraUnavailable, model.statusLabel)
    }

    @Test
    fun runningPracticeShowsProgressAndStop() {
        val practice = PracticeUiState(
            phase = PracticePhase.Running,
            windowMillis = 5_000,
            elapsedMillis = 2_500,
        )
        val model = present(TrackingState.TwoHands, practice)

        assertEquals(CoachPanelKind.Practising, model.kind)
        assertEquals(CoachAction.StopPractice, model.primaryAction)
        assertEquals(StatusLabel.Practising, model.statusLabel)
        assertEquals(0.5f, model.progress ?: -1f, 0.001f)
        assertEquals(3, model.remainingSeconds)
        assertEquals(HandVisibility.Two, model.handVisibility)
    }

    @Test
    fun losingHandsDuringPracticeIsAGuidanceStateNotAnError() {
        val practice = PracticeUiState(
            phase = PracticePhase.Running,
            windowMillis = 5_000,
            elapsedMillis = 1_000,
        )
        val model = present(TrackingState.NoHands, practice)

        assertEquals(CoachPanelKind.Practising, model.kind)
        assertEquals(StatusLabel.HandsOutOfView, model.statusLabel)
        assertEquals(com.hasta.app.ui.theme.HastaTone.Warning, model.tone)
        assertTrue(model.framingGuide)
        assertEquals(CoachAction.StopPractice, model.primaryAction)
    }

    @Test
    fun completedWindowOffersPractiseAgainAndNeverClaimsRecognition() {
        val practice = PracticeUiState(
            phase = PracticePhase.Complete,
            windowMillis = 5_000,
            elapsedMillis = 5_000,
        )
        val model = present(TrackingState.TwoHands, practice)

        assertEquals(CoachPanelKind.PracticeComplete, model.kind)
        assertEquals(CoachAction.PractiseAgain, model.primaryAction)
        assertEquals(StatusLabel.WindowComplete, model.statusLabel)
        assertEquals(1f, model.progress ?: -1f, 0.001f)
    }

    @Test
    fun cameraFailureWinsOverAnInFlightPracticeWindow() {
        val practice = PracticeUiState(phase = PracticePhase.Running, elapsedMillis = 1_000)
        val model = present(TrackingState.CameraUnavailable, practice)

        assertEquals(CoachPanelKind.CameraUnavailable, model.kind)
        assertEquals(CoachAction.RetryCamera, model.primaryAction)
    }

    @Test
    fun livePresenterNeverEmitsAFutureRecognitionState() {
        val future = setOf(
            CoachPanelKind.Evaluating,
            CoachPanelKind.Recognized,
            CoachPanelKind.Feedback,
            CoachPanelKind.LowConfidence,
        )
        val phases = listOf(
            PracticeUiState(phase = PracticePhase.Idle),
            PracticeUiState(phase = PracticePhase.Running),
            PracticeUiState(phase = PracticePhase.Complete),
        )
        val states = listOf(
            TrackingState.Initializing,
            TrackingState.PermissionRequired,
            TrackingState.CameraReady,
            TrackingState.NoHands,
            TrackingState.OneHand,
            TrackingState.TwoHands,
            TrackingState.TrackingError,
            TrackingState.CameraUnavailable,
        )

        states.forEach { tracking ->
            phases.forEach { practice ->
                val model = LiveCoachPresenter.present(LiveCoachUiState(tracking, practice))
                assertFalse("future state leaked: $model", model.kind in future)
            }
        }
    }

    @Test
    fun permissionRequiredMapsToThePermissionAction() {
        val model = present(TrackingState.PermissionRequired, PracticePhase.Idle)

        assertEquals(CoachPanelKind.PermissionRequired, model.kind)
        assertEquals(CoachAction.AllowCamera, model.primaryAction)
    }

    private fun present(tracking: TrackingState, phase: PracticePhase): CoachUiModel =
        present(tracking, PracticeUiState(phase = phase))

    private fun present(tracking: TrackingState, practice: PracticeUiState): CoachUiModel =
        LiveCoachPresenter.present(
            LiveCoachUiState(
                tracking = tracking,
                practice = practice,
            ),
        )
}
