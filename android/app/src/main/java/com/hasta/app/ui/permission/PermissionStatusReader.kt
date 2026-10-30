package com.hasta.app.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Reads the live camera permission state from the platform.
 *
 * The state is re-read on every resume so a grant made through Android Settings is
 * picked up without restarting the app. [hasRequested] is owned by the caller
 * because the platform cannot tell "never asked" from "asked and denied forever".
 */
object PermissionStatusReader {

    fun read(context: Context, hasRequested: Boolean): CameraPermissionStatus {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        val activity = context.findActivity()
        val showRationale = activity?.shouldShowRequestPermissionRationale(
            Manifest.permission.CAMERA,
        ) ?: false
        return PermissionPolicy.resolve(
            granted = granted,
            hasRequested = hasRequested,
            showRationale = showRationale,
        )
    }
}

/** Unwraps the Activity from a themed context wrapper, if there is one. */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
