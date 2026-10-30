# HASTA — Repository Rules

The locked architecture is
`HASTA_Project_Architecture_and_Product_Blueprint_v1.0.pdf` plus the documents in
`docs/`. Those documents are the source of truth.

## Current milestone

M0 — device feasibility spike only. Hands-only tracking, CameraX, MediaPipe Hand
Landmarker, Compose skeleton overlay, developer benchmark. Nothing else.

Do not add sign recognition, LiteRT, GRU/CNN, vocabulary, lessons, Room,
DataStore, authentication, cloud services, analytics, pose or face tracking.

## Architecture invariants

- One Android application module: `android/app`.
- Package boundaries: `ui/`, `domain/`, `data/`, `perception/`.
- MediaPipe types stay inside `perception/`. UI and domain use HASTA-owned types.
- Manual dependency construction through `AppContainer`. No DI framework.
- No second Gradle module without an approved ADR.
- No INTERNET permission and no network calls. This is also the hard control for
  MediaPipe's built-in usage metrics.

## Perception invariants

- `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST`; at most one frame in MediaPipe.
- Every `ImageProxy` is closed in a guaranteed cleanup path.
- Analysis never runs on the main thread; the preview must stay responsive.
- Camera frames are never mirrored before inference. Handedness is subject-relative.
  Mirroring is a display-only transform in the overlay.
- Monotonic timestamps are supplied to MediaPipe live-stream mode.
- No invented landmarks. Missing data is represented as absent, never as zeros.

## Coordinate transform

All coordinate mapping lives in `perception/overlay/OverlayTransform.kt` and is
covered by JVM unit tests. The live pipeline is:

1. `ImageProxy` in sensor orientation.
2. Rotation applied by MediaPipe through `ImageProcessingOptions`.
3. Landmarks normalized against the upright analysis image.
4. Center-crop (`FILL_CENTER`) mapping to the overlay view.
5. Display-only horizontal mirror for the front camera.

Do not replace this with "normalized × screen size".

## Benchmark rules

- Benchmark code is debug-only and must not appear on the ordinary screen.
- Report submitted, processed and skipped frames separately.
- Never fabricate device, FPS, latency, memory, battery or thermal data.
- Physical-device results are reported as NOT VERIFIED when no device was used.

## Workflow

- Plan, then implement in small independently verifiable steps.
- For each step: inspect the diff, compile, run tests, report the result.
- Do not silently change architecture or dependency versions.
- Do not push, reset hard, clean -fd, or install dependencies without approval.

## Commands

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:testDebugUnitTest
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:assembleDebug
```
