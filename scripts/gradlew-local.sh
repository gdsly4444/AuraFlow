#!/usr/bin/env bash
# Use the machine's ~/.gradle wrapper cache; avoid re-downloading distributions in automation.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
cd "$ROOT"
exec ./gradlew --offline "$@"
