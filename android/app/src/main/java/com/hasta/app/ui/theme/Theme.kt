package com.hasta.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val HastaLightScheme = lightColorScheme(
    primary = HastaLightColors.brandPrimary,
    onPrimary = HastaLightColors.onBrandPrimary,
    primaryContainer = HastaLightColors.brandContainer,
    onPrimaryContainer = HastaLightColors.onBrandContainer,
    secondary = HastaLightColors.brandAccent,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = HastaLightColors.brandContainer,
    onSecondaryContainer = HastaLightColors.onBrandContainer,
    tertiary = HastaLightColors.success,
    onTertiary = HastaLightColors.onSuccess,
    background = HastaLightColors.background,
    onBackground = HastaLightColors.textPrimary,
    surface = HastaLightColors.surface,
    onSurface = HastaLightColors.textPrimary,
    surfaceVariant = HastaLightColors.surfaceSubtle,
    onSurfaceVariant = HastaLightColors.textSecondary,
    outline = HastaLightColors.outline,
    outlineVariant = HastaLightColors.divider,
    error = HastaLightColors.error,
    onError = HastaLightColors.onError,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = HastaLightColors.textPrimary,
    inverseOnSurface = HastaLightColors.surface,
    inversePrimary = Color(0xFFF49CBB),
    scrim = Color(0xFF000000),
)

private val HastaDarkScheme = darkColorScheme(
    primary = HastaDarkColors.brandPrimary,
    onPrimary = HastaDarkColors.onBrandPrimary,
    primaryContainer = HastaDarkColors.brandContainer,
    onPrimaryContainer = HastaDarkColors.onBrandContainer,
    secondary = HastaDarkColors.brandAccent,
    onSecondary = Color(0xFF3A0A1D),
    secondaryContainer = HastaDarkColors.brandContainer,
    onSecondaryContainer = HastaDarkColors.onBrandContainer,
    tertiary = HastaDarkColors.success,
    onTertiary = HastaDarkColors.onSuccess,
    background = HastaDarkColors.background,
    onBackground = HastaDarkColors.textPrimary,
    surface = HastaDarkColors.surface,
    onSurface = HastaDarkColors.textPrimary,
    surfaceVariant = HastaDarkColors.surfaceRaised,
    onSurfaceVariant = HastaDarkColors.textSecondary,
    outline = HastaDarkColors.outline,
    outlineVariant = HastaDarkColors.divider,
    error = HastaDarkColors.error,
    onError = HastaDarkColors.onError,
    errorContainer = Color(0xFF5F1220),
    onErrorContainer = Color(0xFFFFD9E0),
    inverseSurface = HastaDarkColors.textPrimary,
    inverseOnSurface = HastaDarkColors.surface,
    inversePrimary = HastaDarkColors.brandPrimary,
    scrim = Color(0xFF000000),
)

private val LocalHastaColors = staticCompositionLocalOf { HastaLightColors }
private val LocalHastaMotion = staticCompositionLocalOf { HastaMotionSpec }

object HastaTheme {
    val colors: HastaColors
        @Composable @ReadOnlyComposable get() = LocalHastaColors.current

    val motion: HastaMotion
        @Composable @ReadOnlyComposable get() = LocalHastaMotion.current
}

@Composable
fun HastaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reducedMotion: Boolean = rememberReducedMotion(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) HastaDarkColors else HastaLightColors
    val motion = if (reducedMotion) HastaReducedMotion else HastaMotionSpec
    CompositionLocalProvider(
        LocalHastaColors provides palette,
        LocalHastaMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) HastaDarkScheme else HastaLightScheme,
            typography = HastaTypography,
            shapes = HastaShapes,
            content = content,
        )
    }
}

/**
 * Camera chrome is deliberately dark even when the system is in light mode. It
 * does not invert the light palette; it reuses the dark tonal palette because
 * the preview and hand skeleton need a dark surround.
 */
@Composable
fun HastaCameraTheme(
    reducedMotion: Boolean = rememberReducedMotion(),
    content: @Composable () -> Unit,
) {
    val motion = if (reducedMotion) HastaReducedMotion else HastaMotionSpec
    CompositionLocalProvider(
        LocalHastaColors provides HastaDarkColors,
        LocalHastaMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = HastaDarkScheme,
            typography = HastaTypography,
            shapes = HastaShapes,
            content = content,
        )
    }
}
