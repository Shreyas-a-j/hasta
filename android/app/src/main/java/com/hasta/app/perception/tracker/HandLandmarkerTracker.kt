package com.hasta.app.perception.tracker

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.hasta.app.domain.model.HandFrame
import com.hasta.app.perception.PipelineStats
import com.hasta.app.perception.smoothing.LandmarkSmoother
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns the MediaPipe Hand Landmarker task and all MediaPipe types.
 *
 * Exactly one frame may be in flight. Live-stream mode silently ignores frames
 * submitted while busy; this class makes that an explicit, counted skip so the
 * analyzer never builds an unbounded queue.
 */
class HandLandmarkerTracker(
    context: Context,
    private val stats: PipelineStats,
    private val onResult: (HandFrame, FrameTiming) -> Unit,
    private val onError: (String) -> Unit,
) : Closeable {

    private val clock = FrameClock()
    private val busy = AtomicBoolean(false)
    private val smoother = LandmarkSmoother()

    @Volatile
    private var pending: PendingFrame? = null

    @Volatile
    private var closed = false

    private var landmarker: HandLandmarker? = null

    /** "GPU" or "CPU", recorded with every benchmark run. */
    @Volatile
    var delegateName: String = "unknown"
        private set

    val isReady: Boolean
        get() = landmarker != null

    init {
        landmarker = try {
            createLandmarker(context)
        } catch (error: Throwable) {
            onError("Could not initialise hand tracking: ${error.message ?: error.javaClass.simpleName}")
            null
        }
    }

    fun isBusy(): Boolean = busy.get()

    /**
     * Submits one frame. Returns true if MediaPipe accepted it.
     *
     * [bitmap] must already be upright (see [com.hasta.app.perception.camera.FrameRotation]);
     * no rotation is passed to the task. Asking the task to rotate produced landmarks
     * in the unrotated sensor frame on the target device, which put the skeleton 180
     * degrees out.
     *
     * @param imageWidth upright width of [bitmap]; landmarks are normalized to it.
     * @param imageHeight upright height of [bitmap].
     * @param rotationDegrees rotation that was applied to obtain the upright bitmap,
     * kept for `HandFrame` metadata and the benchmark only.
     * @param captureElapsedMs camera frame timestamp from `ImageInfo.timestamp`
     * (nanoseconds) converted to elapsed-realtime milliseconds.
     */
    fun detect(
        bitmap: Bitmap,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        isFrontCamera: Boolean,
        captureElapsedMs: Long,
    ): Boolean {
        val task = landmarker ?: return false
        if (closed) return false
        if (!busy.compareAndSet(false, true)) return false

        val executeAtMs = clock.next(SystemClock.uptimeMillis())
        pending = PendingFrame(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            rotationDegrees = rotationDegrees,
            isFrontCamera = isFrontCamera,
            captureElapsedMs = captureElapsedMs,
        )

        val image = BitmapImageBuilder(bitmap).build()
        return try {
            task.detectAsync(image, executeAtMs)
            true
        } catch (error: Throwable) {
            busy.set(false)
            pending = null
            image.close()
            stats.onError()
            onError("Hand tracking failed: ${error.message ?: error.javaClass.simpleName}")
            false
        }
    }

    override fun close() {
        closed = true
        busy.set(false)
        pending = null
        landmarker?.close()
        landmarker = null
    }

    private fun createLandmarker(context: Context): HandLandmarker {
        val gpu = runCatching {
            createLandmarker(context, Delegate.GPU)
        }
        if (gpu.isSuccess) {
            delegateName = DELEGATE_GPU
            Log.i(TAG, "HandLandmarker created with $DELEGATE_GPU delegate")
            return gpu.getOrThrow()
        }

        Log.w(TAG, "GPU delegate unavailable; falling back to CPU", gpu.exceptionOrNull())
        delegateName = DELEGATE_CPU
        return createLandmarker(context, Delegate.CPU).also {
            Log.i(TAG, "HandLandmarker created with $DELEGATE_CPU delegate")
        }
    }

    private fun createLandmarker(context: Context, delegate: Delegate): HandLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .setDelegate(delegate)
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(MAX_HANDS)
            .setMinHandDetectionConfidence(MIN_DETECTION_CONFIDENCE)
            .setMinHandPresenceConfidence(MIN_PRESENCE_CONFIDENCE)
            .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)
            .setResultListener(::handleResult)
            .setErrorListener(::handleError)
            .build()

        return HandLandmarker.createFromOptions(context, options)
    }

    private fun handleResult(result: HandLandmarkerResult, input: MPImage) {
        val frame = pending
        pending = null
        busy.set(false)
        input.close()

        if (closed) return

        if (frame == null) {
            stats.onError()
            onError("Tracker returned a result without a pending frame")
            return
        }

        val hands = result.landmarks().mapIndexed { index, landmarks ->
            val category = result.handedness().getOrNull(index)?.firstOrNull()
            RawHand(
                handednessLabel = category?.categoryName(),
                handednessScore = category?.score() ?: 0f,
                imageLandmarks = landmarks.map { landmark ->
                    RawLandmark(landmark.x(), landmark.y(), landmark.z())
                },
                worldLandmarks = result.worldLandmarks().getOrNull(index)?.map { landmark ->
                    RawLandmark(landmark.x(), landmark.y(), landmark.z())
                },
            )
        }

        val mapped = HandResultMapper.map(
            timestampMs = result.timestampMs(),
            imageWidth = frame.imageWidth,
            imageHeight = frame.imageHeight,
            rotationDegrees = frame.rotationDegrees,
            isFrontCamera = frame.isFrontCamera,
            hands = hands,
        )

        if (mapped == null) {
            stats.onError()
            onError("Tracker returned invalid landmark data")
            return
        }

        stats.onProcessed()
        onResult(
            smoother.smooth(mapped),
            FrameTiming(
                captureElapsedMs = frame.captureElapsedMs,
                resultElapsedMs = SystemClock.elapsedRealtime(),
                rotationDegrees = frame.rotationDegrees,
            ),
        )
    }

    private fun handleError(error: RuntimeException) {
        busy.set(false)
        pending = null
        stats.onError()
        onError(error.message ?: "Unknown hand tracking error")
    }

    private data class PendingFrame(
        val imageWidth: Int,
        val imageHeight: Int,
        val rotationDegrees: Int,
        val isFrontCamera: Boolean,
        val captureElapsedMs: Long,
    )

    companion object {
        private const val TAG = "HastaTracker"
        const val MODEL_ASSET = "hand_landmarker.task"
        const val MAX_HANDS = 2
        const val DELEGATE_GPU = "GPU"
        const val DELEGATE_CPU = "CPU"

        // Documented MediaPipe defaults. M0 does not tune these without evidence.
        const val MIN_DETECTION_CONFIDENCE = 0.5f
        const val MIN_PRESENCE_CONFIDENCE = 0.5f
        const val MIN_TRACKING_CONFIDENCE = 0.5f
    }
}
