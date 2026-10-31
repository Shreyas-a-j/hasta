# HASTA

Offline-first Indian Sign Language learning application.

**Current state: M0.5 — brand theme and Live Sign Coach camera shell.** The M0
camera and tracking engine is complete and verified on a physical device. M0.5 adds
the HASTA design system and replaces the feasibility screen with a polished, honest
coaching interface.

There is still no sign recognition, vocabulary, lesson system or navigation.

## What M0.5 does

- Applies the locked HASTA palette in light and dark themes, with documented contrast.
- Presents Live Sign Coach: ready, no hands, one hand, two hands, practising,
  practice complete, tracking paused, camera unavailable and permission states.
- Runs a five-second practice window that reports whether tracking stayed stable.
  It does not recognise, grade or save anything, and the copy says so.
- Keeps the camera preview and hand skeleton as the primary content, with no raw
  coordinates, FPS or confidence on the live screen.
- Handles rotation cleanly: bottom panel in portrait, right-side panel in landscape.
- Supports font scaling, screen readers, 48dp touch targets, reduced motion and
  non-colour status.

## What M0 established

- CameraX Preview + ImageAnalysis (front camera, 4:3) with
  `STRATEGY_KEEP_ONLY_LATEST`.
- MediaPipe Hand Landmarker in `LIVE_STREAM` mode with up to two hands.
- All 21 landmarks per hand with correct connections and display-only mirroring.
- Debug-only ten-minute benchmark with CSV/JSON export.
- Verified baseline: 8.39 FPS over 600.9 s on a moto g64 5G.

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

Run unit tests, lint and release build:

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:testDebugUnitTest
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:lintDebug
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:assembleRelease
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

4. Open **HASTA** and grant camera permission.
5. Bring both hands into view and start a practice window.
6. Rotate the device while a window is running to confirm the layout and timer.

## Benchmark

The benchmark is available only in debug builds. Long-press the tracking status
pill to start a ten-minute run (it stops automatically; long-press again to abort).

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

- [`docs/spec/product-scope.md`](docs/spec/product-scope.md) — M0 scope and M0.5 addendum
- [`docs/spec/visual-spec.md`](docs/spec/visual-spec.md) — normative design system
- [`docs/architecture/m0-pipeline.md`](docs/architecture/m0-pipeline.md) — pipeline, coordinate transform, metrics
- [`docs/architecture/m0-dependencies.md`](docs/architecture/m0-dependencies.md) — pinned versions and licences
- [`docs/decisions/ADR-0001-hands-only-m0.md`](docs/decisions/ADR-0001-hands-only-m0.md) — hands-only and mirroring decisions
- [`docs/reports/M0_DEVICE_FEASIBILITY_REPORT.md`](docs/reports/M0_DEVICE_FEASIBILITY_REPORT.md) — M0 device evidence
- [`docs/reports/M0_5_BRAND_AND_CAMERA_SHELL_REPORT.md`](docs/reports/M0_5_BRAND_AND_CAMERA_SHELL_REPORT.md) — M0.5 report
