package com.hasta.app.perception

import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe counters for the camera/analysis pipeline. The benchmark uses these to
 * distinguish camera delivery from successful MediaPipe processing.
 */
class PipelineStats {
    private val submitted = AtomicLong()
    private val processed = AtomicLong()
    private val skipped = AtomicLong()
    private val errors = AtomicLong()

    fun onSubmitted() {
        submitted.incrementAndGet()
    }

    fun onProcessed() {
        processed.incrementAndGet()
    }

    fun onSkipped() {
        skipped.incrementAndGet()
    }

    fun onError() {
        errors.incrementAndGet()
    }

    fun snapshot(): PipelineSnapshot = PipelineSnapshot(
        submitted = submitted.get(),
        processed = processed.get(),
        skipped = skipped.get(),
        errors = errors.get(),
    )

    fun reset() {
        submitted.set(0)
        processed.set(0)
        skipped.set(0)
        errors.set(0)
    }
}

data class PipelineSnapshot(
    val submitted: Long,
    val processed: Long,
    val skipped: Long,
    val errors: Long,
)
