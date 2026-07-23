#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/manual-common-core-smoke"
rm -rf "$OUT"
mkdir -p "$OUT/classes"

find "$ROOT/common-core/src/main/java" -name '*.java' -print0 \
  | xargs -0 javac -encoding UTF-8 -d "$OUT/classes"

find "$ROOT/test-support/src/main/java" -name '*.java' -print0 \
  | xargs -0 javac -encoding UTF-8 -cp "$OUT/classes" -d "$OUT/classes"

java -cp "$OUT/classes" com.velorise.emc.tests.RefactorBaselineSmoke
