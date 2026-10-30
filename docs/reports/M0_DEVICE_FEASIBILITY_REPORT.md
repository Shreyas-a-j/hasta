# M0 Device Feasibility Report

**Status: all automated and physical-device gates PASS; the ten-minute benchmark
completed with `endReason=duration_complete`.**

**Report date:** 2026-10-30 (UTC)
**Milestone:** M0 — device feasibility spike
**Variant under test:** debug (`com.hasta.app`, `0.1.0-m0-debug`)
**Test device:** moto g64 5G (`ZA222MM3L5`), Android 15 (SDK 35), 1080×2400, 11.6 GB RAM

## 1. Environment

| Item | Value |
|---|---|
| Host OS | Linux 7.0.0-38-generic x86_64 |
| JDK | OpenJDK 17.0.20.1 |
| Gradle | 8.14.3 (wrapper) |
| Android Gradle Plugin | 8.13.2 |
| Project Kotlin | 2.2.21 |
| compileSdk / targetSdk | 36 / 36 |
| minSdk | 26 |
| Compose BOM | 2026.03.01 |
| CameraX | 1.6.2 |
| MediaPipe tasks-vision | 1.2.0 |
| Application ID | `com.hasta.app` |
| Model asset | `hand_landmarker.task`, 7,819,105 bytes, SHA-256 `fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1` |
| Device | moto g64 5G, Android 15 (SDK 35), 1080×2400, 11.6 GB RAM |

## 2. Automated verification

All commands were run from `android/` with
`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`.

| Check | Command | Result |
|---|---|---|
| Debug build | `./gradlew :app:assembleDebug` | **PASS** |
| Release build | `./gradlew :app:assembleRelease` | **PASS** |
| Lint | `./gradlew :app:lintDebug` | **PASS** |
| JVM unit tests | `./gradlew :app:testDebugUnitTest` | **PASS** — 75 tests, 0 failures, 0 errors, 0 skipped |
| Debug APK permissions | `aapt2 dump permissions app-debug.apk` | `CAMERA` only (plus AndroidX's non-exported dynamic-receiver permission) |
| Release APK permissions | `aapt2 dump permissions app-release-unsigned.apk` | `CAMERA` only (plus the same AndroidX permission) |
| Release benchmark exclusion | scan release DEX for `hasta.m0.benchmark/1`, `peakPssKb`, `duration_complete` | **PASS** — no matches. The reflective lookup constant `DebugBenchmarkRecorder` remains in `BenchmarkFactory`, but the class and schema strings are absent, so `Class.forName` fails as intended |
| Debug benchmark presence | same scan of debug DEX | **PASS** — benchmark strings present, confirming the debug-only source set is active |

No `INTERNET` or `ACCESS_NETWORK_STATE` permission is present in either APK. MediaPipe's
manifest permissions are removed at merge time.

The new precision tests are part of the 75: `LandmarkSmootherTest` covers jitter
reduction, fast-motion following, handedness/world-landmark preservation, hand-order
swapping and the long-gap reset; `ExposurePolicyTest` covers EV selection, hysteresis,
step conversion and device-range clamping.

## 3. Gate results

| Gate | Evidence | Result |
|---|---|---|
| G1 — `assembleDebug` succeeds | Command above | **PASS** |
| G2 — Permission grant/deny/permanent/settings-return on device | Permission screen shown after `pm revoke`; system dialog captured; denial produced the permanent-denial screen with **Open settings** (`USER_FIXED`); Settings opened the app info page; granting Camera and pressing Back resumed the app and restarted the camera. The intermediate "denied once" screen was not separately observed because this Android 15 build set `USER_FIXED` on the first denial after an adb revoke. | **PASS** |
| G3 — Preview works on the physical phone | Live front-camera preview observed in portrait and landscape, with frames and luma samples continuing | **PASS** |
| G4 — Analysis receives frames continuously; counters increase | Ten-minute run recorded 14,925 submitted / 5,039 processed / 9,887 skipped / 0 errors; luma sampling continued throughout | **PASS** |
| G5 — One hand detected | One-hand state and a single skeleton observed during device testing | **PASS** |
| G6 — Two hands detected | Two distinct skeletons (white and red) observed, each on the correct hand | **PASS** |
| G7 — 21 landmarks per hand exposed | Both skeletons render all 21 points; mapper rejects incomplete hands rather than padding | **PASS** |
| G8 — Overlay aligns at 0° and 90° device rotation | Overlay aligned in portrait and landscape during device testing | **PASS** |
| G9 — Front-camera mirroring aligns; handedness correct | Display-only mirror; signer confirmed the red skeleton corresponded to the physical right hand | **PASS** |
| G10 — Rotation does not displace or crash overlay | Repeated portrait/landscape changes, same PID, no crash, overlay stayed aligned | **PASS** |
| G11 — No proxy leak or analyzer stall across enter/exit | 10× Home/resume cycles plus repeated rotations: PID unchanged (`14645`), no `FATAL EXCEPTION`, one camera device open, preview and frame flow recovered | **PASS** |
| G12 — Ten-minute benchmark completed on the physical phone | Run `benchmark-20261030-230602`: `endReason=duration_complete`, 600.9 s, 14,925 submitted / 5,039 processed / 9,887 skipped / 0 errors, 8.39 FPS | **PASS** |
| G13 — Benchmark CSV + JSON export and report generated | Both files exported from `files/benchmark/`; CSV has 5,031 data rows, JSON carries the full schema and this report records the result | **PASS** |
| G14 — Limitations documented | Section 7 of this report | **PASS** |

## 4. Benchmark status

### 4.1 Diagnostic run (pre-precision build)

A real device run was made before the precision changes and stopped by the user after
33.6 s. It is reported only as a diagnostic sample, not as a gate pass:

| Metric | Value |
|---|---|
| `endReason` | `stopped_by_user` |
| Duration | 33.6 s |
| Submitted / processed / skipped / errors | 969 / 233 / 736 / 0 |
| Effective FPS | 6.93 |
| Frame interval p50 / p95 / p99 | 141 / 211 / 258 ms |
| Latency p50 / p95 / p99 | 241 / 319 / 363 ms |
| PSS start / peak / end | 306,046 / 309,754 / 262,737 KB |
| Battery / charging | 30% / charging, 34 °C |
| Thermal | none |
| Analysis resolution | 640×480 |
| Preview resolution | 1440×1088 |

### 4.2 Ten-minute run (precision build)

Run `benchmark-20261030-230602`, started 2026-10-30 22:56:01 IST and completed
at 23:06:02 IST. `endReason` is `duration_complete`; duration 600.9 s.

| Metric | Value |
|---|---|
| `endReason` | `duration_complete` |
| Duration | 600.9 s |
| Submitted / processed / skipped / errors | 14,925 / 5,039 / 9,887 / 0 |
| Recorded frames | 5,031 |
| Effective FPS | 8.39 |
| Frame interval p50 / p95 / p99 | 120 / 148 / 174 ms (min 56, max 244, mean 119.3) |
| Latency p50 / p95 / p99 | 233 / 259 / 286 ms (min 154, max 330, mean 233.5) |
| PSS start / peak / end | 282,824 / 353,857 / 310,103 KB (577 samples) |
| Battery / charging | 29% → 29% / charging throughout |
| Battery temperature | 36 → 37 °C, peak 37 °C |
| Thermal | none at start, end and peak |
| Analysis resolution | 640×480 (1.3333) |
| Preview resolution | 1440×1088 (1.3235) |
| Target rotation | 90° |
| Delegate | GPU |
| Smoothing / exposure | `one_euro` / `adaptive_luma` |

Two earlier attempts on the same build were stopped with the status chip
(`stopped_by_user`, 196.8 s and 2.8 s) and are excluded from the gate result. The
`endReason` field is what distinguishes them from the completed run.

The raw export is kept in the repository as
`docs/reports/evidence/benchmark-20261030-230602.json` and
`docs/reports/evidence/benchmark-20261030-230602.csv`.

The implementation writes:

- `files/benchmark/benchmark-<timestamp>.csv` — per-frame
  `frame_index,result_elapsed_ms,latency_ms,interval_ms`.
- `files/benchmark/benchmark-<timestamp>.json` — schema `hasta.m0.benchmark/1` with
  device/app build, actual analysis and preview resolutions, target rotation, delegate,
  smoothing/exposure settings, submitted/processed/skipped/error deltas, effective and
  recorded FPS, nearest-rank p50/p95/p99 latency and interval, PSS start/peak/end,
  battery level/charging/temperature, and thermal start/end/max plus histogram.

`endReason` distinguishes `duration_complete` from `stopped_by_user` and
`pipeline_stopped`, so a partial run cannot be mistaken for a completed ten-minute
run. The recorder is compiled only into the debug variant; the release APK contains
no benchmark classes and the UI receives a null recorder.

## 5. Precision work (verified on device)

- **GPU delegate:** the device log records
  `HandLandmarker created with GPU delegate`; the CPU fallback was not needed.
- **Adaptive smoothing:** a per-landmark One Euro filter (minCutoff 0.5, beta 0.5,
  dCutoff 1.0, 600 ms max gap) with handedness + wrist-distance association. Only
  image landmarks are filtered; world landmarks, handedness and scores are unchanged.
- **Low-light exposure:** average luma from the central Y-plane region. Measured on
  device: covered camera `luma=17 → +2 EV (index 12)`; bright `luma=156 → +1 EV`;
  `luma=84 → 0 EV`; dim room `luma=44–56 → +1 EV`; bright `luma=98–140 → 0 EV`.
  The device reports a 1/6 EV compensation step, so index 6 = 1 EV.

## 6. What was verified without hardware

- The app builds in debug and release, and lint passes.
- 75 JVM tests pass, covering hand topology, domain validation, frame mapping,
  rotation-aware overlay transform, centre-crop, display mirroring, permission policy,
  monotonic frame timestamps, benchmark percentiles and frame-series accumulation,
  adaptive smoothing and exposure policy.
- The generated APKs request camera permission only and have no network permission.
- The debug APK contains the benchmark; the release APK does not.
- The bundled model hash and size match the published MediaPipe float16 model.

These results do not prove camera behaviour, overlay alignment on a real display,
MediaPipe throughput, or sustained thermal/battery behaviour. Those are covered only by
the device gates above.

## 7. Limitations and known constraints

1. **Ten-minute evidence is from one device only.** The completed run is 600.9 s on a
   moto g64 5G; no other phone was tested.
2. **Single denial was not observed separately.** After an adb revoke, this Android 15
   build set `USER_FIXED` on the first denial, so the app moved straight to the
   permanent-denial screen.
3. **Emulator output is not evidence.** No emulator result may be substituted for a
   physical-device run.
4. **MediaPipe-internal drops are not observable.** Only submitted, processed and
   skipped frames are reported. The report must not invent a total drop count.
5. **Analysis resolution is a request.** The pipeline asks for 640×480 4:3; the device
   may select another supported size. The real sizes are exported with each run.
   The observed preview (1440×1088) and analysis (640×480) differ by 0.7% in aspect
   ratio; the center-crop transform handles this, but it is recorded as a known
   mismatch.
6. **GPU delegate is an experiment.** It is attempted first with an automatic CPU
   fallback at task creation, and the chosen delegate is recorded per run. A delegate
   failure after successful creation is not auto-switched; it surfaces as a tracking
   error. GPU vs CPU throughput is not benchmarked head-to-head here.
7. **Exposure compensation is bounded and device-dependent.** It biases auto-exposure
   by at most +2 EV and only when the device reports support and a non-zero step.
8. **Thermal status is unavailable below API 29.** Those samples are recorded as -1 /
   `unavailable`, never as "none".
9. **Memory is process PSS** sampled with `Debug.getPss()` at 1 Hz. A peak between
   samples can be missed; the value is a sampled peak, not an absolute maximum.
10. **Benchmark sampling runs on the main thread** at 1 Hz for a debug build. Its own
    cost is not subtracted from the measurements.
11. **Handedness is subject-relative.** Frames are not mirrored before inference. The
    overlay mirror is display-only.
12. **One front camera, portrait and landscape only.** Lens switching, zoom and
    multi-camera behaviour are out of scope.
13. **The benchmark trigger is deliberately hidden** (long-press the status chip), so
    it does not appear on the ordinary screen.

## 8. Reproduction steps

Build and test:

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:assembleDebug
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:testDebugUnitTest
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:assembleRelease
```

On a physical device:

```bash
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then follow the device protocol in `docs/tasks/M0.md`, finish with the ten-minute
benchmark, and export:

```bash
adb shell run-as com.hasta.app ls files/benchmark
adb exec-out run-as com.hasta.app cat files/benchmark/<run>.json > m0-benchmark.json
adb exec-out run-as com.hasta.app cat files/benchmark/<run>.csv > m0-benchmark.csv
```

Only accept a run as completed after confirming the `endReason` is
`duration_complete`. Do not convert a partial run into a completed one.
