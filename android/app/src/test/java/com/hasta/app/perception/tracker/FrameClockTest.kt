package com.hasta.app.perception.tracker

import org.junit.Assert.assertTrue
import org.junit.Test

class FrameClockTest {

    @Test
    fun `increasing input is preserved`() {
        val clock = FrameClock()
        assertTrue(clock.next(10L) == 10L)
        assertTrue(clock.next(20L) == 20L)
    }

    @Test
    fun `equal timestamps are clamped`() {
        val clock = FrameClock()
        val first = clock.next(10L)
        val second = clock.next(10L)
        assertTrue(second > first)
    }

    @Test
    fun `backwards timestamps are clamped`() {
        val clock = FrameClock()
        val first = clock.next(500L)
        val second = clock.next(10L)
        assertTrue(second > first)
    }

    @Test
    fun `a long sequence is strictly increasing`() {
        val clock = FrameClock()
        var previous = Long.MIN_VALUE
        repeat(10_000) { index ->
            val value = clock.next((index % 7).toLong())
            assertTrue("value $value after $previous", value > previous)
            previous = value
        }
    }
}
