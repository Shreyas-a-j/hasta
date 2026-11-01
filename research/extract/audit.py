"""Audit a research manifest against the files on disk."""

from __future__ import annotations

import argparse
import sys
from dataclasses import dataclass, field
from pathlib import Path

from .manifest import load_manifest, sha256_file
from .sequence import LandmarkSequence

RAW_VIDEO_SUFFIXES = {".mp4", ".avi", ".mov", ".mkv", ".webm", ".m4v"}


@dataclass(frozen=True)
class Finding:
    level: str
    code: str
    message: str


@dataclass
class AuditReport:
    entries_checked: int = 0
    findings: list[Finding] = field(default_factory=list)
    raw_video_paths: list[str] = field(default_factory=list)

    def error(self, code: str, message: str) -> None:
        self.findings.append(Finding("error", code, message))

    def warning(self, code: str, message: str) -> None:
        self.findings.append(Finding("warning", code, message))

    @property
    def errors(self) -> list[Finding]:
        return [finding for finding in self.findings if finding.level == "error"]

    @property
    def warnings(self) -> list[Finding]:
        return [finding for finding in self.findings if finding.level == "warning"]

    @property
    def ok(self) -> bool:
        return not self.errors


def audit_manifest(manifest_path: Path, base_dir: Path) -> AuditReport:
    report = AuditReport()
    base = Path(base_dir).resolve()

    try:
        document = load_manifest(manifest_path)
    except Exception as error:  # noqa: BLE001 - audit must report, not crash
        report.error("manifest.invalid", f"cannot load manifest: {error}")
        return report

    if not document["entries"]:
        report.warning("manifest.empty", "manifest contains no entries")

    for entry in document["entries"]:
        report.entries_checked += 1
        clip_id = entry["clip_id"]
        relative = Path(entry["path"])
        resolved = (base / relative).resolve()
        if not resolved.is_relative_to(base):
            report.error("path.escape", f"{clip_id}: path escapes the data root: {entry['path']}")
            continue
        if not resolved.is_file():
            report.error("file.missing", f"{clip_id}: missing {entry['path']}")
            continue
        if resolved.stat().st_size != entry["bytes"]:
            report.error(
                "file.size",
                f"{clip_id}: size {resolved.stat().st_size} != manifest {entry['bytes']}",
            )
        digest = sha256_file(resolved)
        if digest != entry["sha256"]:
            report.error("file.sha256", f"{clip_id}: SHA-256 does not match the manifest")

        try:
            sequence = LandmarkSequence.load(resolved)
        except Exception as error:  # noqa: BLE001
            report.error("sequence.invalid", f"{clip_id}: cannot load sequence: {error}")
            continue

        if sequence.frame_count != entry["frames"]:
            report.error(
                "sequence.frames",
                f"{clip_id}: {sequence.frame_count} frames != manifest {entry['frames']}",
            )
        if sequence.duration_ms != entry["duration_ms"]:
            report.error(
                "sequence.duration",
                f"{clip_id}: duration {sequence.duration_ms} != manifest {entry['duration_ms']}",
            )
        metadata = sequence.metadata
        checks = (
            ("clip_id", metadata.get("clip_id"), entry["clip_id"]),
            ("signer_id", metadata.get("signer_id"), entry["signer_id"]),
            ("label", metadata.get("label"), entry["label"]),
            ("consent_reference", metadata.get("consent", {}).get("reference"), entry["consent_reference"]),
            ("license", metadata.get("license", {}).get("name"), entry["license"]),
            ("license_url", metadata.get("license", {}).get("url"), entry["license_url"]),
            ("source_kind", metadata.get("source", {}).get("kind"), entry["source_kind"]),
            ("source_ref", metadata.get("source", {}).get("ref"), entry["source_ref"]),
        )
        for name, actual, expected in checks:
            if str(actual) != str(expected):
                report.error(
                    "sequence.metadata",
                    f"{clip_id}: metadata {name} {actual!r} != manifest {expected!r}",
                )

    for path in sorted(base.rglob("*")):
        if path.is_file() and path.suffix.lower() in RAW_VIDEO_SUFFIXES:
            report.raw_video_paths.append(str(path.relative_to(base)))
            report.warning("raw_video.present", f"raw video present: {path.relative_to(base)}")

    return report


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Audit a HASTA research manifest.")
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--base", required=True, type=Path)
    args = parser.parse_args(argv)

    report = audit_manifest(args.manifest, args.base)
    for finding in report.findings:
        print(f"{finding.level.upper()}: [{finding.code}] {finding.message}")
    print(
        f"checked {report.entries_checked} entries, "
        f"{len(report.errors)} errors, {len(report.warnings)} warnings"
    )
    return 0 if report.ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
