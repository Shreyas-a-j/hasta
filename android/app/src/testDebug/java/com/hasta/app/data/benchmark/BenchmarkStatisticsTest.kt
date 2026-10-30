package com.hasta.app.data.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BenchmarkStatisticsTest {

    @Test
    fun `no samples means no percentiles`() {
        assertNull(BenchmarkStatistics.percentiles(emptyList()))
    }

    @Test
    fun `nearest rank percentiles of 1 to 100`() {
        val values = (1L..100L).toList()
        val percentiles = requireNotNull(BenchmarkStatistics.percentiles(values))

        assertEquals(50L, percentiles.p50)
        assertEquals(95L, percentiles.p95)
        assertEquals(99L, percentiles.p99)
        assertEquals(1L, percentiles.min)
        assertEquals(100L, percentiles.max)
        assertEquals(50.5, percentiles.mean, 0.001)
    }

    @Test
    fun `small sample rounds up to the next rank`() {
        val values = listOf(10L, 20L, 30L, 40L)
        val percentiles = requireNotNull(BenchmarkStatistics.percentiles(values))

        assertEquals(20L, percentiles.p50)
        assertEquals(40L, percentiles.p95)
        assertEquals(40L, percentiles.p99)
    }

    @Test
    fun `single sample reports itself everywhere`() {
        val percentiles = requireNotNull(BenchmarkStatistics.percentiles(listOf(7L)))

        assertEquals(7L, percentiles.p50)
        assertEquals(7L, percentiles.p95)
        assertEquals(7L, percentiles.p99)
        assertEquals(7L, percentiles.min)
        assertEquals(7L, percentiles.max)
        assertEquals(1, percentiles.count)
    }
}
