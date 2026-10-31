"""Build and validate the research manifest.

The manifest is the auditable index of landmark sequences. It records consent,
licence, provenance, size and SHA-256 for every sequence. It never points at raw
video, and it binds the data to the exact features-v1 contract hash.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath

from jsonschema import Draft202012Validator

from features.contract import CONTRACT_NAME, VERSION as FEATURE_VERSION, contract_sha256

from .sequence import LandmarkSequence

FORMAT = "hasta.research_manifest"
VERSION = "research-manifest-v1"
SCHEMA_PATH = Path(__file__).resolve().parents[2] / "contracts" / "research_manifest.schema.json"


def utc_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with Path(path).open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonical_json(value: object) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


def _relative_path(path: Path, base_dir: Path) -> str:
    try:
        relative = Path(path).resolve().relative_to(Path(base_dir).resolve())
    except ValueError as error:
        raise ValueError(f"{path} is outside the research data root {base_dir}") from error
    return PurePosixPath(relative).as_posix()


def build_entry(sequence_path: Path, base_dir: Path) -> dict:
    sequence = LandmarkSequence.load(sequence_path)
    metadata = sequence.metadata
    source = metadata.get("source")
    license_info = metadata.get("license")
    consent = metadata.get("consent")
    if not isinstance(source, dict) or not isinstance(license_info, dict) or not isinstance(consent, dict):
        raise ValueError(f"{sequence_path} metadata must contain source, license and consent objects")
    if source.get("kind") not in ("webcam", "dataset"):
        raise ValueError(f"{sequence_path} source.kind must be webcam or dataset")
    return {
        "clip_id": metadata["clip_id"],
        "path": _relative_path(sequence_path, base_dir),
        "sha256": sha256_file(sequence_path),
        "bytes": Path(sequence_path).stat().st_size,
        "frames": sequence.frame_count,
        "duration_ms": sequence.duration_ms,
        "signer_id": metadata["signer_id"],
        "label": metadata["label"],
        "source_kind": source["kind"],
        "source_ref": str(source.get("ref", "")),
        "consent_reference": str(consent.get("reference", "")),
        "license": str(license_info.get("name", "")),
        "license_url": str(license_info.get("url", "")),
    }


def build_manifest(
    sequence_paths: list[Path],
    base_dir: Path,
    created_at_utc: str | None = None,
) -> dict:
    entries = [build_entry(path, base_dir) for path in sorted(sequence_paths, key=str)]
    document = {
        "format": FORMAT,
        "version": VERSION,
        "created_at_utc": created_at_utc or utc_now(),
        "feature_contract": {
            "name": CONTRACT_NAME,
            "version": FEATURE_VERSION,
            "sha256": contract_sha256(),
        },
        "entries": entries,
    }
    validate_manifest(document)
    return document


def _schema() -> dict:
    return json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))


def validate_manifest(document: dict) -> None:
    Draft202012Validator(_schema()).validate(document)
    if document["feature_contract"]["sha256"] != contract_sha256():
        raise ValueError("manifest feature contract hash does not match the current contract")
    seen_clips: set[str] = set()
    seen_paths: set[str] = set()
    for entry in document["entries"]:
        clip_id = entry["clip_id"]
        if clip_id in seen_clips:
            raise ValueError(f"duplicate clip_id {clip_id!r}")
        seen_clips.add(clip_id)
        path = PurePosixPath(entry["path"])
        if path.is_absolute() or ".." in path.parts:
            raise ValueError(f"manifest path must be relative and contained: {entry['path']!r}")
        if path.suffix != ".npz":
            raise ValueError(f"manifest path must be a .npz sequence: {entry['path']!r}")
        if entry["path"] in seen_paths:
            raise ValueError(f"duplicate manifest path {entry['path']!r}")
        seen_paths.add(entry["path"])
        if not entry["source_ref"].strip():
            raise ValueError(f"clip {clip_id!r} has an empty source reference")
        if not entry["consent_reference"].strip():
            raise ValueError(f"clip {clip_id!r} has an empty consent reference")


def write_manifest(document: dict, path: Path) -> Path:
    validate_manifest(document)
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, indent=2, sort_keys=True, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    return path


def load_manifest(path: Path) -> dict:
    document = json.loads(Path(path).read_text(encoding="utf-8"))
    validate_manifest(document)
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Build a research manifest from .npz sequences.")
    parser.add_argument("--base", required=True, type=Path, help="research data root")
    parser.add_argument("--out", required=True, type=Path, help="manifest output path")
    parser.add_argument("--created-at", default=None, help="UTC timestamp, default now")
    args = parser.parse_args(argv)

    paths = sorted(args.base.rglob("*.npz"))
    if not paths:
        print(f"no .npz sequences under {args.base}", file=sys.stderr)
        return 1
    document = build_manifest(paths, args.base, created_at_utc=args.created_at)
    write_manifest(document, args.out)
    print(f"wrote {args.out} with {len(document['entries'])} entries")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
