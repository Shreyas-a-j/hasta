package com.hasta.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hasta.app.ui.theme.HastaRadius
import com.hasta.app.ui.theme.HastaSizes
import com.hasta.app.ui.theme.HastaTheme

enum class HastaButtonVariant { Primary, Secondary, Text }

@Composable
fun HastaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HastaButtonVariant = HastaButtonVariant.Primary,
    enabled: Boolean = true,
    icon: HastaIconName? = null,
    contentDescription: String? = null,
) {
    val colors = HastaTheme.colors
    val semantics = if (contentDescription == null) {
        Modifier
    } else {
        Modifier.semantics { this.contentDescription = contentDescription }
    }
    val shared = modifier
        .heightIn(min = if (variant == HastaButtonVariant.Text) HastaSizes.touchTarget else HastaSizes.primaryAction)
        .then(semantics)

    val content: @Composable RowScope.() -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                HastaIcon(
                    name = icon,
                    tint = if (variant == HastaButtonVariant.Primary) colors.onBrandPrimary else colors.onBrandContainer,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }

    when (variant) {
        HastaButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = shared,
            enabled = enabled,
            shape = RoundedCornerShape(HastaRadius.medium),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.brandPrimary,
                contentColor = colors.onBrandPrimary,
                disabledContainerColor = colors.surfaceRaised,
                disabledContentColor = colors.textSecondary,
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
            content = content,
        )

        HastaButtonVariant.Secondary -> Button(
            onClick = onClick,
            modifier = shared,
            enabled = enabled,
            shape = RoundedCornerShape(HastaRadius.medium),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.brandContainer,
                contentColor = colors.onBrandContainer,
                disabledContainerColor = colors.surfaceRaised,
                disabledContentColor = colors.textSecondary,
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
            content = content,
        )

        HastaButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = shared,
            enabled = enabled,
            shape = RoundedCornerShape(HastaRadius.medium),
            colors = ButtonDefaults.textButtonColors(
                contentColor = colors.brandAccent,
                disabledContentColor = colors.textSecondary,
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            content = content,
        )
    }
}
