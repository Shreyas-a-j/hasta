# HASTA

Offline-first Indian Sign Language learning application.

**Current state: M0 — device feasibility spike.** This repository contains only the
technical pipeline needed to answer one question: can CameraX and MediaPipe Hand
Landmarker track up to two hands continuously, with an aligned skeleton overlay, on
the target physical Android phone?

There is no sign recognition, vocabulary, lesson system or navigation yet.

## What M0 does

- Requests camera permission and handles every permission state.
- Shows a front-camera CameraX preview.
- Runs MediaPipe Hand Landmarker in `LIVE_STREAM` mode with up to two hands.
- Draws all 21 landmarks per hand, using the correct hand connections.
- Handles front-camera mirroring and device rotation.
- Shows a simple tracking state (no hands / one hand / two hands / error).
- Provides a debug-only ten-minute benchmark with CSV/JSON export.

## Requirements

| Tool | Version |
|---|---|
| JDK | 17 |
| Android SDK Platform | 36 |
| Android build-tools | 36.0.0 |
| Android device | API 26+, physical phone with a front camera |
| Gradle | provided wrapper (8.14.3) |

The project deliberately has **no INTERNET permission**.

## Build

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:assembleDebug
```

The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`.

Run unit tests:

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:testDebugUnitTest
```

## Install and run

1. Enable USB debugging on the phone and connect it.
2. Confirm the device is visible:

   ```bash
   adb devices
   ```

3. Install:

   ```bash
   adb install -r android/app/build/outputs/apk/debug/app-debug.apk
   ```

4. Open **HASTA M0** and grant camera permission.
5. Verify the permission matrix and device scenarios in
   [`docs/tasks/M0.md`](docs/tasks/M0.md).

## Benchmark

The benchmark is available only in debug builds. Long-press the tracking status
chip to start a ten-minute run (it stops automatically; long-press again to abort).

Export files are written to the app-private files directory
(`files/benchmark/`), so no storage permission is required:

```bash
adb shell run-as com.hasta.app ls files/benchmark
adb exec-out run-as com.hasta.app cat files/benchmark/<run>.json > m0-benchmark.json
adb exec-out run-as com.hasta.app cat files/benchmark/<run>.csv > m0-benchmark.csv
```

Stopping early, rotating the device or leaving the app also ends a run; the export
records an `endReason` so a partial run is never presented as a full ten minutes.

See [`docs/architecture/m0-pipeline.md`](docs/architecture/m0-pipeline.md) for the
measurement definitions and [`docs/tasks/M0.md`](docs/tasks/M0.md) for the device
test protocol.

## Documentation

- [`docs/spec/product-scope.md`](docs/spec/product-scope.md) — M0 boundaries
- [`docs/architecture/m0-pipeline.md`](docs/architecture/m0-pipeline.md) — pipeline, coordinate transform, metrics
- [`docs/architecture/m0-dependencies.md`](docs/architecture/m0-dependencies.md) — pinned versions and licences
- [`docs/decisions/ADR-0001-hands-only-m0.md`](docs/decisions/ADR-0001-hands-only-m0.md) — hands-only and mirroring decisions
- [`docs/reports/M0_DEVICE_FEASIBILITY_REPORT.md`](docs/reports/M0_DEVICE_FEASIBILITY_REPORT.md) — final evidence (produced after device testing)
