package com.hasta.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hasta.app.domain.tracking.TrackingState
import com.hasta.app.ui.theme.HastaCameraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every state the live presenter can produce must be renderable and readable.
 * The camera stage is replaced with an empty box, so these tests never touch
 * CameraX or MediaPipe.
 */
@RunWith(AndroidJUnit4::class)
class LiveCoachStateRenderingTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(state: LiveCoachUiState) {
        compose.setContent {
            HastaCameraTheme(reducedMotion = false) {
                LiveCoachScreen(
                    state = state,
                    onAction = {},
                    stage = { Box(Modifier.fillMaxSize()) },
                )
            }
        }
    }

    @Test
    fun readyStateExplainsHandsAndStartAction() {
        show(LiveCoachUiState(tracking = TrackingState.CameraReady))

        compose.onNodeWithText("Ready when you are").assertIsDisplayed()
        compose.onNodeWithText("Start practice").assertIsDisplayed()
    }

    @Test
    fun noHandsStateIsGuidanceAndOffersStart() {
        show(LiveCoachUiState(tracking = TrackingState.NoHands))

        compose.onNodeWithText("Bring your hands into view").assertIsDisplayed()
        compose.onNodeWithText("Start practice").assertIsDisplayed()
    }

    @Test
    fun oneHandStateNamesTheVisibleHand() {
        show(LiveCoachUiState(tracking = TrackingState.OneHand))

        compose.onNodeWithText("One hand visible").assertIsDisplayed()
        compose.onNodeWithText("1 of 2 hands").assertIsDisplayed()
    }

    @Test
    fun twoHandsStateConfirmsFraming() {
        show(LiveCoachUiState(tracking = TrackingState.TwoHands))

        compose.onNodeWithText("2 of 2 hands").assertIsDisplayed()
    }

    @Test
    fun startingStateDoesNotOfferAnActionThatCannotWork() {
        show(LiveCoachUiState(tracking = TrackingState.Initializing))

        compose.onNodeWithText("Starting camera").assertIsDisplayed()
        compose.onNodeWithText("Start practice").assertDoesNotExist()
    }

    @Test
    fun practisingStateShowsProgressAndStop() {
        show(
            LiveCoachUiState(
                tracking = TrackingState.TwoHands,
                practice = PracticeUiState(
                    phase = PracticePhase.Running,
                    elapsedMillis = 2_000,
                ),
            ),
        )

        compose.onNodeWithText("Practising…").assertIsDisplayed()
        compose.onNodeWithText("Stop practice").assertIsDisplayed()
        compose.onNodeWithText("0:03").assertIsDisplayed()
    }

    @Test
    fun losingHandsMidWindowIsGuidanceNotFailure() {
        show(
            LiveCoachUiState(
                tracking = TrackingState.NoHands,
                practice = PracticeUiState(
                    phase = PracticePhase.Running,
                    elapsedMillis = 1_000,
                ),
            ),
        )

        compose.onNodeWithText("Hands out of view").assertIsDisplayed()
        compose.onNodeWithText("Bring your hands back into view. The window keeps running.")
            .assertIsDisplayed()
    }

    @Test
    fun completeStateNeverClaimsRecognition() {
        show(
            LiveCoachUiState(
                tracking = TrackingState.TwoHands,
                practice = PracticeUiState(
                    phase = PracticePhase.Complete,
                    elapsedMillis = 5_000,
                ),
            ),
        )

        compose.onNodeWithText("Practice window complete").assertIsDisplayed()
        compose.onNodeWithText("No sign was recognised or saved.").assertIsDisplayed()
        compose.onNodeWithText("Practise again").assertIsDisplayed()
    }

    @Test
    fun trackingPausedOffersARetry() {
        show(LiveCoachUiState(tracking = TrackingState.TrackingError))

        compose.onNodeWithText("Hand tracking paused").assertIsDisplayed()
        compose.onNodeWithText("Try tracking again").assertIsDisplayed()
    }

    @Test
    fun cameraUnavailableOffersARetry() {
        show(LiveCoachUiState(tracking = TrackingState.CameraUnavailable))

        compose.onNodeWithText("Camera could not start").assertIsDisplayed()
        compose.onNodeWithText("Try camera again").assertIsDisplayed()
    }

    @Test
    fun liveScreenNeverShowsDebugMetrics() {
        show(LiveCoachUiState(tracking = TrackingState.TwoHands))

        compose.onNodeWithText("FPS").assertDoesNotExist()
        compose.onNodeWithText("latency", substring = true).assertDoesNotExist()
        compose.onNodeWithText("confidence", substring = true).assertDoesNotExist()
    }
}
