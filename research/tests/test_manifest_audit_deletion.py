import json
from pathlib import Path

import pytest
from jsonschema import ValidationError

from conftest import make_research_sequence
from extract.audit import audit_manifest
from extract.deletion import execute_deletion, plan_deletion
from extract.manifest import build_manifest, load_manifest, validate_manifest, write_manifest

CREATED = "2026-10-31T00:00:00Z"


def build(tmp_path):
    first = make_research_sequence(
        clip_id="clip-a", signer="S01", base_dir=tmp_path
    )
    second = make_research_sequence(
        clip_id="clip-b", signer="S02", label="hello", base_dir=tmp_path
    )
    paths = [
        tmp_path / "sequences/S01/namaste/clip-a.npz",
        tmp_path / "sequences/S02/hello/clip-b.npz",
    ]
    document = build_manifest(paths, tmp_path, created_at_utc=CREATED)
    return first, second, paths, document


def test_manifest_records_provenance_and_integrity(tmp_path):
    _, _, _, document = build(tmp_path)
    validate_manifest(document)
    assert document["format"] == "hasta.research_manifest"
    assert len(document["entries"]) == 2
    assert [entry["clip_id"] for entry in document["entries"]] == ["clip-a", "clip-b"]
    for entry in document["entries"]:
        assert len(entry["sha256"]) == 64
        assert entry["bytes"] > 0
        assert entry["frames"] == 3
        assert entry["duration_ms"] == 200
        assert entry["consent_reference"].startswith("consent/")
        assert entry["license"] == "CC BY 4.0"


def test_manifest_round_trip(tmp_path):
    _, _, _, document = build(tmp_path)
    path = write_manifest(document, tmp_path / "manifest.json")
    assert load_manifest(path) == document


def test_manifest_rejects_duplicate_clip_ids(tmp_path):
    _, _, _, document = build(tmp_path)
    document["entries"].append(dict(document["entries"][0]))
    with pytest.raises(ValueError, match="duplicate clip_id"):
        validate_manifest(document)


def test_manifest_rejects_path_escape(tmp_path):
    _, _, _, document = build(tmp_path)
    document["entries"][0]["path"] = "../outside.npz"
    with pytest.raises(ValueError, match="relative and contained"):
        validate_manifest(document)


def test_manifest_rejects_a_stale_contract_hash(tmp_path):
    _, _, _, document = build(tmp_path)
    document["feature_contract"]["sha256"] = "0" * 64
    with pytest.raises(ValueError, match="contract hash"):
        validate_manifest(document)


def test_manifest_schema_rejects_a_bad_license_url(tmp_path):
    _, _, _, document = build(tmp_path)
    document["entries"][0]["license_url"] = "not-a-url"
    with pytest.raises(ValidationError):
        validate_manifest(document)


def test_audit_accepts_a_consistent_manifest(tmp_path):
    _, _, _, document = build(tmp_path)
    path = write_manifest(document, tmp_path / "manifest.json")
    report = audit_manifest(path, tmp_path)
    assert report.ok
    assert report.entries_checked == 2
    assert report.errors == []


def test_audit_detects_missing_and_tampered_files(tmp_path):
    _, _, paths, document = build(tmp_path)
    path = write_manifest(document, tmp_path / "manifest.json")
    paths[1].unlink()
    with paths[0].open("ab") as handle:
        handle.write(b"tamper")
    report = audit_manifest(path, tmp_path)
    codes = {finding.code for finding in report.errors}
    assert "file.missing" in codes
    assert "file.size" in codes or "file.sha256" in codes


def test_audit_detects_metadata_drift(tmp_path):
    _, _, _, document = build(tmp_path)
    document["entries"][0]["label"] = "different"
    path = write_manifest(document, tmp_path / "manifest.json")
    report = audit_manifest(path, tmp_path)
    assert any(finding.code == "sequence.metadata" for finding in report.errors)


def test_audit_warns_about_raw_video(tmp_path):
    _, _, _, document = build(tmp_path)
    path = write_manifest(document, tmp_path / "manifest.json")
    (tmp_path / "raw.mp4").write_bytes(b"not really video")
    report = audit_manifest(path, tmp_path)
    assert report.ok
    assert report.raw_video_paths == ["raw.mp4"]
    assert any(finding.code == "raw_video.present" for finding in report.warnings)


def test_deletion_dry_run_does_not_delete(tmp_path):
    _, _, paths, document = build(tmp_path)
    plan = plan_deletion(document, tmp_path, signer_id="S01")
    assert len(plan.files) == 1
    result = execute_deletion(plan, dry_run=True)
    assert result == [paths[0]]
    assert paths[0].exists()


def test_deletion_removes_only_selected_recordings_and_logs(tmp_path):
    _, _, paths, document = build(tmp_path)
    plan = plan_deletion(document, tmp_path, signer_id="S01")
    deleted = execute_deletion(
        plan,
        dry_run=False,
        actor="tester",
        reason="consent withdrawn",
    )
    assert deleted == [paths[0]]
    assert not paths[0].exists()
    assert paths[1].exists()
    log = (tmp_path / "deletion_log.jsonl").read_text(encoding="utf-8").splitlines()
    record = json.loads(log[0])
    assert record["actor"] == "tester"
    assert record["reason"] == "consent withdrawn"
    assert record["clip_id"] == "clip-a"
    assert record["existed"] is True


def test_deletion_requires_a_selector(tmp_path):
    _, _, _, document = build(tmp_path)
    with pytest.raises(ValueError, match="without a signer or clip selector"):
        plan_deletion(document, tmp_path)


def test_live_deletion_requires_an_actor_and_reason(tmp_path):
    _, _, _, document = build(tmp_path)
    plan = plan_deletion(document, tmp_path, signer_id="S01")
    with pytest.raises(ValueError, match="actor and reason"):
        execute_deletion(plan, dry_run=False)


def test_deletion_refuses_a_path_outside_the_data_root(tmp_path):
    _, _, _, document = build(tmp_path)
    document["entries"][0]["path"] = "../outside.npz"
    with pytest.raises(ValueError, match="escapes"):
        plan_deletion(document, tmp_path, signer_id="S01")
