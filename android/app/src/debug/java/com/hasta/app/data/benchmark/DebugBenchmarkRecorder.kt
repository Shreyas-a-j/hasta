package com.hasta.app.data.benchmark

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ten-minute debug benchmark.
 *
 * This class exists only in the `debug` source set. It is constructed reflectively by
 * [BenchmarkFactory], so no part of it ships in a release build.
 *
 * Runs are started by long-pressing the status chip. A run records pipeline counter
 * deltas, per-frame latency and interval samples, 1 Hz PSS/battery/thermal samples,
 * and the resolution/rotation reported by CameraX. The CSV holds per-frame rows; the
 * JSON holds the summary, device facts and the 1 Hz samples.
 */
class DebugBenchmarkRecorder @JvmOverloads constructor(
    context: Context,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val sampler: DeviceSampler = DeviceSampler(context),
    private val exporter: BenchmarkExporter = BenchmarkExporter(context),
    private val durationMs: Long = DEFAULT_DURATION_MS,
    private val sampleIntervalMs: Long = DEFAULT_SAMPLE_INTERVAL_MS,
) : BenchmarkRecorder {

    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow<BenchmarkState>(BenchmarkState.Idle)
    override val state: StateFlow<BenchmarkState> = _state.asStateFlow()

    private var currentRun: Run? = null

    private val tick = object : Runnable {
        override fun run() {
            val finished: Boolean
            synchronized(lock) {
                val run = currentRun ?: return
                run.samples.add(sampler.sample())
                finished = clock() - run.startedElapsedMs >= durationMs
            }
            if (finished) {
                synchronized(lock) { finishLocked(END_DURATION) }
            } else {
                handler.postDelayed(this, sampleIntervalMs)
            }
        }
    }

    override fun onFrame(
        resultElapsedMs: Long,
        latencyMs: Long,
        rotationDegrees: Int,
    ) {
        synchronized(lock) {
            val run = currentRun ?: return
            val relative = resultElapsedMs - run.startedElapsedMs
            if (relative < 0L || relative > durationMs) return
            run.series.onResult(
                resultElapsedMs = resultElapsedMs,
                latencyMs = latencyMs,
                rotationDegrees = rotationDegrees,
            )
        }
    }

    override fun toggle(
        camera: BenchmarkCameraInfo?,
        counters: () -> BenchmarkCounters,
    ) {
        synchronized(lock) {
            if (currentRun == null) {
                startLocked(camera, counters)
            } else {
                finishLocked(END_USER_STOPPED)
            }
        }
    }

    override fun onPipelineStopped() {
        synchronized(lock) { finishLocked(END_PIPELINE_STOPPED) }
    }

    private fun startLocked(
        camera: BenchmarkCameraInfo?,
        counters: () -> BenchmarkCounters,
    ) {
        val run = Run(
            startedElapsedMs = clock(),
            startedWallMs = wallClock(),
            camera = camera,
            baseline = counters(),
            counters = counters,
            startDevice = sampler.deviceInfo(),
            startSample = sampler.sample(),
            series = FrameSeries(),
        )
        currentRun = run
        _state.value = BenchmarkState.Running(
            startedAtElapsedMs = run.startedElapsedMs,
            durationMs = durationMs,
        )
        handler.postDelayed(tick, sampleIntervalMs)
    }

    private fun finishLocked(reason: String) {
        val run = currentRun ?: return
        currentRun = null
        handler.removeCallbacks(tick)

        val endedElapsedMs = clock()
        val endedWallMs = wallClock()
        val endSample = sampler.sample()
        val endCounters = runCatching { run.counters() }.getOrElse { run.baseline }
        val snapshot = run.series.snapshot()

        val elapsedMs = (endedElapsedMs - run.startedElapsedMs).coerceAtLeast(0L)
        val submitted = (endCounters.submitted - run.baseline.submitted).coerceAtLeast(0L)
        val processed = (endCounters.processed - run.baseline.processed).coerceAtLeast(0L)
        val skipped = (endCounters.skipped - run.baseline.skipped).coerceAtLeast(0L)
        val errors = (endCounters.errors - run.baseline.errors).coerceAtLeast(0L)
        val effectiveFps = if (elapsedMs > 0) processed * 1000.0 / elapsedMs else 0.0
        val recordedFps = if (elapsedMs > 0) snapshot.latencies.size * 1000.0 / elapsedMs else 0.0

        val export = runCatching {
            val stamp = FILE_STAMP.format(Date(endedWallMs))
            exporter.export(
                baseName = "benchmark-$stamp",
                json = buildJson(
                    run = run,
                    endedWallMs = endedWallMs,
                    elapsedMs = elapsedMs,
                    reason = reason,
                    submitted = submitted,
                    processed = processed,
                    skipped = skipped,
                    errors = errors,
                    effectiveFps = effectiveFps,
                    recordedFps = recordedFps,
                    snapshot = snapshot,
                    endSample = endSample,
                ),
                csv = buildCsv(snapshot),
            )
        }

        _state.value = export.fold(
            onSuccess = { files ->
                BenchmarkState.Completed(
                    jsonFile = files.jsonFile.absolutePath,
                    csvFile = files.csvFile.absolutePath,
                    processedFrames = processed,
                    endReason = reason,
                )
            },
            onFailure = { error ->
                BenchmarkState.Failed(
                    message = error.message ?: error.javaClass.simpleName,
                )
            },
        )
    }

    private fun buildJson(
        run: Run,
        endedWallMs: Long,
        elapsedMs: Long,
        reason: String,
        submitted: Long,
        processed: Long,
        skipped: Long,
        errors: Long,
        effectiveFps: Double,
        recordedFps: Double,
        snapshot: FrameSeriesSnapshot,
        endSample: DeviceSample,
    ): String {
        val frames = JSONObject()
            .put("submitted", submitted)
            .put("processed", processed)
            .put("skipped", skipped)
            .put("errors", errors)
            .put("recorded", snapshot.latencies.size)
            .put("effectiveFps", effectiveFps.round(2))
            .put("recordedFps", recordedFps.round(2))
            .put("intervalMs", percentilesJson(snapshot.intervalPercentiles))
            .put("latencyMs", percentilesJson(snapshot.latencyPercentiles))
            .put("analysisRotationDegreesSeen", JSONArray(snapshot.rotations))

        val memorySamples = run.samples.map { it.pssKb }
        val peakPss = (memorySamples + listOf(run.startSample.pssKb, endSample.pssKb)).max()
        val memory = JSONObject()
            .put("startPssKb", run.startSample.pssKb)
            .put("peakPssKb", peakPss)
            .put("endPssKb", endSample.pssKb)
            .put("samples", run.samples.size)

        val temperatures = run.samples.mapNotNull { it.batteryTemperatureC }
        val peakTemperature = (temperatures + listOfNotNull(
            run.startSample.batteryTemperatureC,
            endSample.batteryTemperatureC,
        )).maxOrNull()

        val battery = JSONObject()
            .putNullable("startPercent", run.startSample.batteryPercent)
            .putNullable("endPercent", endSample.batteryPercent)
            .putNullable("chargingStart", run.startSample.charging)
            .putNullable("chargingEnd", endSample.charging)
            .putNullable("startTemperatureC", run.startSample.batteryTemperatureC)
            .putNullable("endTemperatureC", endSample.batteryTemperatureC)
            .putNullable("peakTemperatureC", peakTemperature)

        val thermalStatuses = run.samples.map { it.thermalStatus }
        val allThermal = thermalStatuses + listOf(
            run.startSample.thermalStatus,
            endSample.thermalStatus,
        )
        val thermal = JSONObject()
            .put("startStatus", run.startSample.thermalStatus)
            .put("startStatusName", thermalStatusName(run.startSample.thermalStatus))
            .put("endStatus", endSample.thermalStatus)
            .put("endStatusName", thermalStatusName(endSample.thermalStatus))
            .put("maxStatus", allThermal.max())
            .put("maxStatusName", thermalStatusName(allThermal.max()))
            .put("samples", JSONArray(thermalStatuses))
            .put("histogram", thermalHistogram(allThermal))

        return JSONObject()
            .put("schema", SCHEMA)
            .put("startedAtWallMs", run.startedWallMs)
            .put("completedAtWallMs", endedWallMs)
            .put("durationMs", elapsedMs)
            .put("endReason", reason)
            .put("device", deviceJson(run.startDevice))
            .put("camera", cameraJson(run.camera))
            .put("frames", frames)
            .put("memory", memory)
            .put("battery", battery)
            .put("thermal", thermal)
            .toString(2)
    }

    private fun deviceJson(info: DeviceInfo): JSONObject = JSONObject()
        .put("manufacturer", info.manufacturer)
        .put("model", info.model)
        .put("device", info.device)
        .put("androidRelease", info.androidRelease)
        .put("sdkInt", info.sdkInt)
        .put("supportedAbis", JSONArray(info.supportedAbis))
        .put("appVersionName", info.appVersionName)
        .put("appVersionCode", info.appVersionCode)
        .put("totalRamMb", info.totalRamMb)

    private fun cameraJson(camera: BenchmarkCameraInfo?): Any {
        if (camera == null) return JSONObject.NULL
        return JSONObject()
            .put("analysisWidth", camera.analysisWidth)
            .put("analysisHeight", camera.analysisHeight)
            .put("analysisAspectRatio", camera.analysisAspectRatio.toDouble().round(4))
            .put("previewWidth", camera.previewWidth)
            .put("previewHeight", camera.previewHeight)
            .put("previewAspectRatio", camera.previewAspectRatio.toDouble().round(4))
            .put("targetRotationDegrees", camera.targetRotationDegrees)
            .put("delegate", camera.delegate)
            .put("smoothing", camera.smoothing)
            .put("exposure", camera.exposure)
    }

    private fun percentilesJson(percentiles: Percentiles?): Any {
        if (percentiles == null) return JSONObject.NULL
        return JSONObject()
            .put("count", percentiles.count)
            .put("p50", percentiles.p50)
            .put("p95", percentiles.p95)
            .put("p99", percentiles.p99)
            .put("min", percentiles.min)
            .put("max", percentiles.max)
            .put("mean", percentiles.mean.round(2))
    }

    private fun thermalHistogram(statuses: List<Int>): JSONObject {
        val histogram = JSONObject()
        statuses.groupingBy { thermalStatusName(it) }.eachCount().forEach { (name, count) ->
            histogram.put(name, count)
        }
        return histogram
    }

    private fun buildCsv(snapshot: FrameSeriesSnapshot): String = buildString {
        append("frame_index,result_elapsed_ms,latency_ms,interval_ms\n")
        snapshot.resultTimes.indices.forEach { index ->
            append(index).append(',')
            append(snapshot.resultTimes[index]).append(',')
            append(snapshot.latencies[index]).append(',')
            if (index > 0) append(snapshot.intervals[index - 1])
            append('\n')
        }
    }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private fun Double.round(decimals: Int): Double = when (decimals) {
        2 -> "%.2f".format(Locale.US, this).toDouble()
        4 -> "%.4f".format(Locale.US, this).toDouble()
        else -> this
    }

    private data class Run(
        val startedElapsedMs: Long,
        val startedWallMs: Long,
        val camera: BenchmarkCameraInfo?,
        val baseline: BenchmarkCounters,
        val counters: () -> BenchmarkCounters,
        val startDevice: DeviceInfo,
        val startSample: DeviceSample,
        val series: FrameSeries,
    ) {
        val samples = ArrayList<DeviceSample>()
    }

    private companion object {
        const val DEFAULT_DURATION_MS = 10L * 60L * 1000L
        const val DEFAULT_SAMPLE_INTERVAL_MS = 1_000L
        const val SCHEMA = "hasta.m0.benchmark/1"
        const val END_DURATION = "duration_complete"
        const val END_USER_STOPPED = "stopped_by_user"
        const val END_PIPELINE_STOPPED = "pipeline_stopped"
        val FILE_STAMP = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
    }
}
