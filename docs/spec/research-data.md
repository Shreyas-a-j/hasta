# Research Data Format — landmarks-v1 and research-manifest-v1

- Status: Locked for M1
- Sequence format: `hasta.landmarks` / `landmarks-v1`
- Manifest format: `hasta.research_manifest` / `research-manifest-v1`
- Feature contract: `hasta.features` / `features-v1`

This document defines the data HASTA records for research and how it is audited,
licensed and deleted. It is deliberately separate from the feature contract: a
record preserves detector output so features can be recomputed later.

---

## 1. What is stored

Each recording is one compressed `.npz` file containing landmarks and timing, plus
JSON metadata. Raw video is **not** stored, copied or redistributed by HASTA tools.

### 1.1 Arrays

All arrays have a leading frame dimension `T`:

| Key | Shape | dtype | Meaning |
|---|---|---|---|
| `timestamp_ms` | `(T,)` | int64 | Capture timestamp, milliseconds |
| `width` | `(T,)` | int32 | Upright analysis image width |
| `height` | `(T,)` | int32 | Upright analysis image height |
| `hands` | `(T, 2, 21, 3)` | float64 | Detector-order landmarks, image-normalized |
| `validity` | `(T, 2, 21)` | bool | Per-landmark validity |
| `present` | `(T, 2)` | bool | Hand slot present in this frame |
| `handedness` | `(T, 2)` | int8 | `-1` unknown, `0` left, `1` right |
| `handedness_score` | `(T, 2)` | float64 | MediaPipe handedness confidence |
| `mirrored_input` | scalar | bool | Source image was mirrored |
| `metadata` | scalar | unicode | Canonical JSON metadata |

Hands remain in **detector output order**. Slot assignment is a feature concern and
is recomputed at extraction time, not baked into the record.

### 1.2 Missingness

- An absent hand has `present == False`, all 21 validity entries `False`, and zero
  coordinates.
- A present hand must have at least one valid landmark.
- Coordinates are always finite. `NaN` and infinity are rejected; they are never
  used as missing markers.
- `handedness_score` is `0.0` when unknown and always finite.

### 1.3 Metadata

Required keys:

```json
{
  "format": "hasta.landmarks",
  "version": "landmarks-v1",
  "clip_id": "S01-namaste-001",
  "signer_id": "S01",
  "label": "namaste",
  "source": {"kind": "webcam", "ref": "webcam:default"},
  "consent": {"granted": true, "reference": "consent/S01-2026-10-31"},
  "license": {
    "name": "CC BY 4.0",
    "url": "https://creativecommons.org/licenses/by/4.0/"
  },
  "notes": ""
}
```

`source.kind` is `webcam` or `dataset`. `consent.granted` must be `true` and the
reference must be non-empty before a sequence can be written.

---

## 2. Research manifest

`research-manifest-v1` indexes sequences under a data root. It records:

- the exact `features-v1` contract name, version and SHA-256;
- each clip's relative `.npz` path, SHA-256, byte size, frame count and duration;
- signer, label, source kind and source reference;
- consent reference, licence name and licence URL.

Schema: `contracts/research_manifest.schema.json`.

Manifest rules:

- Paths are relative, contained under the data root, and end in `.npz`.
- `clip_id` and `path` are unique.
- Every entry's consent reference and source reference are non-empty.
- The contract hash must match the current `contracts/feature_contract.json`.
- A manifest is regenerated, never hand-edited.

Build:

```bash
cd research
uv run --frozen python -m extract.manifest \
  --base data \
  --out data/manifest.json \
  --created-at 2026-10-31T00:00:00Z
```

---

## 3. Audit

```bash
cd research
uv run --frozen python -m extract.audit \
  --manifest data/manifest.json \
  --base data
```

The audit checks:

- manifest schema and semantic rules;
- each file exists, stays inside the data root, and matches byte size and SHA-256;
- each `.npz` loads with `allow_pickle=False` and passes `landmarks-v1` validation;
- metadata matches the manifest (clip, signer, label, consent, licence, source);
- frame count and duration match;
- raw video files are reported as warnings.

Errors make the audit fail. Warnings are reported but do not fail.

---

## 4. Deletion

Deletion is scoped to a signer or explicit clip IDs, only removes `.npz` files
referenced by the manifest, and appends a JSONL audit record.

Dry run (default):

```bash
uv run --frozen python -m extract.deletion \
  --manifest data/manifest.json --base data --signer S01
```

Live deletion requires an explicit confirmation, actor and reason:

```bash
uv run --frozen python -m extract.deletion \
  --manifest data/manifest.json --base data --signer S01 \
  --execute --confirm DELETE --actor "data steward" --reason "consent withdrawn"
```

After deletion, regenerate the manifest from the remaining sequences.

---

## 5. Capture and extraction

### 5.1 Webcam

Requires a webcam, a MediaPipe `hand_landmarker.task` model and the `capture` extra.

```bash
cd research
uv sync --extra capture
uv run --frozen python -m extract.recorder \
  --model /path/to/hand_landmarker.task \
  --out data/sequences/S01/namaste/S01-namaste-001.npz \
  --clip-id S01-namaste-001 --signer S01 --label namaste \
  --consent-reference consent/S01-2026-10-31 \
  --license "CC BY 4.0" \
  --license-url https://creativecommons.org/licenses/by/4.0/ \
  --duration 10
```

### 5.2 Dataset video

The video is read in place and never copied. The tool must only be used with
datasets whose licence permits derivative landmark extraction.

```bash
uv run --frozen python -m extract.video_dataset \
  --model /path/to/hand_landmarker.task \
  --video /path/to/clip.mp4 \
  --out data/sequences/INCLUDE/.../clip.npz \
  --clip-id ... --signer ... --label ... \
  --dataset include --source-ref 4010759/clip.mp4 \
  --consent-reference ... --license "CC BY 4.0" \
  --license-url https://creativecommons.org/licenses/by/4.0/
```

Both tools report submitted, processed and dropped frames. Neither writes raw
video.

---

## 6. Dataset policy

- Do not download, redistribute or bundle copyrighted dataset assets.
- Do not commit raw video or derived sequences to this repository.
- Record the dataset DOI/Zenodo ID, licence and source clip in metadata.
- A dataset with unclear or conflicting licence terms is on HOLD.
- Consent is required for self-recorded data; deletion must be possible per signer.
- Physical-device, webcam and dataset gates are reported as **NOT VERIFIED** when
  the required hardware or data was not used.
