package com.hasta.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasta.app.R
import com.hasta.app.ui.theme.HastaTheme

@Composable
fun BrandLockup(
    onCamera: Boolean,
    modifier: Modifier = Modifier,
    showTagline: Boolean = false,
) {
    val colors = HastaTheme.colors
    val primary = if (onCamera) colors.cameraText else colors.textPrimary
    val secondary = if (onCamera) colors.cameraTextSecondary else colors.textSecondary
    val description = stringResource(
        if (showTagline) R.string.brand_tagline_content_description
        else R.string.brand_live_coach_content_description,
    )

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.brandPrimary),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = stringResource(R.string.brand_name),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                ),
                color = primary,
            )
            Text(
                text = stringResource(
                    if (showTagline) R.string.brand_tagline else R.string.live_coach_subtitle,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = secondary,
            )
        }
    }
}
