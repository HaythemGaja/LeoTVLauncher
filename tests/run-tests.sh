#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
if ! command -v javac && [[ -x ../jdk/bin/javac ]]; then export PATH="$PWD/../jdk/bin:$PATH"; fi
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
javac -d "$work" app/java/com/leo/tvlauncher/TileMetrics.java tests/TileMetricsTest.java
java -cp "$work" TileMetricsTest
