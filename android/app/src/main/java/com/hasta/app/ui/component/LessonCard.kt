package com.hasta.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hasta.app.ui.theme.HastaRadius
import com.hasta.app.ui.theme.HastaSpacing
import com.hasta.app.ui.theme.HastaTheme

/**
 * Reusable design-system lesson card. It is not wired to a lesson engine; M0.5
 * only demonstrates the component with preview data.
 */
@Composable
fun LessonCard(
    title: String,
    subtitle: String,
    levelLabel: String,
    durationLabel: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    progressLabel: String? = null,
) {
    val colors = HastaTheme.colors

    HastaCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                color = colors.brandContainer,
                shape = RoundedCornerShape(HastaRadius.medium),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    HastaIcon(
                        name = HastaIconName.Hand,
                        tint = colors.brandAccent,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(HastaSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        color = colors.brandContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = levelLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onBrandContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = durationLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = HastaSpacing.xs),
                )
            }
        }

        if (progress != null) {
            Column(modifier = Modifier.padding(top = HastaSpacing.md)) {
                if (progressLabel != null) {
                    Text(
                        text = progressLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                }
                PracticeProgress(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HastaSpacing.xs),
                    contentDescription = progressLabel,
                    trackColor = colors.divider,
                    progressColor = colors.brandPrimary,
                )
            }
        }
    }
}
