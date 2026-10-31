package com.hasta.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.hasta.app.ui.permission.findActivity

/**
 * Keeps system-bar icons readable per screen. The camera chrome is always dark
 * (light icons), while permission screens follow the system light/dark theme.
 */
@Composable
fun SystemBarAppearance(lightIcons: Boolean) {
    val view = LocalView.current
    val activity = view.context.findActivity() ?: return
    DisposableEffect(activity, lightIcons) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        val previousStatus = controller.isAppearanceLightStatusBars
        val previousNavigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = lightIcons
        controller.isAppearanceLightNavigationBars = lightIcons
        onDispose {
            controller.isAppearanceLightStatusBars = previousStatus
            controller.isAppearanceLightNavigationBars = previousNavigation
        }
    }
}
