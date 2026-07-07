#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/check-documented-workflow.sh [--base <branch>] [--through <stage>] [--quiet]
EOF
}

fail() {
  echo "[check-documented-workflow] $1" >&2
  exit 1
}

base_branch="${BASE_BRANCH:-main}"
through_stage="push"
quiet="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base)
      [[ $# -ge 2 ]] || fail "--base requires a value"
      base_branch="$2"
      shift 2
      ;;
    --through)
      [[ $# -ge 2 ]] || fail "--through requires a value"
      through_stage="$2"
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

stage_index() {
  case "$1" in
    start) printf '%s\n' 0 ;;
    brainstorm) printf '%s\n' 1 ;;
    plan) printf '%s\n' 2 ;;
    work) printf '%s\n' 3 ;;
    review) printf '%s\n' 4 ;;
    validation) printf '%s\n' 5 ;;
    commit) printf '%s\n' 6 ;;
    push) printf '%s\n' 7 ;;
    *) fail "Unsupported stage: $1" ;;
  esac
}

stage_required() {
  local stage="$1"
  [[ "$(stage_index "$stage")" -le "$(stage_index "$through_stage")" ]]
}

require_stage_doc() {
  local stage="$1"
  local found="$2"
  local pattern="$3"

  if stage_required "$stage" && [[ "$found" != "true" ]]; then
    fail "Documented workflow requires at least one ${pattern} file through ${through_stage} against ${base_ref}"
  fi
}

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

changed_files=$(
  {
    git diff --diff-filter=ACMR --name-only "${merge_base}..HEAD"
    git diff --diff-filter=ACMR --cached --name-only
    git diff --diff-filter=ACMR --name-only
    git ls-files --others --exclude-standard
  } | sort -u
)

if [[ -z "$changed_files" ]]; then
  [[ "$quiet" == "true" ]] || echo "[check-documented-workflow] No changes detected against ${base_ref}"
  exit 0
fi

brainstorm_found="false"
plan_found="false"
work_found="false"
review_found="false"
validation_found="false"
commit_found="false"
push_found="false"
gate_found="false"

stage_docs=()
gate_docs=()

while IFS= read -r file; do
  [[ -n "$file" ]] || continue
  case "$file" in
    docs/brainstorms/*.md) brainstorm_found="true"; stage_docs+=("$file") ;;
    docs/plans/*.md) plan_found="true"; stage_docs+=("$file") ;;
    docs/works/*.md) work_found="true"; stage_docs+=("$file") ;;
    docs/reviews/*.md) review_found="true"; stage_docs+=("$file") ;;
    docs/validations/*.md) validation_found="true"; stage_docs+=("$file") ;;
    docs/commits/*.md) commit_found="true"; stage_docs+=("$file") ;;
    docs/pushes/*.md) push_found="true"; stage_docs+=("$file") ;;
    docs/gates/*.md) gate_found="true"; gate_docs+=("$file") ;;
  esac
done <<< "$changed_files"

require_stage_doc brainstorm "$brainstorm_found" "docs/brainstorms/*.md"
require_stage_doc plan "$plan_found" "docs/plans/*.md"
require_stage_doc work "$work_found" "docs/works/*.md"
require_stage_doc review "$review_found" "docs/reviews/*.md"
require_stage_doc validation "$validation_found" "docs/validations/*.md"
require_stage_doc commit "$commit_found" "docs/commits/*.md"
require_stage_doc push "$push_found" "docs/pushes/*.md"
[[ "$gate_found" == "true" ]] || fail "Documented workflow requires at least one docs/gates/*.md file in the branch diff against ${base_ref}"

for file in "${stage_docs[@]}"; do
  grep -Eq '^gate: docs/gates/[^[:space:]]+\.md$' "$file" \
    || fail "Stage document must reference a gate log with 'gate: docs/gates/*.md': ${file}"
done

for file in "${gate_docs[@]}"; do
  for stage in start brainstorm plan work review validation commit push; do
    stage_required "$stage" || continue
    grep -Eq "^- ${stage}: approved$" "$file" \
      || fail "Gate log must include '- ${stage}: approved': ${file}"
  done
done

if [[ "$quiet" != "true" ]]; then
  cat <<EOF
[check-documented-workflow] OK
- base: ${base_ref}
- through: ${through_stage}
- required stage docs found through ${through_stage}
- gate docs found in branch diff
EOF
fi
