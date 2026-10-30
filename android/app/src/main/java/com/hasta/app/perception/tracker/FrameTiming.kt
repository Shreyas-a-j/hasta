package com.hasta.app.perception.tracker

/**
 * Timing for one processed frame, in `SystemClock.elapsedRealtime()` milliseconds.
 *
 * [captureElapsedMs] comes from the camera frame metadata; [resultElapsedMs] is read
 * when the landmark result arrives. Their difference is the acquisition-to-landmarks
 * latency reported by the benchmark. [rotationDegrees] is the rotation that was
 * applied to this frame, so a run can prove which orientation it measured.
 */
data class FrameTiming(
    val captureElapsedMs: Long,
    val resultElapsedMs: Long,
    val rotationDegrees: Int,
) {
    val latencyMs: Long
        get() = (resultElapsedMs - captureElapsedMs).coerceAtLeast(0L)
}
