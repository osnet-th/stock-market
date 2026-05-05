#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/check-documented-workflow.sh [--base <branch>] [--quiet]
EOF
}

fail() {
  echo "[check-documented-workflow] $1" >&2
  exit 1
}

base_branch="${BASE_BRANCH:-main}"
quiet="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base)
      [[ $# -ge 2 ]] || fail "--base requires a value"
      base_branch="$2"
      shift 2
      ;;
    --quiet)
      quiet="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown option: $1"
      ;;
  esac
done

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || fail "Git repository not found"
cd "$repo_root"

if git show-ref --verify --quiet "refs/heads/${base_branch}"; then
  base_ref="$base_branch"
elif git show-ref --verify --quiet "refs/remotes/origin/${base_branch}"; then
  base_ref="origin/${base_branch}"
else
  fail "Base branch '${base_branch}' was not found locally or on origin"
fi

merge_base=$(git merge-base HEAD "$base_ref") || fail "Unable to compute merge-base with ${base_ref}"

changed_files=$(git diff --diff-filter=ACMR --name-only "${merge_base}..HEAD")

if [[ -z "$changed_files" ]]; then
  [[ "$quiet" == "true" ]] || echo "[check-documented-workflow] No changes detected against ${base_ref}"
  exit 0
fi

brainstorm_found="false"
plan_found="false"

while IFS= read -r file; do
  [[ -n "$file" ]] || continue
  case "$file" in
    docs/brainstorms/*.md) brainstorm_found="true" ;;
    docs/plans/*.md) plan_found="true" ;;
  esac
done <<< "$changed_files"

[[ "$brainstorm_found" == "true" ]] || fail "Documented workflow requires at least one docs/brainstorms/*.md file in the branch diff against ${base_ref}"
[[ "$plan_found" == "true" ]] || fail "Documented workflow requires at least one docs/plans/*.md file in the branch diff against ${base_ref}"

if [[ "$quiet" != "true" ]]; then
  cat <<EOF
[check-documented-workflow] OK
- base: ${base_ref}
- brainstorm docs found in branch diff
- plan docs found in branch diff
EOF
fi
