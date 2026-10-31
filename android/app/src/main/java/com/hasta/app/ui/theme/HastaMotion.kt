package com.hasta.app.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Short, purposeful motion. No looping or decorative animation is allowed while
 * the camera is running; progress and status are the only animated values.
 */
@Immutable
data class HastaMotion(
    val fastMs: Int = 120,
    val standardMs: Int = 180,
    val emphasizedMs: Int = 240,
    val reduced: Boolean = false,
) {
    fun durationFor(baseMs: Int): Int = if (reduced) 0 else baseMs
}

val HastaMotionSpec = HastaMotion()

val HastaReducedMotion = HastaMotion(
    fastMs = 0,
    standardMs = 0,
    emphasizedMs = 0,
    reduced = true,
)

internal fun isReducedMotion(animatorScale: Float): Boolean = animatorScale == 0f

@Composable
internal fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
        isReducedMotion(scale)
    }
}
