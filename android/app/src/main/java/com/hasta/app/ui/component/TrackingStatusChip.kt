package com.hasta.app.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hasta.app.R
import com.hasta.app.domain.tracking.TrackingState
import com.hasta.app.ui.CameraUiState

/**
 * Small, low-distraction status indicator. No FPS, coordinates or internal scores.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackingStatusChip(
    state: CameraUiState,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val label = when (state.tracking) {
        TrackingState.Initializing -> "Starting camera"
        TrackingState.PermissionRequired -> "Camera permission required"
        TrackingState.CameraReady -> "Camera ready"
        TrackingState.NoHands -> "No hands visible"
        TrackingState.OneHand -> "1 hand"
        TrackingState.TwoHands -> "2 hands"
        TrackingState.TrackingError -> "Tracking error"
        TrackingState.CameraUnavailable -> "Camera unavailable"
    }
    val dotColor = when (state.tracking) {
        TrackingState.TwoHands -> Color(0xFF23845A)
        TrackingState.OneHand -> Color(0xFFC58B24)
        TrackingState.NoHands,
        TrackingState.CameraReady,
        -> Color(0xFFB9B4C2)
        TrackingState.TrackingError,
        TrackingState.CameraUnavailable,
        -> Color(0xFFCF6679)
        TrackingState.Initializing,
        TrackingState.PermissionRequired,
        -> Color(0xFFB9B4C2)
    }

    val gestureModifier = if (onLongClick == null) {
        Modifier
    } else {
        Modifier.combinedClickable(
            onClick = {},
            onLongClickLabel = stringResource(R.string.benchmark_long_press_label),
            onLongClick = onLongClick,
        )
    }

    Column(
        modifier = modifier.then(gestureModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.62f),
            shape = RoundedCornerShape(50),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFF4F1F6),
                )
            }
        }

        if (state.message != null) {
            Text(
                text = state.message,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFD7E1),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}
