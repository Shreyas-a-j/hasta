package com.hasta.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hasta.app.domain.tracking.TrackingState
import com.hasta.app.ui.component.HandVisibilityRow
import com.hasta.app.ui.theme.HastaCameraTheme
import com.hasta.app.ui.theme.HastaTheme
import com.hasta.app.ui.theme.HastaTone
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveCoachAccessibilityTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(state: LiveCoachUiState) {
        compose.setContent {
            HastaCameraTheme(reducedMotion = false) {
                LiveCoachScreen(
                    state = state,
                    onAction = {},
                    stage = { Box(Modifier.size(393.dp, 852.dp)) },
                )
            }
        }
    }

    @Test
    fun primaryActionMeetsTheTouchTargetMinimum() {
        show(LiveCoachUiState(tracking = TrackingState.CameraReady))

        compose.onNodeWithText("Start practice").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun statusIsExposedAsStateNotColourAlone() {
        show(LiveCoachUiState(tracking = TrackingState.CameraReady))

        compose.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Camera ready",
            ),
        ).assertIsDisplayed()
    }

    @Test
    fun handVisibilityHasItsOwnContentDescription() {
        compose.setContent {
            HastaCameraTheme(reducedMotion = false) {
                HandVisibilityRow(
                    visibility = HandVisibility.One,
                    tone = HastaTone.Warning,
                )
            }
        }

        compose.onNodeWithContentDescription("Hand visibility: 1 of 2 hands")
            .assertIsDisplayed()
    }

    @Test
    fun largeFontScaleStillRendersThePanel() {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = base.density, fontScale = 2f),
            ) {
                HastaCameraTheme(reducedMotion = false) {
                    LiveCoachScreen(
                        state = LiveCoachUiState(tracking = TrackingState.TwoHands),
                        onAction = {},
                        stage = { Box(Modifier.size(393.dp, 852.dp)) },
                    )
                }
            }
        }

        compose.onNodeWithText("You are in frame. Start when you are ready.").assertIsDisplayed()
    }

    @Test
    fun reducedMotionRemovesDurationsButKeepsContent() {
        var reduced = false
        compose.setContent {
            HastaTheme(darkTheme = false, reducedMotion = true) {
                reduced = HastaTheme.motion.reduced
                LiveCoachScreen(
                    state = LiveCoachUiState(tracking = TrackingState.TwoHands),
                    onAction = {},
                    stage = { Box(Modifier.size(393.dp, 852.dp)) },
                )
            }
        }

        compose.waitForIdle()
        assertTrue(reduced)
        compose.onNodeWithText("You are in frame. Start when you are ready.").assertIsDisplayed()
    }

    @Test
    fun errorStatesUseAnIconAndTextRatherThanColourAlone() {
        show(LiveCoachUiState(tracking = TrackingState.TrackingError))

        compose.onNodeWithText("Hand tracking paused").assertIsDisplayed()
        compose.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Tracking paused",
            ),
        ).assertIsDisplayed()
    }
}
