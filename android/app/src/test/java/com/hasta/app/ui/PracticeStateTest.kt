package com.hasta.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeStateTest {

    @Test
    fun defaultStateIsIdleWithAFiveSecondWindow() {
        val state = PracticeUiState()

        assertEquals(PracticePhase.Idle, state.phase)
        assertEquals(5_000, state.windowMillis)
        assertEquals(0f, state.progress, 0.001f)
        assertEquals(5, state.remainingSeconds)
    }

    @Test
    fun startingClearsElapsedTime() {
        val state = PracticeUiState(elapsedMillis = 2_000).start()

        assertEquals(PracticePhase.Running, state.phase)
        assertEquals(0, state.elapsedMillis)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun progressIsClampedAndCompletesAtTheWindowEnd() {
        val half = PracticeUiState().start().advance(2_500)
        assertEquals(0.5f, half.progress, 0.001f)
        assertEquals(PracticePhase.Running, half.phase)
        assertEquals(3, half.remainingSeconds)

        val complete = half.advance(2_500)
        assertEquals(PracticePhase.Complete, complete.phase)
        assertEquals(1f, complete.progress, 0.001f)
        assertEquals(0, complete.remainingSeconds)

        val overrun = complete.advance(10_000)
        assertEquals(1f, overrun.progress, 0.001f)
        assertTrue(overrun.phase == PracticePhase.Complete)
    }

    @Test
    fun stoppingReturnsToIdleAndClearsProgress() {
        val stopped = PracticeUiState().start().advance(1_000).stop()

        assertEquals(PracticePhase.Idle, stopped.phase)
        assertEquals(0, stopped.elapsedMillis)
        assertEquals(0f, stopped.progress, 0.001f)
    }

    @Test
    fun practiseAgainStartsAFreshWindow() {
        val again = PracticeUiState().start().advance(5_000).again()

        assertEquals(PracticePhase.Running, again.phase)
        assertEquals(0, again.elapsedMillis)
        assertEquals(0f, again.progress, 0.001f)
    }

    @Test
    fun advancingAnIdleWindowDoesNothing() {
        val idle = PracticeUiState().advance(1_000)

        assertEquals(PracticePhase.Idle, idle.phase)
        assertEquals(0, idle.elapsedMillis)
    }
}
