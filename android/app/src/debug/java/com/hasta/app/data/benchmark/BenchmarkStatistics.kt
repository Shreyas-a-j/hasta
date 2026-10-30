package com.hasta.app.data.benchmark

import kotlin.math.ceil

data class Percentiles(
    val p50: Long,
    val p95: Long,
    val p99: Long,
    val min: Long,
    val max: Long,
    val mean: Double,
    val count: Int,
)

/**
 * Nearest-rank percentiles. Deliberately simple and testable: for n values the
 * p-th percentile is the value at rank ceil(p / 100 * n), 1-based.
 */
object BenchmarkStatistics {

    fun percentiles(values: List<Long>): Percentiles? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        return Percentiles(
            p50 = percentile(sorted, 50.0),
            p95 = percentile(sorted, 95.0),
            p99 = percentile(sorted, 99.0),
            min = sorted.first(),
            max = sorted.last(),
            mean = values.average(),
            count = values.size,
        )
    }

    fun percentile(sortedValues: List<Long>, percent: Double): Long {
        require(sortedValues.isNotEmpty()) { "percentile of an empty list" }
        require(percent in 0.0..100.0) { "percent must be in 0..100: $percent" }
        val rank = ceil(percent / 100.0 * sortedValues.size)
            .toInt()
            .coerceIn(1, sortedValues.size)
        return sortedValues[rank - 1]
    }
}

/**
 * Accumulates per-frame latency and interval samples. Not thread-safe; the benchmark
 * recorder guards it with its own lock.
 */
class FrameSeries {

    private val resultTimes = ArrayList<Long>()
    private val latencies = ArrayList<Long>()
    private val intervals = ArrayList<Long>()
    private val rotations = LinkedHashSet<Int>()
    private var lastResultElapsedMs: Long? = null

    fun onResult(
        resultElapsedMs: Long,
        latencyMs: Long,
        rotationDegrees: Int,
    ) {
        lastResultElapsedMs?.let { previous ->
            intervals.add((resultElapsedMs - previous).coerceAtLeast(0L))
        }
        lastResultElapsedMs = resultElapsedMs
        resultTimes.add(resultElapsedMs)
        latencies.add(latencyMs.coerceAtLeast(0L))
        rotations.add(rotationDegrees)
    }

    fun snapshot(): FrameSeriesSnapshot = FrameSeriesSnapshot(
        resultTimes = resultTimes.toList(),
        latencies = latencies.toList(),
        intervals = intervals.toList(),
        rotations = rotations.toList(),
    )
}

data class FrameSeriesSnapshot(
    val resultTimes: List<Long>,
    val latencies: List<Long>,
    val intervals: List<Long>,
    val rotations: List<Int>,
) {
    val latencyPercentiles: Percentiles? get() = BenchmarkStatistics.percentiles(latencies)
    val intervalPercentiles: Percentiles? get() = BenchmarkStatistics.percentiles(intervals)
}
