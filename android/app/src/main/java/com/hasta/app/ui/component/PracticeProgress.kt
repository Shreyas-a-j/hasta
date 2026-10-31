package com.hasta.app.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hasta.app.ui.theme.HastaSizes
import com.hasta.app.ui.theme.HastaTheme

/**
 * Slim determinate progress. The animated value is read in the draw phase so a
 * five-second practice window does not relayout the panel on every frame.
 */
@Composable
fun PracticeProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    trackColor: Color? = null,
    progressColor: Color? = null,
) {
    val colors = HastaTheme.colors
    val target = progress.coerceIn(0f, 1f)
    val animated = animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = HastaTheme.motion.durationFor(HastaTheme.motion.standardMs),
            easing = FastOutSlowInEasing,
        ),
        label = "practice-progress",
    )
    val track = trackColor ?: colors.cameraGuide.copy(alpha = 0.16f)
    val fill = progressColor ?: colors.brandPrimary

    Box(
        modifier = modifier
            .height(HastaSizes.progressTrack)
            .clip(CircleShape)
            .background(track)
            .drawBehind {
                val width = size.width * animated.value
                if (width > 0f) {
                    drawRoundRect(
                        color = fill,
                        size = Size(width, size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                }
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
                if (contentDescription != null) this.contentDescription = contentDescription
            },
    )
}
