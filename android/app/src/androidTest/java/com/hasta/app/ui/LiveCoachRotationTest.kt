package com.hasta.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hasta.app.domain.tracking.TrackingState
import com.hasta.app.ui.theme.HastaCameraTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Rotation must produce a clean layout in both orientations: the panel stays
 * clear of the top bar and keeps a readable column width. These checks use the
 * pure screen with a fixed-size stage, so no CameraX is involved.
 */
@RunWith(AndroidJUnit4::class)
class LiveCoachRotationTest {

    @get:Rule
    val compose = createComposeRule()

    private val size = mutableStateOf(393.dp to 852.dp)

    private fun show(width: Dp, height: Dp) {
        size.value = width to height
        compose.setContent {
            val (w, h) = size.value
            HastaCameraTheme(reducedMotion = false) {
                Box(Modifier.size(w, h)) {
                    LiveCoachScreen(
                        state = LiveCoachUiState(
                            tracking = TrackingState.TwoHands,
                            practice = PracticeUiState(
                                phase = PracticePhase.Running,
                                elapsedMillis = 1_000,
                            ),
                        ),
                        onAction = {},
                        stage = { Box(Modifier.size(w, h)) },
                    )
                }
            }
        }
    }

    @Test
    fun portraitKeepsThePanelAtTheBottomWithUsableMargins() {
        show(width = 393.dp, height = 852.dp)

        val panel = compose.onNodeWithTag(COACH_PANEL_TAG).getUnclippedBoundsInRoot()
        val topBar = compose.onNodeWithTag(LIVE_TOP_BAR_TAG).getUnclippedBoundsInRoot()

        compose.onNodeWithTag(COACH_PANEL_TAG).assertIsDisplayed()
        assertTrue("panel should sit in the lower half", panel.top > 393.dp)
        assertTrue("panel must stay inside the screen", panel.bottom <= 852.dp)
        assertTrue("panel must clear the top bar", panel.top > topBar.bottom)
        assertTrue("panel keeps horizontal margins", panel.left >= 8.dp && panel.right <= 385.dp)
    }

    @Test
    fun landscapeMovesThePanelToTheRightOfThePreview() {
        show(width = 852.dp, height = 393.dp)

        val panel = compose.onNodeWithTag(COACH_PANEL_TAG).getUnclippedBoundsInRoot()
        val topBar = compose.onNodeWithTag(LIVE_TOP_BAR_TAG).getUnclippedBoundsInRoot()

        compose.onNodeWithTag(COACH_PANEL_TAG).assertIsDisplayed()
        assertTrue("panel should occupy the right side", panel.left > 426.dp)
        assertTrue("panel must clear the top bar", panel.top > topBar.top)
        assertTrue("panel must stay inside the screen", panel.bottom <= 393.dp)
        assertTrue("panel must keep a readable width", panel.right - panel.left >= 240.dp)
        assertTrue("panel must not run past the right edge", panel.right <= 852.dp)
    }

    @Test
    fun rotatingAtRuntimeKeepsStatusAndActionVisible() {
        show(width = 393.dp, height = 852.dp)
        compose.onNodeWithText("Stop practice").assertIsDisplayed()

        compose.runOnIdle { size.value = 852.dp to 393.dp }

        compose.onNodeWithText("Stop practice").assertIsDisplayed()
        compose.onNodeWithText("Practising…").assertIsDisplayed()
        assertTrue(
            "panel should move to the right after rotation",
            compose.onNodeWithTag(COACH_PANEL_TAG).getUnclippedBoundsInRoot().left > 426.dp,
        )
    }
}
