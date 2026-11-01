# features-v1 — Normative Feature Contract

- Status: Locked
- Contract name: `hasta.features`
- Version: `features-v1`
- Machine-readable contract: `contracts/feature_contract.json`
- Schema: `contracts/feature_contract.schema.json`
- Python reference: `research/features/contract.py`
- Decision record: `docs/decisions/ADR-0003-features-v1-contract.md`

This document is normative. If the implementation and this document disagree, the
implementation is wrong. Changing any channel name, channel order, formula, unit or
constant in this document is an incompatible change and requires a new version
(`features-v2`), new golden vectors and new parity tests.

---

## 1. Scope

`features-v1` converts one camera frame of MediaPipe hand landmarks into exactly
**256 `float64` channels** per frame, for both hands, with explicit missingness.

In scope:

- two subject-relative hand slots (`left`, `right`);
- image-space handshape geometry, position, orientation and palm scale;
- causal wrist motion and palm angular velocity;
- inter-hand relative geometry;
- explicit validity masks for missing hands and missing landmarks.

Out of scope for v1:

- recognition, classification, training, vocabulary or lessons;
- LiteRT, GRU, CNN or any model runtime;
- pose, face or body landmarks;
- world landmarks (MediaPipe's metric hand landmarks);
- depth (`z`) features;
- smoothing, filtering or interpolation;
- resampling to a fixed frame rate;
- temporal context longer than one previous frame.

The raw image landmark `z` is retained in recorded data for provenance but is **not**
consumed by v1 features.

---

## 2. Input

### 2.1 Frame observation

A frame observation is:

| Field | Type | Meaning |
|---|---|---|
| `timestamp_ms` | integer | Monotonic capture timestamp in milliseconds |
| `width` | integer > 0 | Width of the upright analysis image |
| `height` | integer > 0 | Height of the upright analysis image |
| `mirrored_input` | boolean | The landmarks already come from a mirrored image |
| `hands` | list, length 0–2 | Hand observations in detector output order |

### 2.2 Hand observation

Each hand observation has:

| Field | Type | Meaning |
|---|---|---|
| `handedness` | `"left"`, `"right"` or `null` | Subject-relative MediaPipe label |
| `handedness_score` | number or `null` | MediaPipe handedness confidence |
| `landmarks` | 21 × `(x, y, z)` or `null` | Image-normalized landmarks, upright image |
| `landmark_validity` | 21 booleans or `null` | Per-landmark validity; `null` means all true |

`landmarks` is `null` when the detector reported no usable landmarks for that hand.
Individual landmarks may be absent only through `landmark_validity`; a landmark is
never encoded as `(0, 0, 0)` to mean missing.

`x` and `y` are normalized against the **upright analysis image**, so
`x ∈ [0, 1]` across image width and `y ∈ [0, 1]` across image height. The analysis
image has already been rotated upright by `FrameRotation` (ADR-0002). `z` is
wrist-relative and is ignored by v1.

### 2.3 Rejected input

The implementation must reject, not repair, any of the following:

- `width <= 0` or `height <= 0`;
- a non-finite landmark coordinate (`NaN`, `+Inf`, `-Inf`);
- more than two hands;
- a landmark list whose length is not 21;
- a validity list whose length is not 21;
- a non-monotonic frame stream that cannot be interpreted under §6.4.

Rejection is a raised error in Python and an exception or explicit failure result in
Kotlin. Silent clamping, zero-filling or skipping is forbidden.

---

## 3. Coordinate space

All geometry is computed in an **isotropic, upright, unmirrored** feature space
measured in image-width fractions:

```
u = x
v = y * (height / width)
```

This removes the non-square pixel aspect ratio. `u` and `v` therefore have the same
physical scale, and Euclidean distances are meaningful. No other transform is
applied: no clipping, no centring, no scaling to `[0, 1]` in both axes.

`v` may exceed 1 for a landscape image; that is valid and must not be clamped.

### 3.1 Mirroring

Camera frames are never mirrored before inference (ADR-0001). If a dataset or
recording is explicitly marked `mirrored_input = true`:

1. every landmark becomes `x := 1 - x` before any feature computation;
2. the handedness labels are swapped (`left ↔ right`) before slot assignment.

`mirrored_input = false` applies neither step. The display mirror is never part of
feature computation.

### 3.2 Rotation

Landmarks must already be upright. `rotation_degrees` is provenance only and must
not be applied again. Applying a rotation inside feature extraction is a bug.

---

## 4. Hand slots and assignment

Slots are fixed in this order: `left`, then `right`. Slot identity is
**subject-relative handedness**, not viewer-relative position.

Assignment rules, in order:

1. **Distinct known labels.** If the frame has exactly one `left` and one `right`
   observation, assign each to its named slot. Not ambiguous.
2. **Continuity.** Otherwise, if the previous frame assigned hands to slots, choose
   the assignment that minimises the sum of squared wrist distances in feature
   space between the current hands and the previous frame's slot wrists. With two
   hands, evaluate both pairings. If one slot was empty previously, assign the
   single hand to the nearer previously occupied slot only when the other slot is
   empty; otherwise continue to rule 3.
3. **Input order.** Fill remaining slots in detector output order: first hand to
   `left`, second to `right`.
4. **Single unknown hand.** Use continuity if exactly one slot was occupied on the
   previous frame; otherwise assign to `left`.

The frame flag `header.slot_assignment_ambiguous` is `1` when the frame's known
labels do not form exactly one `left` and one `right`, including two hands with the
same label, any unknown label, or a single hand with no continuity. It is `0` when
no hands are present.

`header.left_handedness_known` / `header.right_handedness_known` are `1` when the
observation assigned to that slot carries a non-null handedness label, else `0`.

Continuity state is cleared on a temporal gap (§6.4) and when a slot is absent.

---

## 5. Vector layout

The vector is the concatenation of the groups below, in this exact order. Offsets
are channel indices. Channel names are listed in `contracts/feature_contract.json`
and must not be renamed within v1.

| Offset | Length | Group |
|---:|---:|---|
| 0 | 16 | `header` |
| 16 | 42 | `validity` |
| 58 | 81 | `left.local` |
| 139 | 7 | `left.global` |
| 146 | 8 | `left.temporal` |
| 154 | 81 | `right.local` |
| 235 | 7 | `right.global` |
| 242 | 8 | `right.temporal` |
| 250 | 6 | `inter_hand` |
| **256** | | **total** |

### 5.1 Header (16)

| Index | Channel | Meaning |
|---:|---|---|
| 0 | `header.dt_s` | Seconds since previous accepted frame; `0` on first frame or gap |
| 1 | `header.elapsed_s` | Seconds since current temporal segment start; `0` at segment start |
| 2 | `header.frame_gap` | `1` if this frame starts a new segment because of a gap |
| 3 | `header.first_frame` | `1` only for the first frame of the stream |
| 4 | `header.left_present` | `1` if a hand is assigned to `left` |
| 5 | `header.right_present` | `1` if a hand is assigned to `right` |
| 6 | `header.both_present` | `1` iff both slots are present |
| 7 | `header.slot_assignment_ambiguous` | See §4 |
| 8 | `header.left_handedness_known` | See §4 |
| 9 | `header.right_handedness_known` | See §4 |
| 10 | `header.left_local_valid` | `1` iff the left local group is defined |
| 11 | `header.right_local_valid` | `1` iff the right local group is defined |
| 12 | `header.left_global_valid` | `1` iff the left global group is defined |
| 13 | `header.right_global_valid` | `1` iff the right global group is defined |
| 14 | `header.left_temporal_valid` | `1` iff the left temporal group is defined |
| 15 | `header.right_temporal_valid` | `1` iff the right temporal group is defined |

### 5.2 Validity (42)

`validity.left.00` … `validity.left.20`, then `validity.right.00` …
`validity.right.20`. `1` means the input landmark is valid, `0` means it is
missing or the hand is absent. These channels copy input validity; they are never
inferred from geometry.

### 5.3 Per-slot local (81)

Channels appear in this order for each slot:

1. wrist-relative coordinates, 40 channels;
2. joint angles, 15 channels;
3. bone lengths, 15 channels;
4. fingertip spreads, 4 channels;
5. finger extensions, 5 channels;
6. thumb–index opposition, 1 channel;
7. palm aspect, 1 channel.

Let `p_i = (u_i, v_i)`, with `i = 0` the wrist. Let the palm scale be

```
s = 0.5 * ( ||p5 - p0|| + ||p17 - p0|| )
```

in feature space. The local group is **defined** only if all 21 landmarks are
valid and `s >= 1e-6`. If it is not defined, all 81 channels are `0` and
`header.<slot>_local_valid = 0`. No imputation is performed.

When defined, `s` is used directly as the divisor; it is not clamped after the
definition check.

#### 5.3.1 Wrist-relative coordinates (40)

For `i = 1 … 20`, in index order, emit `u` then `v`:

```
u_i' = (u_i - u_0) / s
v_i' = (v_i - v_0) / s
```

Channel names: `<slot>.local.wrist_rel.<ii>.u` and `.v`, with `<ii>` zero-padded
to two digits.

#### 5.3.2 Joint angles (15)

Fingers and their landmark chains:

| Finger | Chain |
|---|---|
| thumb | `1, 2, 3, 4` |
| index | `5, 6, 7, 8` |
| middle | `9, 10, 11, 12` |
| ring | `13, 14, 15, 16` |
| pinky | `17, 18, 19, 20` |

For a chain starting at `b`, the three angles are:

```
j1 = angle(p0 - p_b,          p_{b+1} - p_b)
j2 = angle(p_b - p_{b+1},     p_{b+2} - p_{b+1})
j3 = angle(p_{b+1} - p_{b+2}, p_{b+3} - p_{b+2})
```

so each vector points away from the joint being measured. `j1` is the MCP angle
with the wrist as the incoming segment, `j2` the PIP angle and `j3` the DIP angle.
A straight finger therefore has angles near `π`. `angle(a, b)` is

```
angle(a, b) = atan2( |a.u * b.v - a.v * b.u|, a.u * b.u + a.v * b.v )
```

with range `[0, π]`. If either input vector has norm `< 1e-9`, the angle is `0`.
Emit in finger order `thumb, index, middle, ring, pinky`, and within a finger
`j1, j2, j3`.

#### 5.3.3 Bone lengths (15)

For each finger, in chain order, emit three normalized segment lengths:

```
bone_k = ||p_{b+k} - p_{b+k-1}|| / s,  k = 1, 2, 3
```

Channel names: `<slot>.local.bone.<finger>.<k>`.

#### 5.3.4 Fingertip spreads (4)

In this order, normalized by `s`:

| Channel | Distance |
|---|---|
| `<slot>.local.tip_spread.thumb_index` | `||p4 - p8|| / s` |
| `<slot>.local.tip_spread.index_middle` | `||p8 - p12|| / s` |
| `<slot>.local.tip_spread.middle_ring` | `||p12 - p16|| / s` |
| `<slot>.local.tip_spread.ring_pinky` | `||p16 - p20|| / s` |

#### 5.3.5 Finger extensions (5)

For each finger chain `b … b+3`:

```
extension = ||p_{b+3} - p_b|| / (bone_1 + bone_2 + bone_3)
```

If the denominator is `< 1e-9`, the value is `0`. Emit in finger order.

#### 5.3.6 Thumb–index opposition (1)

```
opposition = ||p4 - p8|| / s
```

#### 5.3.7 Palm aspect (1)

```
palm_aspect = ||p5 - p17|| / ||p0 - p9||
```

If `||p0 - p9|| < 1e-9`, the value is `0`.

### 5.4 Per-slot global (7)

Let the hand centre and orientation be

```
centre = mean(p0, p5, p9, p13, p17)
d      = p9 - p0
orientation = (cos θ, sin θ),  θ = atan2(d.v, d.u)
```

The global group is **defined** only if landmarks `0, 5, 9, 13, 17` are all valid.
If it is not defined, all 7 channels are `0` and
`header.<slot>_global_valid = 0`.

When defined, emit in order:

| Channel | Value |
|---|---|
| `<slot>.global.centre.u` | `centre.u` |
| `<slot>.global.centre.v` | `centre.v` |
| `<slot>.global.wrist.u` | `p0.u` |
| `<slot>.global.wrist.v` | `p0.v` |
| `<slot>.global.orientation.cos` | `cos θ` |
| `<slot>.global.orientation.sin` | `sin θ` |
| `<slot>.global.palm_scale` | `s` |

If `||d|| < 1e-9`, orientation is `(1, 0)`; the group remains defined because the
required landmarks are present. `palm_scale` uses the same `s` as §5.3; it is not
normalized by image size.

### 5.5 Per-slot temporal (8)

Temporal channels describe the wrist motion of the current frame relative to the
previous **usable** frame for that slot. Let

```
disp = p0_t - p0_prev
vel  = disp / dt_s
acc  = (vel_t - vel_prev) / dt_s
```

in feature space. Emit:

| Channel | Value |
|---|---|
| `<slot>.temporal.disp.u` | `disp.u` |
| `<slot>.temporal.disp.v` | `disp.v` |
| `<slot>.temporal.vel.u` | `vel.u` |
| `<slot>.temporal.vel.v` | `vel.v` |
| `<slot>.temporal.acc.u` | `acc.u` |
| `<slot>.temporal.acc.v` | `acc.v` |
| `<slot>.temporal.angular_velocity` | `wrap(θ_t - θ_prev) / dt_s` |
| `<slot>.temporal.has_previous` | `1` if a usable previous sample exists |

`wrap(x) = atan2(sin x, cos x)` gives a result in `[-π, π]`. Angular velocity is
`0` when either orientation is degenerate.

The temporal group is **defined** iff `has_previous = 1` and the current and
previous wrist landmarks are valid. Otherwise all 8 channels are `0`,
`has_previous = 0` and `header.<slot>_temporal_valid = 0`. If the current and
previous frames have a usable previous pair but the previous velocity is
unavailable (the previous frame itself had no previous), `acc = 0`.

A slot's temporal state is cleared when the hand is absent from that slot.

### 5.6 Inter-hand (6)

The inter-hand group is **defined** only when both slots are present and both
global groups are defined. Otherwise all 6 channels are `0`; there is no separate
header flag, so `header.both_present` plus the two global-validity flags carry the
missingness.

When defined, with `left` and `right` global values:

| Channel | Value |
|---|---|
| `inter_hand.rel_centre.u` | `right.centre.u - left.centre.u` |
| `inter_hand.rel_centre.v` | `right.centre.v - left.centre.v` |
| `inter_hand.distance` | `||rel_centre||` |
| `inter_hand.rel_orientation.cos` | `cos(wrap(θ_right - θ_left))` |
| `inter_hand.rel_orientation.sin` | `sin(wrap(θ_right - θ_left))` |
| `inter_hand.log_scale_ratio` | `ln(s_right / s_left)` |

If either `s_left` or `s_right` is `< 1e-6`, `log_scale_ratio = 0`.

---

## 6. Temporal policy

### 6.1 Clock

Timestamps are monotonic capture timestamps in **milliseconds**. They are not wall
clock time and are not derived from frame indices.

### 6.2 Derivatives

All derivatives are causal forward differences over the **actual** elapsed time.
There is no resampling, no fixed frame rate assumption and no future frame.

### 6.3 First frame

For the first frame of a stream:

```
dt_s = 0, elapsed_s = 0, frame_gap = 0, first_frame = 1,
all derivatives = 0, has_previous = 0
```

### 6.4 Gaps and non-monotonic timestamps

Let `dt_ms = timestamp_ms - previous_frame_timestamp_ms` for the immediately
previous frame, regardless of hand presence. The frame is a **segment break** if

```
dt_ms > 600   or   dt_ms <= 0
```

On a segment break:

- `header.frame_gap = 1`;
- `header.dt_s = 0`, `header.elapsed_s = 0`;
- all slot temporal groups are zeroed and `has_previous = 0`;
- slot continuity state and velocity history are cleared;
- the new frame becomes the segment start.

`header.first_frame` is `1` only for the first frame ever, not for a segment break.

`header.elapsed_s` is `(timestamp_ms - segment_start_ms) / 1000`.

### 6.5 Non-monotonic input

A non-monotonic timestamp is treated as a segment break, never as a negative
`dt`. Implementations must not silently reorder frames.

---

## 7. Missingness

Missingness is explicit; zero geometry is only meaningful together with a zero
mask or a zero group-validity flag.

| Situation | Encoding |
|---|---|
| Hand absent | slot `present = 0`; 21 validity channels `0`; local, global and temporal groups `0`; group-validity flags `0` |
| Landmark invalid | its validity channel `0`; any group that requires it is `0` with its flag `0` |
| All landmarks valid but palm scale `< 1e-6` | local group `0`, `local_valid = 0`; global and temporal unaffected |
| No usable previous frame | temporal group `0`, `has_previous = 0`, `temporal_valid = 0` |
| Fewer than two usable global hands | inter-hand group `0` |
| Unknown handedness | slot still filled; `handedness_known = 0`; `slot_assignment_ambiguous = 1` |

No imputation, interpolation, zero-filling with an implicit valid flag, or "nearest
landmark" substitution is permitted.

---

## 8. Numeric rules

- Computation is `float64` in Python and Kotlin (`Double`).
- Golden vectors store `float64` decimal values with enough digits for exact
  round-trip.
- Comparison tolerance for non-mask channels: `atol = 1e-6`, `rtol = 1e-5`.
- Mask, presence and validity channels compare **exactly**.
- `NaN` and infinities are forbidden in output.
- Evaluation order is fixed by this document; implementations must not reassociate
  sums or use fused multiply-add where it changes results.
- A future on-device `float32` model input is permitted only after re-validating
  parity at the model boundary. It does not change the v1 contract.

---

## 9. Constants

| Constant | Value | Use |
|---|---:|---|
| `FEATURE_DIM` | 256 | Vector length |
| `PALM_SCALE_EPSILON` | `1e-6` | Minimum palm scale for local geometry |
| `VECTOR_EPSILON` | `1e-9` | Minimum vector norm for angles and ratios |
| `GAP_THRESHOLD_MS` | `600` | Segment break threshold |
| `ATOL` | `1e-6` | Parity absolute tolerance |
| `RTOL` | `1e-5` | Parity relative tolerance |

---

## 10. Golden vectors and parity

Golden vectors live under `contracts/golden_vectors/features_v1/`:

- `cases.json` — input cases and expected 256-value outputs, canonical JSON;
- `FeatureGoldenVectors.kt` — generated Kotlin test source containing the same
  cases, so Android parity tests need no JSON dependency.

Rules:

- Golden vectors are generated by the Python reference implementation, never
  hand-edited.
- The generator must be deterministic: fixed cases, fixed iteration order, no
  timestamps, no randomness, no environment-dependent values.
- Regeneration is a reviewed diff. A changed expected value with an unchanged
  contract is a failure, not an update.
- Kotlin parity tests must pass before any other CI step that depends on features.
- Cases must cover at least: first frame, steady motion, acceleration, gap,
  non-monotonic timestamp, left only, right only, both hands, absent hand,
  invalid landmark, degenerate palm scale, extreme scale (a hand just above the
  palm-scale epsilon and a hand filling the image), mirrored input, ambiguous
  slots and unknown handedness.

---

## 11. Versioning

`features-v1` is immutable once tagged.

A change requires `features-v2` when it changes any of:

- channel count, name, order or group offsets;
- a formula, unit, coordinate space or constant;
- missingness semantics;
- temporal or slot-assignment semantics;
- tolerance.

A new version requires:

1. a new `contracts/feature_contract_<version>.json` and schema;
2. a new spec document;
3. a new golden-vector directory;
4. an ADR recording the migration and compatibility impact;
5. updated Python and Kotlin implementations and parity tests.

The old version remains readable until a migration is complete. Silent in-place
edits are forbidden.
