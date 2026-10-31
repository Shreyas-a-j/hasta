# ADR-0003 — Versioned `features-v1` Contract and Dual-Language Parity

- Status: Accepted
- Date: 31 October 2026
- Milestone: M1
- Supersedes: nothing

## Context

M0 proved hands-only tracking on the moto g64 5G but produced no stable numeric
representation of a hand. M1 must define the data that future recognition work will
consume, before any model exists. The representation has to be:

- small enough to run on the M0 device class, which measured 8.39 FPS in M0;
- explicit about missing hands and missing landmarks;
- identical in the Python research pipeline and the Kotlin app;
- independent of MediaPipe types outside `perception/`;
- versioned, because changing channel order or formulas after data collection would
  silently invalidate every dataset and trained model.

The M1 scope excludes recognition, LiteRT, GRU/CNN, vocabulary, lessons, Room,
DataStore, cloud, pose and face. This ADR only fixes the feature contract and its
verification.

## Decision

1. **One locked contract, `features-v1`, of exactly 256 `float64` channels.**
   The layout is: header 16, validity 42, left local 81, left global 7, left
   temporal 8, right local 81, right global 7, right temporal 8, inter-hand 6.
   The normative definition is `docs/spec/features-v1.md`; the machine-readable
   form is `contracts/feature_contract.json` plus its JSON Schema.

2. **Isotropic image space.** Features use `u = x`, `v = y * (height / width)` in
   image-width fractions. This removes pixel aspect ratio while keeping image
   position, which is useful for signing space. Depth `z` is retained in recordings
   but unused by v1.

3. **2D local geometry, palm-normalized.** Wrist-relative coordinates, joint
   angles, bone lengths, fingertip spreads, extension ratios, thumb–index
   opposition and palm aspect are normalized by
   `s = 0.5 * (||p5 - p0|| + ||p17 - p0||)`. World landmarks are not used.

4. **Explicit missingness.** Absent hands and invalid landmarks produce zero
   geometry **and** zero masks or group-validity flags. No imputation, no
   interpolation, no zero-as-valid. `NaN` and infinity are rejected.

5. **Subject-relative handedness and display-only mirroring.** Frames are never
   mirrored before inference. A dataset explicitly marked `mirrored_input` is
   un-mirrored by `x := 1 - x` and label swap at the feature boundary. Slots are
   `left` then `right`, assigned by distinct labels, then continuity, then input
   order, with an ambiguity flag.

6. **Causal temporal features.** Forward differences over actual `dt`, no
   resampling, a 600 ms gap threshold, and state reset on gaps, non-monotonic
   timestamps or absence. No future frames.

7. **Golden vectors and dual-language parity.** The Python implementation generates
   deterministic golden vectors in canonical JSON and a generated Kotlin test
   source. Kotlin parity tests compare all non-mask channels at
   `atol = 1e-6`, `rtol = 1e-5`, and all mask channels exactly. Parity runs first
   in CI.

8. **Immutability.** Any change to channels, formulas, units, constants,
   missingness, temporal or slot semantics requires `features-v2`, new artifacts
   and an ADR. In-place edits are forbidden.

## Alternatives considered

- **Normalized `x/y` directly (non-isotropic).** Rejected: horizontal and vertical
  distances would have different physical scales, corrupting angles, distances and
  motion.
- **World landmarks in metres.** Rejected for v1: they add scale-invariance but
  change the coordinate space and are less stable at the M0 frame rate. They remain
  available in recorded data for a future version.
- **3D local geometry including `z`.** Deferred: MediaPipe image `z` is
  wrist-relative and less reliable than `x/y`; including it now would make the
  contract harder to validate on device.
- **Per-hand tracking confidence.** Not available from the M0 mapper. The contract
  records only handedness confidence and marks it as such; no confidence is
  invented.
- **Smoothing inside features.** Rejected: M0 One Euro smoothing is display-only.
  Features consume raw mapped landmarks so training and evaluation see the same
  signal.
- **Resampling sequences to a fixed rate.** Rejected for v1: it hides real timing
  and creates interpolation choices before the data pipeline is validated.
- **A Kotlin JSON parser for golden vectors.** Rejected: it adds a dependency and
  a second parser to verify. Generating Kotlin source keeps the test hermetic.
- **Editing the contract in place after data collection.** Rejected: it would make
  old and new data indistinguishable.

## Consequences

- The Python and Kotlin implementations must be kept in lockstep; parity tests are
  the guard.
- Data collected under v1 remains usable only with v1 features. A v2 migration
  needs a documented mapping or re-extraction.
- `header` and `validity` channels make the vector partly redundant, but they let a
  consumer distinguish "zero because absent" from "zero because the hand is at the
  origin". The cost is 58 of 256 channels.
- The contract is larger than a minimal handshape descriptor. The cost is accepted
  for image position and inter-hand geometry, which signing depends on.
- Device storage may use `float32` later, but only after parity is re-validated at
  the model boundary.
- The 600 ms gap threshold is a proposed constant, not a measured optimum. It is
  isolated so it can be revised in a future version with evidence.

## Verification

- `research/tests/test_contract.py`: layout continuity, unique names, constants,
  generated JSON equality, JSON Schema acceptance/rejection, semantic validation,
  canonical hash stability.
- Python unit tests for coordinates, slots, local, global, temporal and inter-hand
  behaviour, including missingness and gaps.
- Generated golden vectors compared against the Python reference.
- Kotlin JVM parity tests against the generated Kotlin golden source.
- CI runs parity before the remaining checks.
- Physical-device and dataset gates are reported as **NOT VERIFIED** unless a real
  device, webcam or dataset was used.
