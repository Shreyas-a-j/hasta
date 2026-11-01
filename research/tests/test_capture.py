import numpy as np
import pytest

from extract.capture import CaptureMetadata, DetectedHand, SequenceBuilder


def metadata(**overrides):
    values = {
        "clip_id": "clip-1",
        "signer_id": "S01",
        "label": "namaste",
        "source_kind": "webcam",
        "source_ref": "webcam:test",
        "consent_reference": "consent/S01",
        "license_name": "CC BY 4.0",
        "license_url": "https://creativecommons.org/licenses/by/4.0/",
    }
    values.update(overrides)
    return CaptureMetadata(**values)


def hand(label="right", score=0.9, validity=None):
    return DetectedHand(
        handedness=label,
        handedness_score=score,
        landmarks=np.zeros((21, 3), dtype=np.float64),
        validity=validity,
    )


def test_builder_preserves_detector_order_and_missingness():
    builder = SequenceBuilder(metadata())
    builder.add_frame(0, 640, 480, [hand("right")])
    builder.add_frame(100, 640, 480, [])
    sequence = builder.build()

    assert sequence.frame_count == 2
    assert sequence.present[0, 0]
    assert not sequence.present[0, 1]
    assert not sequence.present[1].any()
    assert np.all(np.isfinite(sequence.hands))
    assert sequence.handedness[0, 0] == 1


def test_builder_rejects_more_than_two_hands():
    builder = SequenceBuilder(metadata())
    with pytest.raises(ValueError, match="two hands"):
        builder.add_frame(0, 640, 480, [hand(), hand(), hand()])


def test_builder_rejects_an_empty_sequence():
    with pytest.raises(ValueError, match="empty"):
        SequenceBuilder(metadata()).build()


def test_metadata_requires_consent_reference():
    with pytest.raises(ValueError, match="consent"):
        metadata(consent_reference=" ").validate()


def test_metadata_requires_a_license_url():
    with pytest.raises(ValueError, match="license_url"):
        metadata(license_url="not-a-url").validate()


def test_metadata_rejects_an_unknown_source_kind():
    with pytest.raises(ValueError, match="source_kind"):
        metadata(source_kind="cloud").validate()


def test_detected_hand_rejects_bad_shapes_and_values():
    with pytest.raises(ValueError, match="shape"):
        DetectedHand("right", 0.9, np.zeros((20, 3)))
    with pytest.raises(ValueError, match="finite"):
        DetectedHand("right", 0.9, np.full((21, 3), np.nan))
    with pytest.raises(ValueError, match="score"):
        DetectedHand("right", 1.5, np.zeros((21, 3)))
    with pytest.raises(ValueError, match="handedness"):
        DetectedHand("unknown", 0.9, np.zeros((21, 3)))


def test_detected_hand_defaults_to_all_valid():
    assert hand().validity.all()
