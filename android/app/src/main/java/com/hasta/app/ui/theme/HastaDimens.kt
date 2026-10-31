package com.hasta.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 4dp base grid. */
object HastaSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

object HastaRadius {
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
}

object HastaSizes {
    val touchTarget = 48.dp
    val primaryAction = 56.dp
    val topBar = 56.dp
    val icon = 24.dp
    val statusDot = 8.dp
    val progressTrack = 4.dp
}

val HastaShapes = Shapes(
    small = RoundedCornerShape(HastaRadius.small),
    medium = RoundedCornerShape(HastaRadius.medium),
    large = RoundedCornerShape(HastaRadius.large),
    extraLarge = RoundedCornerShape(HastaRadius.extraLarge),
)
