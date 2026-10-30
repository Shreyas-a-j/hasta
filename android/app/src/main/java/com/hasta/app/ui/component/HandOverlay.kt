package com.hasta.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hasta.app.domain.model.HandObservation
import com.hasta.app.domain.model.Handedness
import com.hasta.app.domain.tracking.HandTopology
import com.hasta.app.perception.overlay.OverlayTransform

/**
 * Draws the 21-point hand skeleton for every detected hand.
 *
 * The canvas has the same bounds as the preview. Coordinates are mapped through
 * [OverlayTransform], which handles centre-crop and the display-only front-camera
 * mirror. Nothing is drawn for a hand that the tracker did not return.
 */
@Composable
fun HandOverlay(
    hands: List<HandObservation>,
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier,
) {
    val description = when (hands.size) {
        0 -> "Hand skeleton overlay, no hands"
        1 -> "Hand skeleton overlay, one hand"
        else -> "Hand skeleton overlay, two hands"
    }

    Canvas(
        modifier = modifier.semantics { contentDescription = description },
    ) {
        if (hands.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return@Canvas

        val transform = OverlayTransform.createForUprightImage(
            uprightWidth = imageWidth,
            uprightHeight = imageHeight,
            viewWidth = size.width,
            viewHeight = size.height,
            mirrorHorizontally = true,
        ) ?: return@Canvas

        hands.forEach { hand ->
            val points = hand.landmarks.map { landmark ->
                val mapped = transform.mapNormalized(landmark.x, landmark.y)
                Offset(mapped.x, mapped.y)
            }
            val color = handColor(hand.handedness)

            HandTopology.connections.forEach { (start, end) ->
                val first = points.getOrNull(start) ?: return@forEach
                val second = points.getOrNull(end) ?: return@forEach
                drawLine(
                    color = Color.Black.copy(alpha = 0.55f),
                    start = first,
                    end = second,
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = first,
                    end = second,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            points.forEach { point ->
                drawCircle(
                    color = Color.Black.copy(alpha = 0.55f),
                    radius = 5.5.dp.toPx(),
                    center = point,
                )
                drawCircle(
                    color = color,
                    radius = 3.dp.toPx(),
                    center = point,
                )
            }
        }
    }
}

private fun handColor(handedness: Handedness): Color = when (handedness) {
    Handedness.LEFT -> Color(0xFFDFF1FD)
    Handedness.RIGHT -> Color(0xFFC01D5A)
    Handedness.UNKNOWN -> Color(0xFFFFFFFF)
}
