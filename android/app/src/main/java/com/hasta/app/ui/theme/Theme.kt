package com.hasta.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Neutral dark surface so the camera feed and skeleton stay readable. The Raspberry
// accent is used sparingly; full brand theming is M0.5 work.
private val HastaDarkColors = darkColorScheme(
    primary = Color(0xFFC01D5A),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFF101014),
    onBackground = Color(0xFFF4F1F6),
    surface = Color(0xFF101014),
    onSurface = Color(0xFFF4F1F6),
    surfaceVariant = Color(0xFF24212B),
    onSurfaceVariant = Color(0xFFB9B4C2),
    outline = Color(0xFF696574),
    error = Color(0xFFCF6679),
)

@Composable
fun HastaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HastaDarkColors,
        content = content,
    )
}
