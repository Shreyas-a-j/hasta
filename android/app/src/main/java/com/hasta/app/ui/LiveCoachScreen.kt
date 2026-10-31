package com.hasta.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.hasta.app.ui.component.CameraCoachPanel
import com.hasta.app.ui.component.FramingGuide
import com.hasta.app.ui.component.HastaTopBar
import com.hasta.app.ui.component.TrackingStatusPill
import com.hasta.app.ui.theme.HastaSizes
import com.hasta.app.ui.theme.HastaSpacing
import com.hasta.app.ui.theme.HastaTheme

const val COACH_PANEL_TAG = "coach_panel"
const val LIVE_TOP_BAR_TAG = "live_top_bar"

/**
 * Pure Live Sign Coach layout. The camera preview and hand overlay are supplied
 * as [stage], so this screen can be rendered in Compose tests and previews
 * without CameraX.
 *
 * Portrait keeps the panel at the bottom and the signing area above it. Landscape
 * moves the panel to the right, below the top bar, so rotation never stacks the
 * controls over the centre of the preview.
 */
@Composable
fun LiveCoachScreen(
    state: LiveCoachUiState,
    onAction: (CoachAction) -> Unit,
    stage: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    debugOnLongClick: (() -> Unit)? = null,
) {
    val model = LiveCoachPresenter.present(state)
    val colors = HastaTheme.colors

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isLandscape = screenWidth > screenHeight
        val screenInset = if (isLandscape) HastaSpacing.lg else HastaSpacing.md

        Box(modifier = Modifier.fillMaxSize()) {
            stage()
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(if (isLandscape) 96.dp else 120.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.cameraScrim, Color.Transparent),
                    ),
                ),
        )

        // Chrome lives inside the safe area, and the panel's height budget is measured
        // from that same box. Measuring it against the full window (as an earlier
        // version did) let the landscape panel overflow the bottom of the screen and
        // clip its primary action.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val safeWidth = maxWidth
            val safeHeight = maxHeight
            // The bar occupies its minimum height plus its vertical padding.
            val panelTopGap = HastaSizes.topBar + HastaSpacing.xs * 2
            val panelMaxHeight = if (isLandscape) {
                (safeHeight - panelTopGap - HastaSpacing.sm).coerceAtLeast(200.dp)
            } else {
                safeHeight * 0.56f
            }

            HastaTopBar(
                onCamera = true,
                compact = isLandscape,
                status = {
                    TrackingStatusPill(
                        label = statusText(model.statusLabel),
                        tone = model.tone,
                        onLongClick = debugOnLongClick,
                    )
                },
                modifier = if (isLandscape) {
                    Modifier
                        .align(Alignment.TopStart)
                        .testTag(LIVE_TOP_BAR_TAG)
                        .padding(horizontal = screenInset, vertical = HastaSpacing.xs)
                } else {
                    Modifier
                        .align(Alignment.TopStart)
                        .testTag(LIVE_TOP_BAR_TAG)
                        .fillMaxWidth()
                        .padding(horizontal = screenInset, vertical = HastaSpacing.xs)
                },
            )

            if (model.framingGuide) {
                FramingGuide(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(
                            x = if (isLandscape) -(safeWidth * 0.10f) else 0.dp,
                            y = if (isLandscape) 0.dp else -(safeHeight * 0.08f),
                        )
                        .size(
                            width = if (isLandscape) safeWidth * 0.42f else safeWidth * 0.72f,
                            height = if (isLandscape) safeHeight * 0.62f else safeHeight * 0.34f,
                        ),
                )
            }

            CameraCoachPanel(
                model = model,
                onAction = onAction,
                maxHeight = panelMaxHeight,
                modifier = if (isLandscape) {
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            end = screenInset,
                            top = panelTopGap,
                        )
                        .testTag(COACH_PANEL_TAG)
                        .widthIn(max = 320.dp)
                } else {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = screenInset, vertical = HastaSpacing.sm)
                        .testTag(COACH_PANEL_TAG)
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                },
            )
        }
    }
}
