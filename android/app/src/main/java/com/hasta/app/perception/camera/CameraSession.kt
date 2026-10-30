package com.hasta.app.perception.camera

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.hasta.app.BuildConfig
import com.hasta.app.domain.model.HandFrame
import com.hasta.app.perception.PipelineStats
import com.hasta.app.perception.tracker.FrameTiming
import com.hasta.app.perception.tracker.HandLandmarkerTracker
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Binds CameraX Preview + ImageAnalysis to the lifecycle owner and owns the analyzer
 * executor and the MediaPipe tracker.
 *
 * All calls must happen on the main thread. The camera provider future is guarded by
 * a generation token so a delayed callback cannot bind after the session stopped.
 */
class CameraSession(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val stats: PipelineStats,
    private val onFrame: (HandFrame, FrameTiming) -> Unit,
    private val onTrackingError: (String) -> Unit,
    private val onCameraError: (String) -> Unit,
    private val onCameraReady: () -> Unit,
) {

    @Volatile
    private var cameraInfo: CameraInfo? = null

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var tracker: HandLandmarkerTracker? = null
    private var executor: ExecutorService? = null
    private var analysis: ImageAnalysis? = null
    private var started = false
    private var generation = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val exposurePolicy = ExposurePolicy()
    private var lastExposureUpdateMs = 0L

    fun start() {
        if (started) return
        started = true
        val token = ++generation

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!started || token != generation) return@addListener
            try {
                bind(future.get())
            } catch (error: Throwable) {
                onCameraError("Camera unavailable: ${error.message ?: error.javaClass.simpleName}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bind(cameraProvider: ProcessCameraProvider) {
        if (!cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            onCameraError("No front camera on this device")
            return
        }

        provider = cameraProvider
        val targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0

        val preview = Preview.Builder()
            .setTargetRotation(targetRotation)
            .setResolutionSelector(previewResolutionSelector())
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(targetRotation)
            .setResolutionSelector(analysisResolutionSelector())
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()

        val analysisExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "hasta-analysis")
        }
        executor = analysisExecutor

        val handTracker = HandLandmarkerTracker(
            context = context,
            stats = stats,
            onResult = onFrame,
            onError = onTrackingError,
        )
        tracker = handTracker

        imageAnalysis.setAnalyzer(
            analysisExecutor,
            FrameAnalyzer(
                tracker = handTracker,
                stats = stats,
                isFrontCamera = true,
                onError = onTrackingError,
                onLuma = ::onLumaSample,
            ),
        )
        analysis = imageAnalysis

        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            preview,
            imageAnalysis,
        )

        cameraInfo = CameraInfo(
            analysisWidth = imageAnalysis.resolutionInfo?.resolution?.width ?: 0,
            analysisHeight = imageAnalysis.resolutionInfo?.resolution?.height ?: 0,
            previewWidth = preview.resolutionInfo?.resolution?.width ?: 0,
            previewHeight = preview.resolutionInfo?.resolution?.height ?: 0,
            targetRotationDegrees = targetRotation * 90,
            delegate = handTracker.delegateName,
        )

        onCameraReady()
    }

    /**
     * Exposure compensation is applied on the main thread and rate-limited so a
     * changing scene cannot make the preview pump every frame.
     */
    private fun onLumaSample(luma: Int) {
        mainHandler.post {
            val activeCamera = camera ?: return@post
            val now = SystemClock.elapsedRealtime()
            if (now - lastExposureUpdateMs < EXPOSURE_UPDATE_INTERVAL_MS) return@post

            val state = activeCamera.cameraInfo.exposureState
            if (!state.isExposureCompensationSupported) return@post
            val range = state.exposureCompensationRange
            if (range.lower == range.upper) return@post
            val step = state.exposureCompensationStep.toFloat()
            if (step <= 0f) return@post

            val currentIndex = state.exposureCompensationIndex
            val currentEv = (currentIndex * step).roundToInt()
            val targetEv = exposurePolicy.targetEv(luma, currentEv)
            val targetIndex = ExposurePolicy.indexForEv(
                ev = targetEv,
                stepEv = step,
                currentIndex = currentIndex,
                minIndex = range.lower,
                maxIndex = range.upper,
            )

            if (targetIndex != currentIndex) {
                activeCamera.cameraControl.setExposureCompensationIndex(targetIndex)
                lastExposureUpdateMs = now
                Log.i(TAG, "Exposure luma=$luma ev=$targetEv index=$targetIndex (was $currentIndex)")
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Exposure luma=$luma ev=$targetEv index=$currentIndex (unchanged)")
                }
            }
        }
    }

    fun stop() {
        if (!started) return
        started = false
        generation++

        analysis?.clearAnalyzer()
        analysis = null
        provider?.unbindAll()
        provider = null
        camera = null
        mainHandler.removeCallbacksAndMessages(null)
        lastExposureUpdateMs = 0L

        executor?.shutdown()
        executor = null

        // Close after the analyzer is detached so no new frames can arrive.
        tracker?.close()
        tracker = null

        cameraInfo = null
    }

    fun currentCameraInfo(): CameraInfo? = cameraInfo

    private fun previewResolutionSelector(): ResolutionSelector =
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()

    private fun analysisResolutionSelector(): ResolutionSelector =
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(640, 480),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                ),
            )
            .build()

    private companion object {
        const val TAG = "HastaCamera"
        const val EXPOSURE_UPDATE_INTERVAL_MS = 1_500L
    }
}

/**
 * Resolutions reported by CameraX for the active session. The benchmark records these
 * so overlay alignment claims can be checked against the real stream sizes.
 */
data class CameraInfo(
    val analysisWidth: Int,
    val analysisHeight: Int,
    val previewWidth: Int,
    val previewHeight: Int,
    /** Clockwise rotation requested for the use cases: 0, 90, 180 or 270. */
    val targetRotationDegrees: Int,
    /** MediaPipe delegate actually created: "GPU" or "CPU". */
    val delegate: String = "unknown",
) {
    val analysisAspectRatio: Float
        get() = if (analysisHeight == 0) 0f else analysisWidth.toFloat() / analysisHeight

    val previewAspectRatio: Float
        get() = if (previewHeight == 0) 0f else previewWidth.toFloat() / previewHeight
}
