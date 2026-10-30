package com.hasta.app.data.benchmark

import kotlinx.coroutines.flow.StateFlow

/**
 * Debug-only benchmark surface exposed to the UI.
 *
 * The implementation lives in the `debug` source set and is created reflectively by
 * [BenchmarkFactory], so a release build contains neither the recorder nor any
 * sampling code. UI code only ever sees this interface and [BenchmarkState].
 */
interface BenchmarkRecorder {

    val state: StateFlow<BenchmarkState>

    /** Called on the tracker callback thread for every processed frame. */
    fun onFrame(
        resultElapsedMs: Long,
        latencyMs: Long,
        rotationDegrees: Int,
    )

    /** Starts a run when idle, or ends the current run early when running. */
    fun toggle(
        camera: BenchmarkCameraInfo?,
        counters: () -> BenchmarkCounters,
    )

    /** Ends a run if the camera pipeline stops before the ten minutes complete. */
    fun onPipelineStopped()
}

/** Snapshot of pipeline counters, read at the start and end of a run. */
data class BenchmarkCounters(
    val submitted: Long,
    val processed: Long,
    val skipped: Long,
    val errors: Long,
)

/** Camera sizes recorded with a run so alignment claims can be checked later. */
data class BenchmarkCameraInfo(
    val analysisWidth: Int,
    val analysisHeight: Int,
    val previewWidth: Int,
    val previewHeight: Int,
    val targetRotationDegrees: Int,
    val delegate: String = "unknown",
    val smoothing: String = "one_euro",
    val exposure: String = "adaptive_luma",
) {
    val analysisAspectRatio: Float
        get() = if (analysisHeight == 0) 0f else analysisWidth.toFloat() / analysisHeight

    val previewAspectRatio: Float
        get() = if (previewHeight == 0) 0f else previewWidth.toFloat() / previewHeight
}

sealed interface BenchmarkState {
    data object Idle : BenchmarkState

    data class Running(
        val startedAtElapsedMs: Long,
        val durationMs: Long,
    ) : BenchmarkState

    data class Completed(
        val jsonFile: String,
        val csvFile: String,
        val processedFrames: Long,
        val endReason: String,
    ) : BenchmarkState

    data class Failed(val message: String) : BenchmarkState
}
