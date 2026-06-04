#!/usr/bin/env bash
# Use the machine's ~/.gradle wrapper cache.
# Online by default so dependencies (Mapbox, KSP, etc.) can resolve on first clone.
# Set AURA_GRADLE_OFFLINE=1 to pass --offline (requires a warmed Gradle cache).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
cd "$ROOT"
GRADLE_ARGS=()
if [[ "${AURA_GRADLE_OFFLINE:-}" == "1" ]]; then
  GRADLE_ARGS+=(--offline)
fi
exec ./gradlew "${GRADLE_ARGS[@]}" "$@"
