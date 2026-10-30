package com.hasta.app.data.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameSeriesTest {

    @Test
    fun `first frame has no interval but keeps its latency`() {
        val series = FrameSeries()
        series.onResult(resultElapsedMs = 1_000, latencyMs = 42, rotationDegrees = 90)

        val snapshot = series.snapshot()
        assertEquals(listOf(42L), snapshot.latencies)
        assertTrue(snapshot.intervals.isEmpty())
        assertNull(snapshot.intervalPercentiles)
    }

    @Test
    fun `intervals are differences between consecutive results`() {
        val series = FrameSeries()
        series.onResult(1_000, 40, 90)
        series.onResult(1_050, 30, 90)
        series.onResult(1_200, 60, 0)

        val snapshot = series.snapshot()
        assertEquals(listOf(1_000L, 1_050L, 1_200L), snapshot.resultTimes)
        assertEquals(listOf(50L, 150L), snapshot.intervals)
        assertEquals(listOf(40L, 30L, 60L), snapshot.latencies)
    }

    @Test
    fun `rotations are recorded once in first-seen order`() {
        val series = FrameSeries()
        series.onResult(1_000, 10, 90)
        series.onResult(1_100, 10, 90)
        series.onResult(1_200, 10, 0)
        series.onResult(1_300, 10, 90)

        assertEquals(listOf(90, 0), series.snapshot().rotations)
    }

    @Test
    fun `negative latency and backwards timestamps are clamped`() {
        val series = FrameSeries()
        series.onResult(1_000, -5, 0)
        series.onResult(900, 10, 0)

        val snapshot = series.snapshot()
        assertEquals(listOf(0L, 10L), snapshot.latencies)
        assertEquals(listOf(0L), snapshot.intervals)
    }

    @Test
    fun `snapshot is a copy`() {
        val series = FrameSeries()
        series.onResult(1_000, 10, 0)
        val snapshot = series.snapshot()

        series.onResult(1_100, 20, 0)

        assertEquals(1, snapshot.latencies.size)
        assertEquals(2, series.snapshot().latencies.size)
    }
}
