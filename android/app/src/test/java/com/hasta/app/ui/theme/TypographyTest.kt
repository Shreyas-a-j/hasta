package com.hasta.app.ui.theme

import androidx.compose.ui.text.TextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypographyTest {

    @Test
    fun bodyBaseIsAtLeastSixteenSpWithAnExplicitLineHeight() {
        assertEquals(16f, HastaTypography.bodyLarge.fontSize.value, 0.01f)
        assertEquals(24f, HastaTypography.bodyLarge.lineHeight.value, 0.01f)
        assertEquals(14f, HastaTypography.bodyMedium.fontSize.value, 0.01f)
        assertEquals(20f, HastaTypography.bodyMedium.lineHeight.value, 0.01f)
    }

    @Test
    fun noCriticalStyleIsSmallerThanTwelveSp() {
        val styles: List<TextStyle> = listOf(
            HastaTypography.displayLarge,
            HastaTypography.headlineMedium,
            HastaTypography.titleLarge,
            HastaTypography.titleMedium,
            HastaTypography.bodyLarge,
            HastaTypography.bodyMedium,
            HastaTypography.labelLarge,
            HastaTypography.labelSmall,
        )
        styles.forEach { style ->
            assertTrue("font size below 12sp", style.fontSize.value >= 12f)
            assertTrue("line height below font size", style.lineHeight.value >= style.fontSize.value)
        }
    }

    @Test
    fun displayIsTheLargestStyle() {
        assertTrue(HastaTypography.displayLarge.fontSize.value > HastaTypography.headlineMedium.fontSize.value)
        assertTrue(HastaTypography.headlineMedium.fontSize.value > HastaTypography.titleLarge.fontSize.value)
        assertTrue(HastaTypography.titleLarge.fontSize.value > HastaTypography.bodyLarge.fontSize.value)
    }
}
