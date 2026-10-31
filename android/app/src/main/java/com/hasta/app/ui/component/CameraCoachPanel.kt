package com.hasta.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hasta.app.R
import com.hasta.app.ui.CoachAction
import com.hasta.app.ui.CoachPanelKind
import com.hasta.app.ui.CoachUiModel
import com.hasta.app.ui.HandVisibility
import com.hasta.app.ui.actionAccessibilityText
import com.hasta.app.ui.actionText
import com.hasta.app.ui.coachText
import com.hasta.app.ui.theme.HastaSpacing
import com.hasta.app.ui.theme.HastaTheme

private val PREVIEW_ONLY_KINDS = setOf(
    CoachPanelKind.Evaluating,
    CoachPanelKind.Recognized,
    CoachPanelKind.Feedback,
    CoachPanelKind.LowConfidence,
)

private val HAND_VISIBILITY_KINDS = setOf(
    CoachPanelKind.Ready,
    CoachPanelKind.NoHands,
    CoachPanelKind.OneHand,
    CoachPanelKind.TwoHands,
    CoachPanelKind.Practising,
)

/**
 * The camera overlay/feedback panel. It is a solid dark surface rather than a
 * translucent blur so text contrast never depends on the camera image.
 *
 * Future recognition panels are rendered here only from debug previews/tests and
 * carry an explicit "Preview only" badge. The live presenter cannot emit them.
 */
@Composable
fun CameraCoachPanel(
    model: CoachUiModel,
    onAction: (CoachAction) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = Dp.Unspecified,
) {
    val colors = HastaTheme.colors
    val text = coachText(model.kind, model.handVisibility == HandVisibility.None)
    val previewOnly = model.kind in PREVIEW_ONLY_KINDS
    val sizedModifier = if (maxHeight == Dp.Unspecified) {
        modifier
    } else {
        modifier.heightIn(max = maxHeight)
    }

    Surface(
        modifier = sizedModifier,
        color = colors.cameraPanel,
        contentColor = colors.cameraText,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, colors.cameraGuide.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(HastaSpacing.lg),
        ) {
            if (previewOnly) {
                PreviewBadge()
                Spacer(Modifier.padding(top = HastaSpacing.md))
            }

            if (model.kind in HAND_VISIBILITY_KINDS) {
                HandVisibilityRow(
                    visibility = model.handVisibility,
                    tone = model.tone,
                )
                Spacer(Modifier.padding(top = HastaSpacing.sm))
            }

            Text(
                text = text.title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.cameraText,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.padding(top = HastaSpacing.xs))
            Text(
                text = text.body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.cameraTextSecondary,
            )

            if (model.kind == CoachPanelKind.Practising && model.progress != null) {
                Spacer(Modifier.padding(top = HastaSpacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.practice_time_format,
                            model.remainingSeconds ?: 0,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.cameraText,
                    )
                }
                PracticeProgress(
                    progress = model.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HastaSpacing.xs),
                    contentDescription = stringResource(R.string.practice_progress_label),
                )
            }

            if (text.footnote != null) {
                Spacer(Modifier.padding(top = HastaSpacing.sm))
                Text(
                    text = text.footnote,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.cameraTextSecondary,
                )
            }

            val action = model.primaryAction
            if (action != null) {
                Spacer(Modifier.padding(top = HastaSpacing.md))
                HastaButton(
                    text = actionText(action),
                    onClick = { onAction(action) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = if (action == CoachAction.StopPractice) {
                        HastaButtonVariant.Secondary
                    } else {
                        HastaButtonVariant.Primary
                    },
                    enabled = model.primaryEnabled,
                    icon = actionIcon(action),
                    contentDescription = actionAccessibilityText(action),
                )
            }
        }
    }
}

@Composable
private fun PreviewBadge() {
    val colors = HastaTheme.colors
    Surface(
        color = colors.brandContainer,
        contentColor = colors.onBrandContainer,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HastaIcon(
                name = HastaIconName.Info,
                tint = colors.onBrandContainer,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.preview_badge),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onBrandContainer,
            )
        }
    }
}

private fun actionIcon(action: CoachAction): HastaIconName = when (action) {
    CoachAction.StartPractice,
    CoachAction.PractiseAgain,
    -> HastaIconName.Play

    CoachAction.StopPractice -> HastaIconName.Stop
    CoachAction.RetryTracking,
    CoachAction.RetryCamera,
    -> HastaIconName.Retry

    CoachAction.AllowCamera -> HastaIconName.Camera
    CoachAction.OpenSettings -> HastaIconName.Settings
}
