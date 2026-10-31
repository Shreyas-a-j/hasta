package com.hasta.app.ui

import androidx.compose.runtime.Immutable
import com.hasta.app.domain.tracking.TrackingState
import kotlin.math.ceil

const val PRACTICE_WINDOW_MILLIS = 5_000L

enum class PracticePhase { Idle, Running, Complete }

/**
 * UI-only practice window. It represents the learner practising in front of the
 * camera; it is not sign capture and it does not feed any recogniser.
 */
@Immutable
data class PracticeUiState(
    val phase: PracticePhase = PracticePhase.Idle,
    val windowMillis: Long = PRACTICE_WINDOW_MILLIS,
    val elapsedMillis: Long = 0L,
) {
    val progress: Float
        get() = if (windowMillis <= 0L) 1f else (elapsedMillis.toFloat() / windowMillis).coerceIn(0f, 1f)

    val remainingMillis: Long
        get() = (windowMillis - elapsedMillis).coerceAtLeast(0L)

    val remainingSeconds: Int
        get() = ceil(remainingMillis / 1_000.0).toInt()

    fun start(): PracticeUiState = copy(phase = PracticePhase.Running, elapsedMillis = 0L)

    fun advance(deltaMillis: Long): PracticeUiState {
        if (phase != PracticePhase.Running) return this
        val next = elapsedMillis + deltaMillis.coerceAtLeast(0L)
        return if (next >= windowMillis) {
            copy(phase = PracticePhase.Complete, elapsedMillis = windowMillis)
        } else {
            copy(elapsedMillis = next)
        }
    }

    fun stop(): PracticeUiState = PracticeUiState(phase = PracticePhase.Idle, windowMillis = windowMillis)

    fun again(): PracticeUiState = stop().start()
}

@Immutable
data class LiveCoachUiState(
    val tracking: TrackingState = TrackingState.Initializing,
    val practice: PracticeUiState = PracticeUiState(),
)

/**
 * The live screen can only produce these panel kinds. Recognition states are
 * deliberately absent from [LiveCoachPresenter] and appear only in debug previews.
 */
enum class CoachPanelKind {
    Starting,
    Ready,
    NoHands,
    OneHand,
    TwoHands,
    Practising,
    PracticeComplete,
    TrackingPaused,
    CameraUnavailable,
    PermissionRequired,
    Evaluating,
    Recognized,
    Feedback,
    LowConfidence,
}

enum class CoachAction {
    StartPractice,
    StopPractice,
    PractiseAgain,
    RetryTracking,
    RetryCamera,
    AllowCamera,
    OpenSettings,
}

enum class HandVisibility { None, One, Two }

enum class StatusLabel {
    StartingCamera,
    CameraReady,
    NoHands,
    OneHand,
    TwoHands,
    Practising,
    HandsOutOfView,
    WindowComplete,
    TrackingPaused,
    CameraUnavailable,
    PermissionRequired,
}
