package com.hasta.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hasta.app.ui.theme.HastaTheme

enum class HastaCardVariant { Standard, Subtle, Accent }

@Composable
fun HastaCard(
    modifier: Modifier = Modifier,
    variant: HastaCardVariant = HastaCardVariant.Standard,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = HastaTheme.colors
    val container = when (variant) {
        HastaCardVariant.Standard -> colors.surface
        HastaCardVariant.Subtle -> colors.surfaceSubtle
        HastaCardVariant.Accent -> colors.brandContainer
    }
    val contentColor = when (variant) {
        HastaCardVariant.Accent -> colors.onBrandContainer
        else -> colors.textPrimary
    }
    val border = when (variant) {
        HastaCardVariant.Standard -> BorderStroke(1.dp, colors.divider)
        else -> null
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}
