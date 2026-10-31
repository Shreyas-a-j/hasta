"""Regenerate the features-v1 golden vectors and generated Kotlin source.

Run:

    cd research && uv run --frozen python scripts/generate_golden_vectors.py
"""

from __future__ import annotations

import sys
from pathlib import Path

RESEARCH_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(RESEARCH_ROOT))

from features.golden import write_outputs  # noqa: E402


def main() -> None:
    json_path, kotlin_path = write_outputs()
    print(f"wrote {json_path}")
    print(f"wrote {kotlin_path}")


if __name__ == "__main__":
    main()
