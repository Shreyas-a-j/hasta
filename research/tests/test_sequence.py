import json
from dataclasses import replace

import numpy as np
import pytest

from extract.sequence import (
    FORMAT,
    VERSION,
    LandmarkSequence,
    empty_arrays,
    encode_handedness,
)


def metadata(clip_id="clip-1", signer="S01", label="namaste"):
    return {
        "format": FORMAT,
        "version": VERSION,
        "clip_id": clip_id,
        "signer_id": signer,
        "label": label,
        "source": {"kind": "webcam", "ref": "webcam:test"},
        "consent": {"granted": True, "reference": f"consent/{signer}"},
        "license": {
            "name": "CC BY 4.0",
            "url": "https://creativecommons.org/licenses/by/4.0/",
        },
    }


def make_sequence(frames=3, clip_id="clip-1", signer="S01", label="namaste"):
    arrays = empty_arrays(frames)
    arrays["timestamp_ms"][:] = np.arange(frames) * 100
    arrays["width"][:] = 640
    arrays["height"][:] = 480
    arrays["present"][0, 0] = True
    arrays["validity"][0, 0, :] = True
    arrays["handedness"][0, 0] = encode_handedness("right")
    arrays["handedness_score"][0, 0] = 0.9
    return LandmarkSequence(
        **arrays,
        mirrored_input=False,
        metadata=metadata(clip_id, signer, label),
    )


def test_round_trip_preserves_arrays_and_metadata(tmp_path):
    sequence = make_sequence()
    path = sequence.save(tmp_path / "clip.npz")
    loaded = LandmarkSequence.load(path)

    assert loaded.frame_count == 3
    assert loaded.duration_ms == 200
    assert loaded.metadata == sequence.metadata
    assert loaded.present[0, 0]
    assert not loaded.present[0, 1]
    np.testing.assert_array_equal(loaded.timestamp_ms, sequence.timestamp_ms)


def test_missing_hands_are_explicit_and_never_nan(tmp_path):
    sequence = make_sequence()
    loaded = LandmarkSequence.load(sequence.save(tmp_path / "clip.npz"))
    assert np.all(np.isfinite(loaded.hands))
    assert not loaded.validity[0, 1].any()


def test_non_finite_landmarks_are_rejected():
    sequence = make_sequence()
    sequence.hands[0, 0, 0, 0] = np.nan
    with pytest.raises(ValueError, match="finite"):
        sequence.validate()


def test_absent_hand_with_valid_landmarks_is_rejected():
    sequence = make_sequence()
    sequence.present[0, 1] = False
    sequence.validity[0, 1, 0] = True
    with pytest.raises(ValueError, match="absent hands"):
        sequence.validate()


def test_consent_is_required():
    sequence = make_sequence()
    sequence.metadata["consent"]["granted"] = False
    with pytest.raises(ValueError, match="consent"):
        sequence.validate()


def test_bad_shapes_are_rejected():
    sequence = make_sequence()
    sequence = replace(sequence, hands=sequence.hands[:, :1])
    with pytest.raises(ValueError, match="shape"):
        sequence.validate()


def test_handedness_encoding_is_strict():
    assert encode_handedness("left") == 0
    assert encode_handedness("right") == 1
    assert encode_handedness(None) == -1
    with pytest.raises(ValueError):
        encode_handedness("unknown")


def test_saved_file_contains_no_pickle(tmp_path):
    path = make_sequence().save(tmp_path / "clip.npz")
    with np.load(path, allow_pickle=False) as data:
        assert "metadata" in data.files
        assert data["metadata"].dtype.kind == "U"
        assert json.loads(str(data["metadata"]))["clip_id"] == "clip-1"
