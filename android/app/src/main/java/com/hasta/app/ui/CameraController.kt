package com.hasta.app.ui

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.hasta.app.data.benchmark.BenchmarkCameraInfo
import com.hasta.app.data.benchmark.BenchmarkCounters
import com.hasta.app.data.benchmark.BenchmarkRecorder
import com.hasta.app.data.landmarkdump.LandmarkDump
import com.hasta.app.data.landmarkdump.LandmarkDumpFactory
import com.hasta.app.domain.model.HandFrame
import com.hasta.app.domain.tracking.TrackingState
import com.hasta.app.perception.PipelineStats
import com.hasta.app.perception.camera.CameraInfo
import com.hasta.app.perception.camera.CameraSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns the camera session for the screen and exposes two separate streams:
 *
 * - [frameState] changes on every analysed frame and is only read by the overlay.
 * - [tracking] changes only when the learner-visible tracking state changes.
 *
 * The M0 camera engine, MediaPipe configuration and benchmark path are untouched.
 * [benchmark] and [landmarkDump] are null in release builds, so the debug paths
 * are unreachable there.
 */
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    val benchmark: BenchmarkRecorder? = null,
) {
    private val landmarkDump: LandmarkDump? = LandmarkDumpFactory.create(context)
    private val _frameState = MutableStateFlow(FrameUiState())
    val frameState: StateFlow<FrameUiState> = _frameState.asStateFlow()

    private val _tracking = MutableStateFlow<TrackingState>(TrackingState.Initializing)
    val tracking: StateFlow<TrackingState> = _tracking.asStateFlow()

    val stats = PipelineStats()

    private var session: CameraSession? = null

    /**
     * Invalidates callbacks from a session that has been torn down. The analysis
     * thread can deliver one last result after [restart], and it must not overwrite
     * the state of the session that replaced it.
     */
    @Volatile
    private var sessionGeneration = 0

    fun start() {
        if (session != null) return
        resetState()
        val generation = ++sessionGeneration

        session = CameraSession(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            stats = stats,
            onFrame = { frame, timing ->
                if (generation == sessionGeneration) {
                    benchmark?.onFrame(
                        resultElapsedMs = timing.resultElapsedMs,
                        latencyMs = timing.latencyMs,
                        rotationDegrees = timing.rotationDegrees,
                    )
                    landmarkDump?.onFrame(frame)
                    publishFrame(frame)
                }
            },
            onTrackingError = { message ->
                if (generation == sessionGeneration) publishTrackingError(message)
            },
            onCameraError = { message ->
                if (generation == sessionGeneration) publishCameraError(message)
            },
            onCameraReady = {
                if (generation == sessionGeneration && _tracking.value == TrackingState.Initializing) {
                    _tracking.value = TrackingState.CameraReady
                }
            },
        ).also { it.start() }
    }

    fun stop() {
        benchmark?.onPipelineStopped()
        landmarkDump?.endSession()
        sessionGeneration++
        session?.stop()
        session = null
        resetState()
    }

    /** Restarts the existing camera session after a recoverable error. */
    fun restart() {
        stop()
        start()
    }

    fun currentCameraInfo(): CameraInfo? = session?.currentCameraInfo()

    /** Starts a benchmark run, or stops the current one early. */
    fun toggleBenchmark() {
        val recorder = benchmark ?: return
        val info = currentCameraInfo()
        recorder.toggle(
            camera = info?.let {
                BenchmarkCameraInfo(
                    analysisWidth = it.analysisWidth,
                    analysisHeight = it.analysisHeight,
                    previewWidth = it.previewWidth,
                    previewHeight = it.previewHeight,
                    targetRotationDegrees = it.targetRotationDegrees,
                    delegate = it.delegate,
                )
            },
            counters = {
                val snapshot = stats.snapshot()
                BenchmarkCounters(
                    submitted = snapshot.submitted,
                    processed = snapshot.processed,
                    skipped = snapshot.skipped,
                    errors = snapshot.errors,
                )
            },
        )
    }

    private fun resetState() {
        _frameState.value = FrameUiState()
        _tracking.value = TrackingState.Initializing
    }

    private fun publishFrame(frame: HandFrame) {
        _frameState.value = FrameUiState(
            hands = frame.hands,
            imageWidth = frame.imageWidth,
            imageHeight = frame.imageHeight,
        )
        _tracking.value = TrackingState.fromHandCount(frame.hands.size)
    }

    private fun publishTrackingError(message: String) {
        _frameState.update { it.copy(hands = emptyList()) }
        _tracking.value = TrackingState.TrackingError
        // The message is retained by the controller for diagnostics only. It is
        // never rendered on the learner screen.
        lastErrorMessage = message
    }

    private fun publishCameraError(message: String) {
        _frameState.update { it.copy(hands = emptyList()) }
        _tracking.value = TrackingState.CameraUnavailable
        lastErrorMessage = message
    }

    /** Diagnostics-only; the UI deliberately never displays this. */
    var lastErrorMessage: String? = null
        private set
}
