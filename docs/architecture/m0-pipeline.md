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
        ├─ if tracker busy → skip + count → close ImageProxy
        ├─ ImageProxy.toBitmap()
        ├─ HandLandmarker.detectAsync(mpImage, rotation, timestamp)
        └─ close ImageProxy in finally
                 │
                 ▼ MediaPipe result listener
        HandResultMapper (MediaPipe types → HASTA types)
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
| Upright analysis | `W' × H'` | after `rotationDegrees` (90/270 swap W and H) |
| Overlay view | `vw × vh` | Compose Canvas over the preview, same bounds |

### Steps

1. Read `ImageProxy.imageInfo.rotationDegrees` (`0`, `90`, `180`, `270`).
2. Hand the unrotated bitmap and the rotation to MediaPipe:
   `ImageProcessingOptions.builder().setRotationDegrees(rotationDegrees)`.
   MediaPipe returns landmarks normalized to the upright image.
3. Do **not** mirror the input. Handedness stays subject-relative, as documented by
   Google ("Passing a horizontally mirrored image will invert these outputs").
4. Map normalized `(x, y)` to view pixels with center-crop (`FILL_CENTER`):

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

5. The preview stream and the analysis stream both request 4:3 with
   `AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY`. If their real ratios
   differ, the transform is invalid; actual resolutions are logged and exported with
   every benchmark run, and the device checklist includes alignment in both
   orientations.

### Rotation

The activity is not locked and does not swallow configuration changes. Rotation
recreates the activity, CameraX rebinds with the new `targetRotation`, the analysis
rotation changes, the view size changes, and the transform is recomputed. The
overlay is cleared during rebind so a stale skeleton is never shown.

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
| Delegate | CPU |
| Output | handedness category + score, 21 image landmarks, 21 world landmarks |

These are the documented defaults. M0 does not tune them; tuning requires evidence.

Tracking confidence is tracker quality, not sign-recognition confidence. M0 performs
no recognition.

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

## Known constraints

- One front camera; no lens switch.
- 640×480 analysis request; the device may choose the closest supported size.
- Emulator camera output is not evidence for any camera or performance gate.
