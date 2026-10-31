# M0.5 — Brand Theme and Live Sign Coach Camera Shell

Status: **build and automated verification complete; physical-device verification
NOT VERIFIED.**

Date: 31 October 2026.

---

## 1. Scope

M0.5 adds the HASTA design system and the Live Sign Coach camera shell on top of the
unchanged M0 tracking engine.

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

### Unchanged

`perception/`, `domain/`, `data/` implementation, the MediaPipe configuration, GPU
fallback, smoothing, exposure, benchmark implementation and all M0 tests.

---

## 3. Verification performed

All commands were run from `android/` with
`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`.

| Command | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest` | **BUILD SUCCESSFUL** — 108 tests, 0 failures, 0 errors, 0 skipped |
| `./gradlew :app:assembleDebug` | **BUILD SUCCESSFUL** — 0 compiler errors, 0 warnings |
| `./gradlew :app:assembleRelease` | **BUILD SUCCESSFUL** — unsigned APK produced |
| `./gradlew :app:assembleDebugAndroidTest` | **BUILD SUCCESSFUL** — 4 instrumented test classes compile |
| `./gradlew :app:lintDebug` | **BUILD SUCCESSFUL** — 0 errors, 11 warnings (all dependency/target-SDK advisories) |

Unit test coverage added:

- `ThemeTokensTest` — locked hex values, dark/light separation, the four documented
  contrast pairings, AA checks, status tone mapping, "never white on Pale Sky".
- `TypographyTest` — body sizing, minimum sizes, line-height sanity, scale hierarchy.
- `MotionTest` — duration ranges, reduced-motion collapse, animator-scale detection.
- `LiveCoachPresenterTest` — every live state, error precedence, practice progress,
  and a guard that no future recognition state is ever emitted.
- `PracticeStateTest` — window start/advance/stop/again semantics and clamping.

Instrumented tests written and compiling (not executed — see section 4):

- `LiveCoachStateRenderingTest` — all live states, debug-metric absence.
- `LiveCoachRotationTest` — portrait margins, landscape right-side placement,
  runtime rotation.
- `LiveCoachAccessibilityTest` — 48dp target, `stateDescription`, content
  descriptions, font scale 2.0, reduced motion, error semantics.
- `PermissionScreenTest` — rationale, denial, permanent denial, dark theme, clicks.

### Lint warnings accepted

The 11 remaining warnings are informational: available dependency/AGP/Gradle
upgrades (upgrading is outside M0.5 and needs approval), `targetSdk 36` advisory, and
the `mipmap-anydpi-v26` qualifier note. No warning reports a correctness, security or
accessibility defect.

---

## 4. Not verified

The phone (moto g64 5G, `ZA222MM3L5`, Android 15) was disconnected to free RAM
before device work. Therefore the following are **NOT VERIFIED**:

- `connectedDebugAndroidTest` execution on hardware.
- On-device camera start, preview orientation and overlay alignment.
- Mirroring correctness in the live app after the theme change.
- Rotation behaviour on the actual device across all Live Coach states.
- Permission grant, denial, permanent denial and Settings round-trip.
- Background/foreground and camera-resume behaviour.
- Performance regression against the M0 baseline (8.39 FPS).
- Battery, thermal and memory behaviour with the new UI.

No device, FPS, latency, memory, battery or thermal figure is claimed in this report.

### Device protocol when the phone is reconnected

1. `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`.
2. `./gradlew :app:connectedDebugAndroidTest`.
3. Walk every Live Coach state: starting, ready, no hands, one hand, two hands,
   practising, hands lost mid-window, window complete, tracking paused, camera
   unavailable.
4. Rotate mid-window in each of ready, practising and complete; confirm the timer
   continues and the panel moves to the right edge in landscape.
5. Confirm the skeleton matches the hand, that the left/right colours are correct
   and that mirroring is display-only.
6. Revoke the camera permission, deny once, deny permanently, then grant through
   Settings; confirm each recovery path.
7. Background and resume; confirm the preview and tracking recover.
8. Run the ten-minute debug benchmark and compare against the M0 baseline using the
   same analysis resolution and delegate.

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
| Rotation stays clean | Met in layout and `LiveCoachRotationTest`; device NOT VERIFIED |
| M0 engine preserved | Met — `perception/`, `domain/`, `data/` untouched |
| No performance regression | NOT VERIFIED — requires device benchmark |

---

## 6. Risks and follow-ups

- The instrumented suite has never run on hardware. Layout assertions may need small
  tolerance adjustments on the real screen size.
- The practice window is intentionally UI-only. Later milestones must replace it
  rather than extend it into an assessment.
- The launcher icon is a first mark, not a final brand asset.
- Dependency upgrades and `targetSdk` changes are deliberately deferred.

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
