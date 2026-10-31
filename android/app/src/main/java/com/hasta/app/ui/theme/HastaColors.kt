package com.hasta.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic HASTA colours. Composables read these through [HastaTheme.colors] and
 * never hardcode a brand hex value.
 *
 * The camera chrome is intentionally dark in both themes so the preview and hand
 * skeleton keep their contrast; the light palette is used by permission and other
 * non-camera surfaces.
 */
@Immutable
data class HastaColors(
    val isDark: Boolean,
    val brandPrimary: Color,
    val onBrandPrimary: Color,
    val brandContainer: Color,
    val onBrandContainer: Color,
    /** Brand-tinted text/icon colour that stays readable on the current surface. */
    val brandAccent: Color,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
    val divider: Color,
    val success: Color,
    val onSuccess: Color,
    val successText: Color,
    val warning: Color,
    val onWarning: Color,
    val warningText: Color,
    val error: Color,
    val onError: Color,
    val cameraScrim: Color,
    val cameraPanel: Color,
    val cameraText: Color,
    val cameraTextSecondary: Color,
    val cameraGuide: Color,
    val skeletonLeft: Color,
    val skeletonRight: Color,
    val skeletonUnknown: Color,
) {
    fun toneColor(tone: HastaTone): Color = when (tone) {
        HastaTone.Neutral -> textSecondary
        HastaTone.Brand -> brandAccent
        HastaTone.Success -> success
        HastaTone.Warning -> warning
        HastaTone.Error -> error
    }
}

enum class HastaTone { Neutral, Brand, Success, Warning, Error }

val HastaLightColors = HastaColors(
    isDark = false,
    brandPrimary = Color(0xFFC01D5A),
    onBrandPrimary = Color(0xFFFFFFFF),
    brandContainer = Color(0xFFDFF1FD),
    onBrandContainer = Color(0xFF24212B),
    brandAccent = Color(0xFFC01D5A),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFF7F5F9),
    textPrimary = Color(0xFF24212B),
    textSecondary = Color(0xFF696574),
    outline = Color(0xFF696574),
    divider = Color(0xFFD9D4E0),
    success = Color(0xFF23845A),
    onSuccess = Color(0xFFFFFFFF),
    successText = Color(0xFF1B6B48),
    warning = Color(0xFFC58B24),
    onWarning = Color(0xFF24212B),
    warningText = Color(0xFF8A5A00),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    cameraScrim = Color(0xE6141218),
    cameraPanel = Color(0xFF18151D),
    cameraText = Color(0xFFF4F1F6),
    cameraTextSecondary = Color(0xFFB9B4C2),
    cameraGuide = Color(0xFFDFF1FD),
    skeletonLeft = Color(0xFFDFF1FD),
    skeletonRight = Color(0xFFC01D5A),
    skeletonUnknown = Color(0xFFFFFFFF),
)

/**
 * Dark mode is a separate tonal palette, not an inversion. Raspberry remains a
 * controlled accent: it fills primary actions but never becomes body text on a
 * dark surface (it would be only 2.69:1 on [surface]).
 */
val HastaDarkColors = HastaColors(
    isDark = true,
    brandPrimary = Color(0xFFC01D5A),
    onBrandPrimary = Color(0xFFFFFFFF),
    brandContainer = Color(0xFF24323C),
    onBrandContainer = Color(0xFFDFF1FD),
    brandAccent = Color(0xFFF49CBB),
    background = Color(0xFF141218),
    surface = Color(0xFF24212B),
    surfaceRaised = Color(0xFF2E2A36),
    surfaceSubtle = Color(0xFF1B1820),
    textPrimary = Color(0xFFF4F1F6),
    textSecondary = Color(0xFFB9B4C2),
    outline = Color(0xFF8E8798),
    divider = Color(0xFF3A3542),
    success = Color(0xFF5BC894),
    onSuccess = Color(0xFF141218),
    successText = Color(0xFF5BC894),
    warning = Color(0xFFE6B45C),
    onWarning = Color(0xFF2A1B00),
    warningText = Color(0xFFE6B45C),
    error = Color(0xFFFF9AAF),
    onError = Color(0xFF3B0710),
    cameraScrim = Color(0xE6141218),
    cameraPanel = Color(0xFF18151D),
    cameraText = Color(0xFFF4F1F6),
    cameraTextSecondary = Color(0xFFB9B4C2),
    cameraGuide = Color(0xFFDFF1FD),
    skeletonLeft = Color(0xFFDFF1FD),
    skeletonRight = Color(0xFFC01D5A),
    skeletonUnknown = Color(0xFFFFFFFF),
)
