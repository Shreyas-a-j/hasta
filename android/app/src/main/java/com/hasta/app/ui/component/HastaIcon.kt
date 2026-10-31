package com.hasta.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hasta.app.ui.theme.HastaSizes

enum class HastaIconName {
    Check,
    Alert,
    Hand,
    Camera,
    Settings,
    Retry,
    Play,
    Stop,
    Info,
}

/**
 * Small vector glyphs drawn with Compose Canvas so M0.5 does not need an icon
 * dependency. Decorative icons are removed from the accessibility tree.
 */
@Composable
fun HastaIcon(
    name: HastaIconName,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Canvas(
        modifier = modifier
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                } else {
                    hideFromAccessibility()
                }
            },
    ) {
        val scale = size.minDimension / 24f
        fun point(x: Float, y: Float): Offset = Offset(x * scale, y * scale)
        val stroke = Stroke(
            width = 2f * scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        when (name) {
            HastaIconName.Check -> {
                val path = Path().apply {
                    moveTo(5f * scale, 12f * scale)
                    lineTo(10f * scale, 17f * scale)
                    lineTo(19f * scale, 7f * scale)
                }
                drawPath(path, color = tint, style = stroke)
            }

            HastaIconName.Alert -> {
                drawCircle(
                    color = tint,
                    radius = 9f * scale,
                    center = point(12f, 12f),
                    style = stroke,
                )
                drawLine(tint, point(12f, 7.5f), point(12f, 13.5f), strokeWidth = 2f * scale, cap = StrokeCap.Round)
                drawCircle(tint, radius = 1.2f * scale, center = point(12f, 17f))
            }

            HastaIconName.Hand -> {
                drawRoundRect(
                    color = tint,
                    topLeft = point(7f, 10f),
                    size = Size(10f * scale, 11f * scale),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale, 3f * scale),
                    style = stroke,
                )
                drawLine(tint, point(9f, 10f), point(9f, 5f), strokeWidth = 1.8f * scale, cap = StrokeCap.Round)
                drawLine(tint, point(12f, 10f), point(12f, 4f), strokeWidth = 1.8f * scale, cap = StrokeCap.Round)
                drawLine(tint, point(15f, 10f), point(15f, 6f), strokeWidth = 1.8f * scale, cap = StrokeCap.Round)
                drawLine(tint, point(7f, 13f), point(4f, 10f), strokeWidth = 1.8f * scale, cap = StrokeCap.Round)
            }

            HastaIconName.Camera -> {
                drawRoundRect(
                    color = tint,
                    topLeft = point(3f, 7f),
                    size = Size(18f * scale, 12f * scale),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * scale, 2f * scale),
                    style = stroke,
                )
                drawCircle(
                    color = tint,
                    radius = 4f * scale,
                    center = point(12f, 13f),
                    style = stroke,
                )
                drawLine(tint, point(9f, 7f), point(10f, 5f), strokeWidth = 2f * scale, cap = StrokeCap.Round)
            }

            HastaIconName.Settings -> {
                drawLine(tint, point(4f, 7f), point(20f, 7f), strokeWidth = 2f * scale, cap = StrokeCap.Round)
                drawLine(tint, point(4f, 12f), point(20f, 12f), strokeWidth = 2f * scale, cap = StrokeCap.Round)
                drawLine(tint, point(4f, 17f), point(20f, 17f), strokeWidth = 2f * scale, cap = StrokeCap.Round)
                drawCircle(tint, radius = 2f * scale, center = point(9f, 7f))
                drawCircle(tint, radius = 2f * scale, center = point(15f, 12f))
                drawCircle(tint, radius = 2f * scale, center = point(8f, 17f))
            }

            HastaIconName.Retry -> {
                drawArc(
                    color = tint,
                    startAngle = 35f,
                    sweepAngle = 285f,
                    useCenter = false,
                    topLeft = point(5f, 5f),
                    size = Size(14f * scale, 14f * scale),
                    style = Stroke(width = 2f * scale, cap = StrokeCap.Round),
                )
                val arrow = Path().apply {
                    moveTo(17f * scale, 3.5f * scale)
                    lineTo(20.5f * scale, 7f * scale)
                    lineTo(17f * scale, 10.5f * scale)
                }
                drawPath(arrow, color = tint, style = stroke)
            }

            HastaIconName.Play -> {
                val path = Path().apply {
                    moveTo(8f * scale, 5f * scale)
                    lineTo(19f * scale, 12f * scale)
                    lineTo(8f * scale, 19f * scale)
                    close()
                }
                drawPath(path, color = tint)
            }

            HastaIconName.Stop -> {
                drawRoundRect(
                    color = tint,
                    topLeft = point(7f, 7f),
                    size = Size(10f * scale, 10f * scale),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * scale, 2f * scale),
                )
            }

            HastaIconName.Info -> {
                drawCircle(
                    color = tint,
                    radius = 9f * scale,
                    center = point(12f, 12f),
                    style = stroke,
                )
                drawCircle(tint, radius = 1.1f * scale, center = point(12f, 7.8f))
                drawLine(tint, point(12f, 11f), point(12f, 16.5f), strokeWidth = 2f * scale, cap = StrokeCap.Round)
            }
        }
    }
}
