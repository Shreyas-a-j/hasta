# M0 Dependency Pin — Verified 30 October 2026

Every version below was checked against the Google Maven / Maven Central metadata at
implementation time. Compatibility constraints were read from the published AAR
metadata, not guessed.

## Toolchain

| Component | Pin | Evidence |
|---|---|---|
| JDK | 17.0.20.1 | AGP 8.13 requires JDK 17 |
| Gradle | 8.14.3 (wrapper) | AGP 8.13 requires Gradle ≥ 8.13 |
| Android Gradle Plugin | 8.13.2 | Max API 36.1; supports Kotlin 2.3 |
| Kotlin + Compose compiler plugin | 2.2.21 | AGP 8.13-compatible; cached locally; Compose 1.10 metadata is Kotlin 2.0 |
| compileSdk / targetSdk | 36 | Platform installed; AGP 8.13 max 36.1 |
| minSdk | 26 | MediaPipe minimum is 24; 26 avoids legacy camera edge cases |

## Libraries

| Artifact | Pin | Constraint evidence |
|---|---|---|
| `androidx.core:core-ktx` | 1.18.0 | AAR requires compileSdk 36, AGP 8.9.1 (1.19.x requires compileSdk 37 / AGP 9.1) |
| `androidx.activity:activity-compose` | 1.13.0 | AAR requires compileSdk 36, AGP 8.9.1 |
| `androidx.lifecycle:*` | 2.10.0 | AAR requires compileSdk 35, AGP 8.6 (2.11.0 requires AGP 9.1) |
| Compose BOM | 2026.03.01 | Maps `ui` 1.10.6 (`minCompileSdk=35`, AGP 8.6) and Material3 1.4.0 |
| CameraX (`core`, `camera2`, `lifecycle`, `view`) | 1.6.2 | AAR requires compileSdk 36, AGP 8.9.1 |
| `com.google.mediapipe:tasks-vision` | 1.2.0 | minSdk 24; Apache-2.0; API identical to 0.10.x |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.11.0 | Maven Central stable |

Deliberately **not** used: Compose BOM 2026.09.00 and later. Its `ui` 1.12.1 requires
compileSdk 37 and AGP 9.1, which would force an unplanned toolchain migration.

Fallback only with approval: `tasks-vision` 0.10.35, if 1.2.0 shows a runtime defect.

## Native payload

`tasks-vision` is a thin API AAR. Native code is in `tasks-core` 1.2.0:

| ABI | `libmediapipe_tasks_jni.so` |
|---|---|
| arm64-v8a | 15.4 MB |
| armeabi-v7a | 11.0 MB |
| x86 | 22.3 MB |
| x86_64 | 19.3 MB |

The debug APK includes all ABIs. This affects install size, not runtime performance.

## Model asset

| Field | Value |
|---|---|
| File | `android/app/src/main/assets/hand_landmarker.task` |
| Source | `https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task` |
| Version | float16 v1 (last modified 2023-04-26) |
| Size | 7,819,105 bytes (7.46 MiB) |
| SHA-256 | `fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1` |
| Licence | Apache License 2.0 (model card) |
| Contents | Palm detector 192×192 + hand landmark model 224×224, float16 |

The model is bundled in the APK. It is never downloaded at runtime.

## Offline and telemetry control

MediaPipe's official documentation states that the Tasks APIs send usage metrics to
Google. The application therefore declares **no `INTERNET` permission**, so the
library cannot transmit. This is verified in the merged manifest at build time. The
app performs no network access of any kind.

The dependency itself still contains the telemetry code path; this is recorded as a
known limitation in the M0 report.
