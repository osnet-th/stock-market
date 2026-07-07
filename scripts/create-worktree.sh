#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/create-worktree.sh --issue <number> [--base <branch>] [--path <path>] <branch>

Examples:
  scripts/create-worktree.sh --issue 123 feat/issue-123-dashboard-summary
  scripts/create-worktree.sh --issue 124 --base develop fix/issue-124-chat-context-reset
  scripts/create-worktree.sh --issue 125 --path ../wt-issue-125-docs-policy docs/issue-125-git-worktree-policy
EOF
}

fail() {
  echo "[create-worktree] $1" >&2
  exit 1
}

resolve_path() {
  local target="$1"
  local parent
  local base

  parent=$(cd "$(dirname "$target")" && pwd)
  base=$(basename "$target")
  printf '%s/%s\n' "$parent" "$base"
}

find_branch_worktree() {
  local branch_name="$1"

  git worktree list --porcelain | awk -v branch="refs/heads/${branch_name}" '
    /^worktree / { current_worktree = substr($0, 10) }
    /^branch / && $2 == branch { print current_worktree }
  '
}

primary_worktree() {
  git worktree list --porcelain | awk '
    /^worktree / { print substr($0, 10); exit }
  '
}

base_branch="${BASE_BRANCH:-main}"
target_path=""
branch_name=""
issue_number=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base)
      [[ $# -ge 2 ]] || fail "--base requires a value"
      base_branch="$2"
      shift 2
      ;;
    --path)
      [[ $# -ge 2 ]] || fail "--path requires a value"
      target_path="$2"
      shift 2
      ;;
    --issue)
      [[ $# -ge 2 ]] || fail "--issue requires a value"
      issue_number="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    -*)
      fail "Unknown option: $1"
      ;;
    *)
      [[ -z "$branch_name" ]] || fail "Branch name already provided: $branch_name"
      branch_name="$1"
      shift
      ;;
  esac
done

[[ -n "$branch_name" ]] || {
  usage >&2
  exit 1
}
[[ "$issue_number" =~ ^[0-9]+$ ]] || fail "--issue <number> is required before creating a worktree"

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || fail "Git repository not found"
cd "$repo_root"

if [[ -z "$target_path" ]]; then
  sanitized_branch="${branch_name//\//-}"
  target_path="$(dirname "$repo_root")/wt-issue-${issue_number}-${sanitized_branch}"
fi

target_path=$(resolve_path "$target_path")
[[ ! -e "$target_path" ]] || fail "Target path already exists: $target_path"

existing_branch_worktree=$(find_branch_worktree "$branch_name")
[[ -z "$existing_branch_worktree" ]] || fail "Branch '${branch_name}' is already checked out at ${existing_branch_worktree}"

primary_path=$(primary_worktree)
[[ -n "$primary_path" ]] || fail "Primary worktree was not found"

source_env="${primary_path}/.env"
[[ -f "$source_env" ]] || fail "Primary worktree .env was not found: ${source_env}"
[[ -r "$source_env" ]] || fail "Primary worktree .env is not readable: ${source_env}"

if git show-ref --verify --quiet "refs/heads/${branch_name}"; then
  git worktree add "$target_path" "$branch_name"
else
  if git show-ref --verify --quiet "refs/heads/${base_branch}"; then
    start_point="$base_branch"
  elif git show-ref --verify --quiet "refs/remotes/origin/${base_branch}"; then
    start_point="origin/${base_branch}"
  else
    fail "Base branch '${base_branch}' was not found locally or on origin"
  fi

  git worktree add -b "$branch_name" "$target_path" "$start_point"
fi

[[ ! -e "${target_path}/.env" ]] || fail "Target .env already exists: ${target_path}/.env"
cp -p "$source_env" "${target_path}/.env"

cat <<EOF
[create-worktree] Created worktree
- branch: ${branch_name}
- issue: #${issue_number}
- base: ${base_branch}
- path: ${target_path}
- env: copied from primary worktree

Next:
  cd ${target_path}
EOF
