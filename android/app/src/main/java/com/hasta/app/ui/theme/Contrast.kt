package com.hasta.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * WCAG 2.x relative-luminance contrast. Kept in main code so the documented
 * pairings can be verified by unit tests rather than trusted by eye.
 */
internal fun contrastRatio(foreground: Color, background: Color): Double {
    val first = relativeLuminance(foreground)
    val second = relativeLuminance(background)
    val lighter = maxOf(first, second)
    val darker = minOf(first, second)
    return (lighter + 0.05) / (darker + 0.05)
}

internal fun relativeLuminance(color: Color): Double {
    val red = linearChannel(color.red.toDouble())
    val green = linearChannel(color.green.toDouble())
    val blue = linearChannel(color.blue.toDouble())
    return 0.2126 * red + 0.7152 * green + 0.0722 * blue
}

private fun linearChannel(channel: Double): Double =
    if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

private fun Double.pow(exponent: Double): Double = Math.pow(this, exponent)
