package com.hasta.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTokensTest {

    @Test
    fun lockedBrandValuesAreExact() {
        assertEquals(Color(0xFFC01D5A), HastaLightColors.brandPrimary)
        assertEquals(Color(0xFFDFF1FD), HastaLightColors.brandContainer)
        assertEquals(Color(0xFF24212B), HastaLightColors.textPrimary)
        assertEquals(Color(0xFF696574), HastaLightColors.textSecondary)
        assertEquals(Color(0xFFFFFFFF), HastaLightColors.surface)
        assertEquals(Color(0xFF23845A), HastaLightColors.success)
        assertEquals(Color(0xFFC58B24), HastaLightColors.warning)
    }

    @Test
    fun darkThemeIsASeparateTonalPalette() {
        assertNotEquals(HastaLightColors.background, HastaDarkColors.background)
        assertNotEquals(HastaLightColors.surface, HastaDarkColors.surface)
        assertNotEquals(HastaLightColors.textPrimary, HastaDarkColors.textPrimary)
        assertNotEquals(HastaLightColors.surfaceSubtle, HastaDarkColors.surfaceSubtle)
        // Raspberry stays controllable in dark mode instead of becoming a bright large surface.
        assertEquals(HastaLightColors.brandPrimary, HastaDarkColors.brandPrimary)
    }

    @Test
    fun documentedBlueprintPairsKeepTheirContrast() {
        assertEquals(5.89, contrastRatio(Color.White, HastaLightColors.brandPrimary), 0.02)
        assertEquals(5.09, contrastRatio(HastaLightColors.brandPrimary, HastaLightColors.brandContainer), 0.02)
        assertEquals(13.66, contrastRatio(HastaLightColors.textPrimary, HastaLightColors.brandContainer), 0.02)
        assertEquals(4.88, contrastRatio(HastaLightColors.textSecondary, HastaLightColors.brandContainer), 0.02)
    }

    @Test
    fun lightTextPairsMeetAaForNormalText() {
        assertAa(HastaLightColors.textPrimary, HastaLightColors.surface)
        assertAa(HastaLightColors.textSecondary, HastaLightColors.surface)
        assertAa(HastaLightColors.brandAccent, HastaLightColors.surface)
        assertAa(HastaLightColors.textPrimary, HastaLightColors.surfaceSubtle)
        assertAa(HastaLightColors.textSecondary, HastaLightColors.surfaceSubtle)
        assertAa(HastaLightColors.successText, HastaLightColors.surfaceSubtle)
        assertAa(HastaLightColors.warningText, HastaLightColors.surface)
        assertAa(HastaLightColors.error, HastaLightColors.surface)
        assertAa(HastaLightColors.onBrandPrimary, HastaLightColors.brandPrimary)
        assertAa(HastaLightColors.onWarning, HastaLightColors.warning)
        assertAa(HastaLightColors.onSuccess, HastaLightColors.success)
    }

    @Test
    fun darkTextPairsMeetAaForNormalText() {
        assertAa(HastaDarkColors.textPrimary, HastaDarkColors.background)
        assertAa(HastaDarkColors.textPrimary, HastaDarkColors.surface)
        assertAa(HastaDarkColors.textPrimary, HastaDarkColors.surfaceRaised)
        assertAa(HastaDarkColors.textSecondary, HastaDarkColors.surface)
        assertAa(HastaDarkColors.brandAccent, HastaDarkColors.surface)
        assertAa(HastaDarkColors.onBrandContainer, HastaDarkColors.brandContainer)
        assertAa(HastaDarkColors.success, HastaDarkColors.surface)
        assertAa(HastaDarkColors.warning, HastaDarkColors.surface)
        assertAa(HastaDarkColors.error, HastaDarkColors.surface)
        assertAa(HastaDarkColors.onBrandPrimary, HastaDarkColors.brandPrimary)
    }

    @Test
    fun cameraChromeTextIsAlwaysReadable() {
        assertAa(HastaDarkColors.cameraText, HastaDarkColors.cameraPanel)
        assertAa(HastaDarkColors.cameraTextSecondary, HastaDarkColors.cameraPanel)
        assertTrue(contrastRatio(HastaLightColors.cameraGuide, HastaLightColors.cameraPanel) >= 7.0)
    }

    @Test
    fun whiteIsNeverThePairedForegroundOnPaleSky() {
        assertTrue(contrastRatio(Color.White, HastaLightColors.brandContainer) < 3.0)
        assertNotEquals(Color.White, HastaLightColors.onBrandContainer)
        assertNotEquals(Color.White, HastaDarkColors.onBrandContainer)
    }

    @Test
    fun statusToneUsesASemanticColour() {
        assertEquals(HastaLightColors.success, HastaLightColors.toneColor(HastaTone.Success))
        assertEquals(HastaDarkColors.warning, HastaDarkColors.toneColor(HastaTone.Warning))
        assertEquals(HastaLightColors.error, HastaLightColors.toneColor(HastaTone.Error))
        assertEquals(HastaLightColors.textSecondary, HastaLightColors.toneColor(HastaTone.Neutral))
        assertEquals(HastaLightColors.brandAccent, HastaLightColors.toneColor(HastaTone.Brand))
    }

    private fun assertAa(foreground: Color, background: Color) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("contrast $ratio was below 4.5 for $foreground on $background", ratio >= 4.5)
    }
}
