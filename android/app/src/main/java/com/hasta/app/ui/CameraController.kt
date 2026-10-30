package com.hasta.app.ui

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.hasta.app.data.benchmark.BenchmarkCameraInfo
import com.hasta.app.data.benchmark.BenchmarkCounters
import com.hasta.app.data.benchmark.BenchmarkRecorder
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
 * Owns the camera session for the screen and exposes immutable UI state.
 *
 * Results arrive on MediaPipe's callback thread; Compose observes the StateFlow.
 * [benchmark] is null in release builds, so no benchmark path is reachable there.
 */
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    val benchmark: BenchmarkRecorder? = null,
) {
    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    val stats = PipelineStats()

    private var session: CameraSession? = null

    fun start() {
        if (session != null) return
        _state.value = CameraUiState(tracking = TrackingState.Initializing)

        session = CameraSession(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            stats = stats,
            onFrame = { frame, timing ->
                benchmark?.onFrame(
                    resultElapsedMs = timing.resultElapsedMs,
                    latencyMs = timing.latencyMs,
                    rotationDegrees = timing.rotationDegrees,
                )
                publishFrame(frame)
            },
            onTrackingError = ::publishTrackingError,
            onCameraError = ::publishCameraError,
            onCameraReady = {
                _state.update { current ->
                    if (current.tracking == TrackingState.Initializing) {
                        current.copy(tracking = TrackingState.CameraReady)
                    } else {
                        current
                    }
                }
            },
        ).also { it.start() }
    }

    fun stop() {
        benchmark?.onPipelineStopped()
        session?.stop()
        session = null
        _state.value = CameraUiState(tracking = TrackingState.Initializing)
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

    private fun publishFrame(frame: HandFrame) {
        _state.value = CameraUiState(
            tracking = TrackingState.fromHandCount(frame.hands.size),
            hands = frame.hands,
            imageWidth = frame.imageWidth,
            imageHeight = frame.imageHeight,
        )
    }

    private fun publishTrackingError(message: String) {
        _state.value = _state.value.copy(
            tracking = TrackingState.TrackingError,
            hands = emptyList(),
            message = message,
        )
    }

    private fun publishCameraError(message: String) {
        _state.value = _state.value.copy(
            tracking = TrackingState.CameraUnavailable,
            hands = emptyList(),
            message = message,
        )
    }
}
