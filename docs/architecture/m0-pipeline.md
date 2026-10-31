# M0 Pipeline, Coordinate Transform and Metrics

## Runtime pipeline

```
CameraX Preview ────────────────────────────────► PreviewView
                                                   (CameraX mirrors front preview)
CameraX ImageAnalysis
  backpressure: STRATEGY_KEEP_ONLY_LATEST
  executor: single dedicated thread
        │
        ├─ frame arrives
        ├─ every 15th frame: sample central luma → exposure policy
        ├─ if tracker busy → skip + count → close ImageProxy
        ├─ ImageProxy.toBitmap()
        ├─ HandLandmarker.detectAsync(mpImage, rotation, timestamp)
        └─ close ImageProxy in finally
                 │
                 ▼ MediaPipe result listener
        HandResultMapper (MediaPipe types → HASTA types)
                 │
                 ├─ LandmarkSmoother (image landmarks only)
                 │
                 ├─ StateFlow<CameraUiState> → Compose status + overlay
                 └─ BenchmarkRecorder → latency / interval statistics
```

### Why one frame in flight

MediaPipe live-stream mode silently ignores a frame submitted while it is busy. An
explicit in-flight gate turns that into a counted, bounded skip and prevents bitmap
allocation and proxy retention from running ahead of inference. The preview is a
separate CameraX use case, so analysis throttling never blocks preview rendering.

### Frame ownership

| Resource | Owner | Release |
|---|---|---|
| `ImageProxy` | analyzer | `finally` immediately after bitmap extraction |
| `Bitmap` | analyzer → MediaPipe | wrapped in `MPImage`, released after the result callback |
| `HandLandmarker` | tracker | `close()` when the session stops |
| Analyzer executor | camera session | `shutdown()` when the session stops |

## Coordinate transform

### Spaces

| Space | Width × height | Meaning |
|---|---|---|
| Sensor image | `W × H` | raw `ImageProxy`, rotated per device |
| Upright analysis | `W' × H'` | sensor bitmap rotated in app (90/270 swap W and H) |
| Overlay view | `vw × vh` | Compose Canvas over the preview, same bounds |

`W' × H'` is also what `HandFrame.imageWidth` / `imageHeight` report, so the overlay
never re-derives it.

### Steps

1. Read `ImageProxy.imageInfo.rotationDegrees` (`0`, `90`, `180`, `270`).
2. Rotate the analysis bitmap clockwise by `rotationDegrees` **before** inference
   (`perception/camera/FrameRotation.kt`). The bitmap handed to MediaPipe is
   therefore already upright, and 90/270 swap its width and height.
3. Call `detectAsync(image, timestamp)` with **no** `ImageProcessingOptions`
   rotation. The task must not be asked to rotate: on the target device it ignored
   the request and returned landmarks in the raw sensor frame, which put the
   skeleton 180 degrees out (see
   [ADR-0002](../decisions/ADR-0002-rotate-analysis-frame-in-app.md)). Requesting
   zero rotation makes a double rotation structurally impossible.
4. Do **not** mirror the input. Handedness stays subject-relative, as documented by
   Google ("Passing a horizontally mirrored image will invert these outputs").
5. Map normalized `(x, y)` to view pixels with center-crop (`FILL_CENTER`):

```
sx    = x * W'
sy    = y * H'
scale = max(vw / W', vh / H')
dx    = (vw - W' * scale) / 2
dy    = (vh - H' * scale) / 2
px    = dx + sx * scale
py    = dy + sy * scale
if (frontCamera) px = vw - px      // display-only mirror
```

6. The preview stream and the analysis stream both request 4:3 with
   `AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY`. If their real ratios
   differ, the transform is invalid; actual resolutions are logged and exported with
   every benchmark run, and the device checklist includes alignment in both
   orientations.

### Rotation

The activity is not locked and does not swallow configuration changes. Rotation
recreates the activity, CameraX rebinds with the new `targetRotation`, the analysis
rotation changes, the view size changes, and the transform is recomputed. The
overlay is cleared during rebind so a stale skeleton is never shown.

Rotation is applied to the analysis bitmap in this process, not by MediaPipe. The
frame is rotated once, before inference, from `ImageInfo.rotationDegrees`; the task
is always called with no rotation option. See
[ADR-0002](../decisions/ADR-0002-rotate-analysis-frame-in-app.md) for the device
evidence behind that decision.

### Handedness

MediaPipe returns subject-relative handedness on unmirrored input. The overlay is
mirrored on screen for the front camera, so the signer's left hand appears on the
viewer's right. That is correct. Handedness labels are never flipped.

## Tracking states

`Initializing`, `PermissionRequired`, `CameraReady`, `NoHands`, `OneHand`,
`TwoHands`, `TrackingError`, `CameraUnavailable`.

`CameraReady` means the camera is bound and no result has arrived yet. It is not a
claim that hands are visible.

## MediaPipe configuration

| Option | Value |
|---|---|
| Running mode | `LIVE_STREAM` |
| Max hands | 2 |
| Min hand detection confidence | 0.5 |
| Min hand presence confidence | 0.5 |
| Min tracking confidence | 0.5 |
| Delegate | GPU, automatic CPU fallback |
| Output | handedness category + score, 21 image landmarks, 21 world landmarks |

These are the documented defaults. M0 does not tune confidence thresholds; tuning
requires evidence. The GPU delegate is attempted first; if task creation fails, the
tracker logs the failure and creates the same task on CPU. The chosen delegate is
recorded in every benchmark run. The fallback covers creation only; a later delegate
failure surfaces as a tracking error rather than an automatic switch.

Tracking confidence is tracker quality, not sign-recognition confidence. M0 performs
no recognition.

## Precision stages

### Adaptive landmark smoothing

`LandmarkSmoother` applies a per-landmark One Euro filter to image `x` and `y` only.
Left/right filter state is kept separate, and hands are matched frame-to-frame by
handedness plus wrist proximity (0.35 in normalized coordinates). A one-frame dropout
keeps the track for up to 600 ms; a longer gap resets the filter instead of dragging
the old position. World landmarks, handedness and scores are never rewritten. This is
a display-quality stage: it runs after mapping and before the UI state is published.

Defaults: `minCutoff = 0.5`, `beta = 0.5`, `dCutoff = 1.0`. Low cutoff at rest
suppresses shimmer; the speed term raises the cutoff during motion so the skeleton
does not trail a fast hand.

### Low-light exposure bias

`LumaMeter` samples the central 60% of the Y plane with a coarse stride on every 15th
frame. `ExposurePolicy` maps average luma to an EV target:

| Luma | Target |
|---|---|
| ≤ 40 | +2 EV |
| ≤ 70 | +1 EV |
| ≥ 110 | 0 EV |
| in between | hold current, with 8-luma hysteresis on step-down |

The target is converted to a CameraX compensation index using the device-reported
step, clamped to the supported range. Updates are applied on the main thread and
rate-limited to one every 1.5 s. A device that reports no support or a zero step is
left untouched. This biases auto-exposure; it does not replace it, and it never
modifies the frame sent to inference.

## Benchmark definitions

| Metric | Definition |
|---|---|
| Submitted frames | `ImageProxy` frames delivered to the analyzer |
| Processed frames | MediaPipe result callbacks received |
| Skipped frames | Frames discarded because the tracker was busy |
| Effective FPS | processed frames / elapsed seconds |
| Frame interval | time between consecutive result callbacks (p50/p95/p99) |
| Latency | result callback time − camera frame timestamp (p50/p95/p99) |
| Peak memory | maximum process PSS sampled at 1 Hz |
| Battery | level, charging state and temperature at start/end |
| Thermal | `PowerManager.currentThermalStatus` sampled throughout |

MediaPipe-internal drops are not fully observable. The report states the submitted,
processed and skipped counts and does not invent a drop count.

## Benchmark export

The recorder is compiled only into the `debug` variant (`src/debug/`). Release builds
create no recorder, expose no trigger and contain none of the sampling classes.

- Trigger: long-press the status chip; long-press again to stop early.
- Duration: ten minutes, 1 Hz environmental sampling.
- Files: app-private `files/benchmark/benchmark-<timestamp>.json` and `.csv`.
- CSV: per-frame `frame_index,result_elapsed_ms,latency_ms,interval_ms`.
- JSON: schema `hasta.m0.benchmark/1`, device and app build, camera sizes and target
  rotation, delegate (`GPU`/`CPU`), smoothing and exposure settings,
  submitted/processed/skipped/error deltas, effective and recorded FPS,
  latency/interval percentiles, PSS start/peak/end, battery start/end/peak
  temperature, thermal start/end/max and a thermal histogram.
- `endReason` is `duration_complete`, `stopped_by_user` or `pipeline_stopped`. A
  report must not treat a partial run as a completed ten-minute run.

Percentiles use the nearest-rank definition: rank `ceil(p / 100 * n)`, 1-based.

## Known constraints

- One front camera; no lens switch.
- 640×480 analysis request; the device may choose the closest supported size.
- Emulator camera output is not evidence for any camera or performance gate.
