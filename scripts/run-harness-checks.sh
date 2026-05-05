#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/run-harness-checks.sh <mode>

Modes:
  local-documented
  local-lightweight
  ci-documented
EOF
}

fail() {
  echo "[run-harness-checks] $1" >&2
  exit 1
}

mode="${1:-}"
[[ -n "$mode" ]] || {
  usage >&2
  exit 1
}

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || fail "Git repository not found"
base_branch="${BASE_BRANCH:-main}"

case "$mode" in
  local-documented)
    "$repo_root/scripts/check-worktree.sh" --mode documented --quiet
    "$repo_root/scripts/check-documented-workflow.sh" --base "$base_branch" --quiet
    echo "[run-harness-checks] Local documented workflow checks passed."
    ;;
  local-lightweight)
    "$repo_root/scripts/check-worktree.sh" --mode lightweight --quiet
    echo "[run-harness-checks] Local lightweight workflow checks passed."
    ;;
  ci-documented)
    "$repo_root/scripts/check-documented-workflow.sh" --base "$base_branch" --quiet
    echo "[run-harness-checks] CI documented workflow checks passed."
    ;;
  -h|--help)
    usage
    ;;
  *)
    fail "Unknown mode: $mode"
    ;;
esac
