# M0.5 — Brand Theme and Live Sign Coach Camera Shell

Status: **build, automated verification, physical-device verification and a ten-minute
benchmark complete.** The benchmark is not like-for-like against the M0 baseline, so
the performance comparison is partial (section 4.2).

Date: 31 October 2026.

---

## 1. Scope

M0.5 adds the HASTA design system and the Live Sign Coach camera shell on top of the
M0 tracking engine. The engine is unchanged except for the overlay rotation fix
recorded in [ADR-0002](../decisions/ADR-0002-rotate-analysis-frame-in-app.md).

Delivered:

- Locked brand palette, semantic roles and a separate dark tonal palette.
- Typography, 4dp spacing, shape, elevation and motion tokens.
- Six core components.
- Live Sign Coach states with a UI-only five-second practice window.
- Permission rationale/denial/permanent-denial screens.
- Rotation-clean portrait and landscape layouts.
- Accessibility and reduced-motion support.
- `docs/spec/visual-spec.md` as the normative design reference.

Not delivered, by design: sign recognition, LiteRT, temporal models, vocabulary,
lessons, Room/DataStore, profiles, authentication, cloud, speech, the assessment
engine, full navigation, pose and face tracking.

---

## 2. What changed

### Theme (`ui/theme/`)

| File | Purpose |
|---|---|
| `HastaColors.kt` | Light and dark semantic palettes; `HastaTone` mapping |
| `Contrast.kt` | WCAG relative-luminance and contrast helper |
| `HastaTypography.kt` | Full Material 3 type scale on system sans |
| `HastaDimens.kt` | Spacing, radius, size and `HastaShapes` tokens |
| `HastaMotion.kt` | Three durations plus reduced-motion collapse |
| `Theme.kt` | `HastaTheme`, `HastaCameraTheme`, `HastaTheme.colors/motion` |

### Live Coach (`ui/`)

| File | Purpose |
|---|---|
| `LiveCoachState.kt` | Practice window, UI state, panel/action/status enums |
| `LiveCoachPresenter.kt` | Pure live-state mapping; never emits future states |
| `CoachCopy.kt` | All panel/status/action string resolution |
| `LiveCoachScreen.kt` | Pure layout with a `stage` slot; portrait/landscape |
| `PermissionScreen.kt` | Branded permission recovery |
| `SystemBarAppearance.kt` | Per-screen status/nav icon appearance |

### Components (`ui/component/`)

`HastaIcon`, `BrandLockup`, `HastaButton`, `HastaCard`, `PracticeProgress`,
`HastaTopBar`, `TrackingStatusPill`, `HandVisibilityRow`, `FramingGuide`,
`LessonCard`, `CameraCoachPanel`. `TrackingStatusChip` was removed with `CameraUiState`.

### Camera host

- `CameraController` now exposes two streams: `frameState` (high frequency, overlay
  only) and `tracking` (low frequency, panel). Added `restart()`.
- `CameraScreen` became a permission host plus a camera host. The practice timer is
  `rememberSaveable`, so rotation preserves the window.
- `HandOverlay` colours moved to theme tokens and its description to string
  resources. Transform and mirroring are untouched.

### Resources and packaging

- `strings.xml` rewritten for M0.5 copy, accessibility and preview content.
- `values/themes.xml` + `values-night/themes.xml` window theming.
- Adaptive launcher icon (Raspberry background, Pale Sky hand mark), plus
  `dataExtractionRules` and `fullBackupContent="false"`.
- Version `0.2.0-m0.5`, `versionCode 2`.

### Review follow-up

A fresh-context review of the change set found two lifecycle defects, both fixed:

- `CameraController` now ignores callbacks from a superseded session. `restart()`
  increments a generation counter before tearing the old session down, so a late
  MediaPipe result or error cannot overwrite the state of the replacement session.
- The practice window now advances only while the screen is `STARTED`. Backgrounding
  the app freezes the remaining time instead of completing a window during which no
  frames were analysed, and a camera or tracker failure stops the window rather than
  letting the timer outlive the frames. The completion copy no longer claims the
  hands stayed in view.

Also corrected from the review: `FrameUiState` is `@Stable` rather than `@Immutable`
(it exposes a `List`), buttons keep their visible label as the accessible name, the
camera retry label is explicitly "Try camera again", spacers use `height`, preview
colours come from the dark token set, and the rotation note in the visual spec now
states that the activity is recreated while the saved practice window survives.

### Unchanged

`domain/` and `data/` implementation, the MediaPipe model and confidence settings, GPU
fallback, smoothing, exposure, benchmark implementation and all M0 tests.

`perception/` was changed by one defect fix: the analysis frame is now rotated in the
app before inference (ADR-0002). No other perception behaviour changed.

---

## 3. Verification performed

All commands were run from `android/` with
`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`.

| Command | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest` | **BUILD SUCCESSFUL** — 111 tests, 0 failures, 0 errors, 0 skipped |
| `./gradlew :app:assembleDebug` | **BUILD SUCCESSFUL** — 0 compiler errors, 0 warnings |
| `./gradlew :app:assembleRelease` | **BUILD SUCCESSFUL** — unsigned APK produced |
| `./gradlew :app:assembleDebugAndroidTest` | **BUILD SUCCESSFUL** — 5 instrumented test classes compile |
| `./gradlew :app:lintDebug` | **BUILD SUCCESSFUL** — 0 errors, 11 warnings (all dependency/target-SDK advisories) |
| `./gradlew :app:connectedDebugAndroidTest` | **BUILD SUCCESSFUL** — 30 tests executed on the moto g64 5G, 0 failed |

Unit test coverage added:

- `ThemeTokensTest` — locked hex values, dark/light separation, the four documented
  contrast pairings, AA checks, status tone mapping, "never white on Pale Sky".
- `TypographyTest` — body sizing, minimum sizes, line-height sanity, scale hierarchy.
- `MotionTest` — duration ranges, reduced-motion collapse, animator-scale detection.
- `LiveCoachPresenterTest` — every live state, error precedence, practice progress,
  and a guard that no future recognition state is ever emitted.
- `PracticeStateTest` — window start/advance/stop/again semantics and clamping.
- `HandResultMapperTest` — the mapper must not rotate or swap the upright image it is
  given, at any of 0/90/180/270 (regression test for the 180-degree overlay defect).
- `FrameRotationTest` — degree normalisation, including negative and multi-turn input.

Instrumented tests, executed on the device:

- `LiveCoachStateRenderingTest` — all live states, debug-metric absence.
- `LiveCoachRotationTest` — portrait margins, landscape right-side top-anchored
  placement, runtime rotation; viewport forced with `DeviceConfigurationOverride`.
- `LiveCoachAccessibilityTest` — 48dp target, `stateDescription`, content
  descriptions, font scale 2.0, reduced motion, error semantics.
- `PermissionScreenTest` — rationale, denial, permanent denial, dark theme, clicks.
- `FrameRotationInstrumentedTest` — colour-quadrant proof that the frame rotates
  clockwise, that 90 degrees swaps non-square dimensions, that 180 flips both axes,
  and that -90 equals 270.

### Lint warnings accepted

The 11 remaining warnings are informational: available dependency/AGP/Gradle
upgrades (upgrading is outside M0.5 and needs approval), `targetSdk 36` advisory, and
the `mipmap-anydpi-v26` qualifier note. No warning reports a correctness, security or
accessibility defect.

---

## 4. Device verification

Phone: moto g64 5G (`ZA222MM3L5`), Android 15, 1080 x 2400 physical, density
override 492 dpi. The instrumented suite and every observation below were made on
that device; the display was locked with `adb shell cmd window user-rotation lock`.

| Check | Evidence | Result |
|---|---|---|
| Instrumented suite on hardware | `:app:connectedDebugAndroidTest` — 30 tests, 0 failed | **PASS** |
| Camera start and permission screen | Fresh install with camera revoked showed the branded rationale; granting started the preview | **PASS** |
| Live Coach portrait layout | Bottom panel inside the safe area, full primary action visible | **PASS** |
| Live Coach landscape layout | Right-edge panel, top-anchored below the top bar, full primary action visible | **PASS** |
| Skeleton alignment, portrait | Single skeleton overlays the real right hand: wrist at the bracelet, fingertips and thumb on the actual fingers | **PASS** |
| Skeleton alignment, landscape | Both skeletons overlay the corresponding real hands | **PASS** |
| Mirroring and handedness | Display-only mirror; the subject's right hand is drawn in Raspberry | **PASS** |
| Rotation mid-window | Practice window survived a portrait-to-landscape rotation and completed honestly | **PASS** |
| Overlay rotation across orientation | Analysis metadata `rot=270` upright `480x640` in portrait and `rot=180` upright `640x480` in landscape; landmarks followed the hand in both | **PASS** |

### 4.1 Overlay rotation defect found on device, and fixed

The first device pass exposed a real defect: the skeleton was rotated 180 degrees
against the hand. A debug probe measured the cause. With rotation metadata `180`, a
right hand held fingers-up returned `wrist=(0.385,0.300)` and
`midtip=(0.401,0.749)` — the fingertip below the wrist. The MediaPipe Hand
Landmarker was not applying `ImageProcessingOptions.setRotationDegrees`, so the
landmarks stayed in the raw sensor frame while the overlay assumed an upright one.

The fix rotates the analysis bitmap in the app before inference and passes no
rotation to the task; see
[ADR-0002](../decisions/ADR-0002-rotate-analysis-frame-in-app.md). This is a change
to `perception/`, which the earlier draft of this report described as untouched.

### 4.2 Ten-minute benchmark

Run `benchmark-20261031-091213`, started 09:12:12 and completed 09:22:13 IST on the
same phone, using the debug build that passed the checks above.
`endReason` is `duration_complete`; duration 601.0 s.

| Metric | Value |
|---|---|
| `endReason` | `duration_complete` |
| Duration | 601.0 s |
| Submitted / processed / skipped / errors | 17,613 / 4,688 / 12,926 / 0 |
| Recorded frames | 4,680 |
| Effective FPS | 7.80 |
| Frame interval p50 / p95 / p99 | 128 / 177 / 205 ms (min 50, max 339, mean 128.2) |
| Latency p50 / p95 / p99 | 226 / 277 / 291 ms (min 152, max 342, mean 229.8) |
| PSS start / peak / end | 258,819 / 349,262 / 294,358 KB (578 samples) |
| Battery / charging | 62% → 60% / charging throughout |
| Battery temperature | 34 → 36 °C, peak 36 °C |
| Thermal | none at start, end and peak |
| Analysis resolution | 640×480 (1.3333) |
| Preview resolution | 1440×1088 (1.3235) |
| Target rotation | 0° (portrait); analysis rotation 270° on every frame |
| Delegate | `GPU` |
| Smoothing / exposure | `one_euro` / `adaptive_luma` |

**Comparison with M0.** The M0 baseline (`benchmark-20261030-230602`) processed
8.39 FPS; this run processed 7.80 FPS, about 7% fewer frames per second. The two
runs are **not like-for-like**, and the difference is not reported as a regression
verdict:

- The M0 build did no in-app rotation at all; this run rotated every analysis frame
  by 270° (portrait), which is exactly the work ADR-0002 added. The baseline also
  ran at target rotation 90° (landscape), so the device orientation differed too.
- The runs offered different frame counts (14,925 vs 17,613 submitted, 18% more
  here), so this run's stream was not starving the analyzer.
- Each condition was measured once; run-to-run variance is unknown.

What the numbers support: the pipeline stayed healthy for the full ten minutes
(0 errors, no thermal throttling, memory returned below its peak), and the added
rotation did not degrade throughput by a large factor. What they do not support is
the claim that there is **no** performance regression. A like-for-like repeat
(baseline orientation, more than one run each) is the outstanding follow-up. One
landscape attempt was started at 09:15 on the same build, but the phone disconnected
before the run could export, so it produced no result and is not reported as one.

### 4.3 Still not verified

- TalkBack read-through of the live camera screen (labels and semantics are asserted
  by tests, not by a screen-reader pass).
- Behaviour beyond a ten-minute session (multi-hour battery, thermal and memory).
- A like-for-like performance comparison against the M0 baseline (section 4.2).

---

## 5. Acceptance against the blueprint

| Requirement | Status |
|---|---|
| Exact brand colours with semantic roles | Met — asserted by unit tests |
| Documented dark palette, not inverted | Met — `HastaDarkColors` + tests |
| Visual spec: type, spacing, radius, motion, six components | Met — `docs/spec/visual-spec.md` |
| Ready, tracking, no-hand, permission, error states | Met — presenter + instrumented tests |
| No debug clutter on the learner screen | Met — debug metrics never rendered; test guards it |
| Accessibility pass | Met in code and tests; device TalkBack pass NOT VERIFIED |
| Rotation stays clean | Met on device in portrait and landscape, including a mid-window rotation |
| M0 engine preserved | Met apart from the documented ADR-0002 rotation fix; `domain/` and `data/` untouched |
| No performance regression | Measured 7.80 FPS vs the 8.39 baseline, but in a different orientation; no clean verdict (section 4.2) |

---

## 6. Risks and follow-ups

- The instrumented suite now runs on hardware (30/30). Layout assertions use a forced
  viewport, so they are device-size independent, but they do not replace a visual
  check — the clamped landscape panel was found visually.
- The practice window is intentionally UI-only. Later milestones must replace it
  rather than extend it into an assessment.
- The launcher icon is a first mark, not a final brand asset.
- Dependency upgrades and `targetSdk` changes are deliberately deferred.
- Overlay gates must record landmark coordinates, not just a screenshot: the M0
  "aligned" verdict was visually wrong for a whole milestone.
- Performance should be measured in the same orientation for both builds before any
  regression claim. The rotation fix does per-frame work that the M0 benchmark did
  not, so the comparison in section 4.2 is indicative only.

---

## 7. Reproduce

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:testDebugUnitTest
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:assembleDebug
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:assembleRelease
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:lintDebug
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:assembleDebugAndroidTest
```

With a device attached:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:connectedDebugAndroidTest
```

Benchmark (debug build only, long-press the status pill to start or stop):

```bash
adb shell cmd window user-rotation lock 0     # record the orientation you use
adb shell am start -n com.hasta.app/.MainActivity
adb shell input motionevent DOWN 810 220 && sleep 2 && adb shell input motionevent UP 810 220
adb exec-out run-as com.hasta.app cat files/benchmark/<run>.json > run.json
```
