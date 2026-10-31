package com.hasta.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hasta.app.ui.theme.HastaTheme

/**
 * Four static corner brackets. Decorative: they hint at the signing area without
 * becoming a required box, and they are never animated.
 */
@Composable
fun FramingGuide(modifier: Modifier = Modifier) {
    val guide = HastaTheme.colors.cameraGuide.copy(alpha = 0.55f)

    Canvas(modifier = modifier.semantics { hideFromAccessibility() }) {
        val inset = size.minDimension * 0.04f
        val length = size.minDimension * 0.12f
        val stroke = 2.dp.toPx()
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset

        fun line(start: Offset, end: Offset) {
            drawLine(
                color = guide,
                start = start,
                end = end,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        line(Offset(left, top + length), Offset(left, top))
        line(Offset(left, top), Offset(left + length, top))

        line(Offset(right - length, top), Offset(right, top))
        line(Offset(right, top), Offset(right, top + length))

        line(Offset(right, bottom - length), Offset(right, bottom))
        line(Offset(right, bottom), Offset(right - length, bottom))

        line(Offset(left + length, bottom), Offset(left, bottom))
        line(Offset(left, bottom), Offset(left, bottom - length))
    }
}
