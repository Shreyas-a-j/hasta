package com.hasta.app.domain.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TrackingStateTest {

    @Test
    fun `zero hands maps to no hands`() {
        assertEquals(TrackingState.NoHands, TrackingState.fromHandCount(0))
    }

    @Test
    fun `one hand maps to one hand`() {
        assertEquals(TrackingState.OneHand, TrackingState.fromHandCount(1))
    }

    @Test
    fun `two hands maps to two hands`() {
        assertEquals(TrackingState.TwoHands, TrackingState.fromHandCount(2))
    }

    @Test
    fun `out of range counts are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { TrackingState.fromHandCount(-1) }
        assertThrows(IllegalArgumentException::class.java) { TrackingState.fromHandCount(3) }
    }

    @Test
    fun `non-hand states are distinct`() {
        val states = setOf(
            TrackingState.Initializing,
            TrackingState.PermissionRequired,
            TrackingState.CameraReady,
            TrackingState.TrackingError,
            TrackingState.CameraUnavailable,
        )
        assertEquals(5, states.size)
    }
}
