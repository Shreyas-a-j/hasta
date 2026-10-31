package com.hasta.app.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.hasta.app.R
import com.hasta.app.ui.HandVisibility
import com.hasta.app.ui.theme.HastaTheme
import com.hasta.app.ui.theme.HastaTone

@Composable
fun HandVisibilityRow(
    visibility: HandVisibility,
    tone: HastaTone,
    modifier: Modifier = Modifier,
) {
    val colors = HastaTheme.colors
    val label = stringResource(
        when (visibility) {
            HandVisibility.None -> R.string.hand_visibility_none
            HandVisibility.One -> R.string.hand_visibility_one
            HandVisibility.Two -> R.string.hand_visibility_two
        },
    )
    val description = stringResource(R.string.hand_visibility_content_description, label)

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HastaIcon(
            name = HastaIconName.Hand,
            tint = colors.toneColor(tone),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.cameraText,
        )
    }
}
