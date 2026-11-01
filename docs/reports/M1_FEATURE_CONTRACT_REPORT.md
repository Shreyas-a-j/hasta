# M1 — Feature Contract and Data Tooling Report

- Date: 31 October 2026
- Branch: `m1/feature-contract`
- Feature contract: `hasta.features` / `features-v1`
- Contract SHA-256: `c7c72083caf701917bebe3b2ec07f4740402509eae90dbc7883ae6cc04546f04`
- Python: 3.12.15, uv 0.13.3
- JDK: 17
- Device: none used for M1

## Scope delivered

- Locked 256-channel `features-v1` contract with JSON Schema.
- Python reference implementation of coordinates, slots, local, global, temporal
  and inter-hand groups.
- Deterministic golden vectors in canonical JSON and generated Kotlin source.
- Kotlin `Double` implementation with JVM parity tests.
- Research data format `landmarks-v1`, manifest `research-manifest-v1`, audit and
  scoped, logged deletion.
- Webcam recorder and dataset video extractor, both writing landmarks only.
- CI workflow and local verification script, with parity first.

Explicitly **not** delivered: recognition, LiteRT, GRU/CNN, vocabulary, lessons,
Room, DataStore, authentication, cloud services, analytics, pose, face, M2 live
feature wiring.

## Verification evidence

### Python

```bash
cd research
uv sync --frozen --extra test
uv run --frozen pytest
```

Result: **123 passed** (0 failed, 0 skipped). The suite covers:

- contract layout, constants, schema and canonical hash;
- coordinate aspect correction and mirroring;
- slot assignment, continuity and ambiguity;
- every local, global, temporal and inter-hand channel;
- gaps, non-monotonic timestamps, missing hands and invalid landmarks;
- golden-vector replay for all 20 cases, including degenerate, very small and
  very large palm scale;
- sequence round-trip and missingness;
- manifest schema, provenance and contract binding;
- audit of missing, tampered and metadata-drifted files;
- dry-run and live deletion with audit logging;
- capture metadata, consent and licence gates.

### Kotlin parity

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:testDebugUnitTest \
  --tests 'com.hasta.app.domain.features.*'
```

Result: **4 tests, 0 failures**. `FeatureParityTest` replays all 20 golden cases
against the Kotlin implementation. Non-mask channels use `atol = 1e-6`,
`rtol = 1e-5`; masks compare exactly.

### Android suite and build

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:testDebugUnitTest
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:assembleDebug
```

Result: **125 tests, 0 failures, 0 skipped**; `:app:assembleDebug` **BUILD
SUCCESSFUL**. APK: `android/app/build/outputs/apk/debug/app-debug.apk` (90 MB).

### End-to-end verification script

```bash
bash scripts/verify.sh
```

Result: **`verify: PASS`**. The script runs Python tests, regenerates the contract
and golden vectors, checks committed artifact drift, runs Kotlin parity first, runs
the full Android suite and assembles the debug APK.

## Gate results

| Gate | Result | Evidence |
|---|---|---|
| G1 contract and schema | PASS | `contracts/feature_contract.json`, schema, 11 contract tests |
| G2 Python reference | PASS | 123 Python tests |
| G3 deterministic golden vectors | PASS | 20 cases (including extreme scale), canonical JSON, generated Kotlin source, replay tests |
| G4 Kotlin parity | PASS | 4 feature tests, 0 failures |
| G5 Android regression and build | PASS | 125 tests, debug APK |
| G6 artifact regeneration | PASS | `scripts/verify.sh` drift check |
| G7 manifest, audit, deletion | PASS | synthetic sequence/manifest tests |
| G8 consent, licence, missingness | PASS | sequence/capture tests reject violations |
| G9 CI configuration | PASS | remote run [41274799003](https://github.com/Shreyas-a-j/hasta/actions/runs/41274799003) on `2d99cd1` succeeded end-to-end (`verify: PASS`); run #1 exposed and the v4 action upgrade fixed the removed `tools` package failure |
| G10 webcam recorder on hardware | PASS | `/dev/video0`, 407 frames / 14,042 ms, 0 dropped, 401 frames with hands, 802 hand observations, manifest + audit clean, reference run 407×256 |
| G11 dataset extraction on real data | PASS | INCLUDE (Zenodo 4010759, CC BY 4.0), one clip → 56-frame sequence with full provenance; audit 3 entries, 0 errors, 0 warnings; zip and raw video deleted after extraction |
| G12 on-device real-landmark capture | PASS | debug-only JSONL dump from the phone: 448 frames, 284 hand observations, 21 landmarks per hand; reference run 448×256, hand-less frames zero+mask |
| G13 scope exclusions | PASS | static inspection: no recognition, LiteRT, Room, cloud or new module |
| G14 live app feature wiring | DEFERRED | M2 scope by design; the G12 capture is debug-only (`LandmarkDumpFactory` returns null in release) and wires no features |

`G9` is **PASS**: the workflow executes remotely on GitHub Actions and the full
verification pipeline completed successfully.

Remote CI evidence (2026-11-01):

- Green run: <https://github.com/Shreyas-a-j/hasta/actions/runs/41274799003>
  (`verify` on `2d99cd1`, push to `main`), job conclusion **success** in 3 m 41 s;
  the `Verify` step log records `123 passed` and the final `verify: PASS`.
  The remote pipeline is identical to `scripts/verify.sh`: Python suite,
  regeneration drift check, Kotlin parity first, full Android suite, debug APK.
- Run #1 (<https://github.com/Shreyas-a-j/hasta/actions/runs/41270050060>, on
  `a8090d6`) failed before the verification script:
  `android-actions/setup-android@v3` ran `sdkmanager tools` → `Warning: Failed
  to find package 'tools'` → exit 1. Google removed the legacy `tools` package
  from modern cmdline-tools, so v3's default package install can no longer
  succeed. Fixed by upgrading to `android-actions/setup-android@v4` (v4.0.2
  release note: "Fix for removed tools package"), plus `actions/setup-java@v5`
  and `actions/checkout@v5` per the run's own deprecation annotations.
  Commit `e3ca233`.

`G14` is **DEFERRED**, not FAILED: M1 deliberately does not wire the extractor into
the live pipeline. `G12` proves real device landmarks reach the landmarks-v1
contract through a debug-only dump; no release behaviour changed.

Hardware and data evidence (2026-11-01): the webcam runs exercised both the
missingness path (51 hand-less frames) and real motion (3,198 nonzero temporal
values); the INCLUDE clip produced 74 hand observations from third-party footage;
the phone capture produced 2228 nonzero temporal values with absent frames
exactly zero+mask.

## Contract summary

| Offset | Length | Group |
|---:|---:|---|
| 0 | 16 | header |
| 16 | 42 | validity |
| 58 | 81 | left.local |
| 139 | 7 | left.global |
| 146 | 8 | left.temporal |
| 154 | 81 | right.local |
| 235 | 7 | right.global |
| 242 | 8 | right.temporal |
| 250 | 6 | inter_hand |

Key rules:

- isotropic image space `u = x`, `v = y * height / width`;
- palm scale `0.5 * (||p5 - p0|| + ||p17 - p0||)`;
- subject-relative handedness, no inference mirroring;
- explicit zero-plus-mask missingness, no imputation;
- causal forward differences over actual `dt`, 600 ms gap threshold;
- `float64` computation and golden storage;
- masks exact, other channels `atol = 1e-6`, `rtol = 1e-5`.

## Known limitations

- The 600 ms gap threshold is a proposed constant, not a measured optimum.
- Only handedness confidence is available; there is no per-hand tracking
  confidence, so the contract does not invent one.
- Hardware evidence comes from one webcam, one phone and one dataset clip on a
  single developer machine. No device fleet was tested; results beyond this
  setup are NOT VERIFIED.
- The on-device dump (G12) covers one pipeline session and is capped at 900
  frames per file; it is a debug tool, not a data-collection product.
- `features-v1` is locked. Any formula, order or constant change requires
  `features-v2`, new golden vectors and an ADR.
- The capture extra pulls MediaPipe and OpenCV; it is optional and not needed for
  tests or the Android build.

## Next milestone boundary

M2 may wire the Kotlin extractor to the live M0 pipeline, but only after deciding
how per-landmark validity is represented by the tracker. M1 does not change M0
tracking, smoothing, overlay or UI behaviour.
