#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export JAVA_HOME

echo "== Python environment =="
cd "$ROOT/research"
uv sync --frozen --extra test

echo "== Python tests =="
uv run --frozen pytest

echo "== Regenerate committed artifacts =="
uv run --frozen python scripts/generate_contract.py
uv run --frozen python scripts/generate_golden_vectors.py

cd "$ROOT"
if ! git diff --exit-code -- \
  contracts \
  android/app/src/test/java/com/hasta/app/domain/features/FeatureGoldenVectors.kt; then
  echo "generated artifacts are stale; commit the regenerated files" >&2
  exit 1
fi

echo "== Kotlin feature parity (first) =="
cd "$ROOT/android"
./gradlew :app:testDebugUnitTest --tests 'com.hasta.app.domain.features.*'

echo "== Full Android unit tests =="
./gradlew :app:testDebugUnitTest

echo "== Debug APK =="
./gradlew :app:assembleDebug

echo "verify: PASS"
