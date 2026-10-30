package com.hasta.app.ui.permission

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionPolicyTest {

    @Test
    fun `granted wins in every case`() {
        assertEquals(
            CameraPermissionStatus.GRANTED,
            PermissionPolicy.resolve(granted = true, hasRequested = false, showRationale = false),
        )
        assertEquals(
            CameraPermissionStatus.GRANTED,
            PermissionPolicy.resolve(granted = true, hasRequested = true, showRationale = true),
        )
    }

    @Test
    fun `never requested is not a denial`() {
        assertEquals(
            CameraPermissionStatus.NOT_REQUESTED,
            PermissionPolicy.resolve(granted = false, hasRequested = false, showRationale = false),
        )
    }

    @Test
    fun `requested with rationale is a normal denial`() {
        assertEquals(
            CameraPermissionStatus.DENIED,
            PermissionPolicy.resolve(granted = false, hasRequested = true, showRationale = true),
        )
    }

    @Test
    fun `requested without rationale is permanent denial`() {
        assertEquals(
            CameraPermissionStatus.PERMANENTLY_DENIED,
            PermissionPolicy.resolve(granted = false, hasRequested = true, showRationale = false),
        )
    }
}
