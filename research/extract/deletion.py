"""Plan and execute deletion of research recordings.

Deletion is explicit, scoped and logged. A plan can only remove ``.npz`` sequence
files that appear in the manifest and resolve inside the data root. Raw video is
never touched by this tool because the manifest never references it.
"""

from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass
from pathlib import Path

from .manifest import load_manifest, sha256_file, utc_now


@dataclass(frozen=True)
class DeletionPlan:
    base_dir: Path
    entries: tuple[dict, ...]
    files: tuple[Path, ...]

    @property
    def clip_ids(self) -> tuple[str, ...]:
        return tuple(entry["clip_id"] for entry in self.entries)


def plan_deletion(
    document: dict,
    base_dir: Path,
    signer_id: str | None = None,
    clip_ids: set[str] | None = None,
) -> DeletionPlan:
    if signer_id is None and not clip_ids:
        raise ValueError("refusing to plan a deletion without a signer or clip selector")
    base = Path(base_dir).resolve()
    entries: list[dict] = []
    files: list[Path] = []
    seen: set[Path] = set()
    for entry in document["entries"]:
        if signer_id is not None and entry["signer_id"] != signer_id:
            continue
        if clip_ids and entry["clip_id"] not in clip_ids:
            continue
        resolved = (base / entry["path"]).resolve()
        if not resolved.is_relative_to(base):
            raise ValueError(f"{entry['clip_id']}: path escapes the data root")
        if resolved.suffix != ".npz":
            raise ValueError(f"{entry['clip_id']}: refusing to delete a non-sequence file")
        if resolved in seen:
            raise ValueError(f"duplicate deletion target {resolved}")
        seen.add(resolved)
        entries.append(entry)
        files.append(resolved)
    return DeletionPlan(base, tuple(entries), tuple(files))


def execute_deletion(
    plan: DeletionPlan,
    dry_run: bool = True,
    log_path: Path | None = None,
    actor: str = "",
    reason: str = "",
) -> list[Path]:
    if dry_run:
        return list(plan.files)
    if not actor.strip() or not reason.strip():
        raise ValueError("live deletion requires a non-empty actor and reason")

    log_path = Path(log_path) if log_path is not None else plan.base_dir / "deletion_log.jsonl"
    log_path.parent.mkdir(parents=True, exist_ok=True)
    deleted: list[Path] = []
    with log_path.open("a", encoding="utf-8") as log:
        for entry, path in zip(plan.entries, plan.files):
            digest = sha256_file(path) if path.exists() else None
            if path.exists():
                path.unlink()
                deleted.append(path)
            record = {
                "deleted_at_utc": utc_now(),
                "actor": actor,
                "reason": reason,
                "clip_id": entry["clip_id"],
                "path": str(path.relative_to(plan.base_dir)),
                "sha256": digest,
                "existed": digest is not None,
            }
            log.write(json.dumps(record, sort_keys=True) + "\n")
    return deleted


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Delete scoped HASTA research recordings.")
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--base", required=True, type=Path)
    parser.add_argument("--signer", default=None)
    parser.add_argument("--clip", action="append", default=None)
    parser.add_argument("--execute", action="store_true", help="actually delete; default is dry run")
    parser.add_argument("--confirm", default="", help="must be DELETE with --execute")
    parser.add_argument("--actor", default="")
    parser.add_argument("--reason", default="")
    args = parser.parse_args(argv)

    document = load_manifest(args.manifest)
    plan = plan_deletion(document, args.base, args.signer, set(args.clip or []))
    if not plan.files:
        print("no matching recordings", file=sys.stderr)
        return 1

    if args.execute:
        if args.confirm != "DELETE":
            print("refusing to delete: pass --confirm DELETE", file=sys.stderr)
            return 2
        deleted = execute_deletion(
            plan,
            dry_run=False,
            actor=args.actor,
            reason=args.reason,
        )
        print(f"deleted {len(deleted)} recordings")
    else:
        print(f"dry run: {len(plan.files)} recordings would be deleted")
        for path in plan.files:
            print(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
