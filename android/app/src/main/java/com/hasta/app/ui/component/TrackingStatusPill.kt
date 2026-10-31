package com.hasta.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hasta.app.R
import com.hasta.app.ui.theme.HastaTheme
import com.hasta.app.ui.theme.HastaTone

/**
 * Status is always a text label plus a glyph, so it never relies on colour.
 * The long-press is the hidden debug benchmark trigger and is null in release.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackingStatusPill(
    label: String,
    tone: HastaTone,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = HastaTheme.colors
    val toneColor = colors.toneColor(tone)
    val gesture = if (onLongClick == null) {
        Modifier
    } else {
        Modifier.combinedClickable(
            onClick = {},
            onLongClickLabel = stringResource(R.string.benchmark_long_press_label),
            onLongClick = onLongClick,
        )
    }

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(gesture)
            .semantics(mergeDescendants = true) { stateDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = colors.cameraPanel,
            shape = CircleShape,
            border = BorderStroke(1.dp, colors.cameraGuide.copy(alpha = 0.18f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (tone) {
                    HastaTone.Success -> HastaIcon(
                        name = HastaIconName.Check,
                        tint = toneColor,
                        modifier = Modifier.size(16.dp),
                    )

                    HastaTone.Warning,
                    HastaTone.Error,
                    -> HastaIcon(
                        name = HastaIconName.Alert,
                        tint = toneColor,
                        modifier = Modifier.size(16.dp),
                    )

                    HastaTone.Neutral,
                    HastaTone.Brand,
                    -> Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(toneColor, CircleShape),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.cameraText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
