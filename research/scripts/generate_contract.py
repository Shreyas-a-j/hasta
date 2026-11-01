"""Regenerate ``contracts/feature_contract.json`` from the normative Python code.

The generated file is committed and never hand-edited. Run:

    cd research && uv run --frozen python scripts/generate_contract.py

and commit the resulting diff together with any contract change.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

RESEARCH_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(RESEARCH_ROOT))

from features.contract import build_contract, contract_sha256, validate_contract  # noqa: E402

REPO_ROOT = RESEARCH_ROOT.parent
CONTRACT_PATH = REPO_ROOT / "contracts" / "feature_contract.json"


def main() -> None:
    contract = build_contract()
    validate_contract(contract)
    CONTRACT_PATH.write_text(
        json.dumps(contract, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(f"wrote {CONTRACT_PATH.relative_to(REPO_ROOT)}")
    print(f"sha256 {contract_sha256(contract)}")


if __name__ == "__main__":
    main()
