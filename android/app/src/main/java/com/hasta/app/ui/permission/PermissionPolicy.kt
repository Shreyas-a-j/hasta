package com.hasta.app.ui.permission

/**
 * Camera permission state derived from the system, not from a one-off callback.
 * Re-evaluated on every resume so a grant made through Android Settings is picked up.
 */
enum class CameraPermissionStatus {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
}

object PermissionPolicy {

    fun resolve(
        granted: Boolean,
        hasRequested: Boolean,
        showRationale: Boolean,
    ): CameraPermissionStatus = when {
        granted -> CameraPermissionStatus.GRANTED
        !hasRequested -> CameraPermissionStatus.NOT_REQUESTED
        showRationale -> CameraPermissionStatus.DENIED
        else -> CameraPermissionStatus.PERMANENTLY_DENIED
    }
}
