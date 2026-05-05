#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/check-worktree.sh [--mode auto|documented|lightweight] [--print-kind] [--quiet]
EOF
}

fail() {
  echo "[check-worktree] $1" >&2
  exit 1
}

mode="auto"
print_kind="false"
quiet="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      [[ $# -ge 2 ]] || fail "--mode requires a value"
      mode="$2"
      shift 2
      ;;
    --print-kind)
      print_kind="true"
      shift
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

case "$mode" in
  auto|documented|lightweight) ;;
  *) fail "Unsupported mode: $mode" ;;
esac

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || fail "Git repository not found"
current_branch=$(git branch --show-current)
[[ -n "$current_branch" ]] || fail "Detached HEAD is not allowed"

primary_worktree=$(git worktree list --porcelain | awk '
  /^worktree / { print substr($0, 10); exit }
')
current_worktree="$repo_root"

if [[ "$current_worktree" == "$primary_worktree" ]]; then
  kind="primary"
else
  kind="linked"
fi

branch_occurrences=$(git worktree list --porcelain | awk -v branch="refs/heads/${current_branch}" '
  /^branch / && $2 == branch { count++ }
  END { print count + 0 }
')

[[ "$branch_occurrences" -le 1 ]] || fail "Branch '${current_branch}' is attached to multiple worktrees"

if [[ "$mode" == "documented" && "$kind" == "primary" ]]; then
  fail "Documented workflow must run from a linked worktree. Use scripts/create-worktree.sh first."
fi

if [[ "$print_kind" == "true" ]]; then
  printf '%s\n' "$kind"
  exit 0
fi

if [[ "$quiet" != "true" ]]; then
  cat <<EOF
[check-worktree] OK
- branch: ${current_branch}
- kind: ${kind}
- path: ${current_worktree}
EOF
fi
