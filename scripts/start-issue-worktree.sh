#!/bin/sh
#
# GitHub 이슈 착수 스크립트 (gates/github-issue-gate.md 착수 Gate).
#
# 수행 내용:
#   1. gh issue view로 이슈 존재·OPEN 상태 확인
#   2. 메인 저장소 main 전환 + git pull 최신화
#   3. ../stock-market-issue-{N} worktree를 issue/{N}-{slug} 브랜치로 생성
#   4. 이슈 제목/본문을 {worktree}/.claude/issues/{N}/{N}.md 로 저장
#   5. 이슈 본인 assign (실패해도 착수는 계속, 경고만)
#   6. stage 마커를 brainstorm으로 생성
#
# 사용법: start-issue-worktree.sh <issue-number> [slug]
#   - slug 생략 시 이슈 제목에서 파생(영문 소문자·숫자·하이픈). 파생 불가 시 "task".

set -eu

usage() {
  echo "Usage: $0 <issue-number> [slug]" >&2
  echo "Example: $0 29 portfolio-expansion" >&2
}

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  usage
  exit 2
fi

ISSUE="$1"
SLUG="${2:-}"

ROOT="/Users/thlee/Documents/personal/stock-market"
PARENT="/Users/thlee/Documents/personal"
WORKTREE_DIR="$PARENT/stock-market-issue-$ISSUE"

if ! printf '%s\n' "$ISSUE" | grep -Eq '^[0-9]+$'; then
  echo "Issue number must be numeric. Current: $ISSUE" >&2
  exit 1
fi

if [ ! -d "$ROOT/.git" ]; then
  echo "Git repository not found: $ROOT" >&2
  exit 1
fi

cd "$ROOT"

STATE=$(gh issue view "$ISSUE" --json state -q .state 2>/dev/null || true)
if [ -z "$STATE" ]; then
  echo "GitHub issue not found: #$ISSUE" >&2
  exit 1
fi
if [ "$STATE" != "OPEN" ]; then
  echo "GitHub issue is not open (state=$STATE): #$ISSUE" >&2
  exit 1
fi

TITLE=$(gh issue view "$ISSUE" --json title -q .title)
URL=$(gh issue view "$ISSUE" --json url -q .url)

if [ -z "$SLUG" ]; then
  SLUG=$(printf '%s' "$TITLE" \
    | tr '[:upper:]' '[:lower:]' \
    | sed -e 's/[^a-z0-9]/-/g' -e 's/--*/-/g' -e 's/^-//' -e 's/-$//' \
    | cut -c1-30 \
    | sed -e 's/-$//')
fi
[ -n "$SLUG" ] || SLUG="task"

BRANCH="issue/$ISSUE-$SLUG"

if [ -e "$WORKTREE_DIR" ]; then
  echo "Worktree path already exists: $WORKTREE_DIR" >&2
  exit 1
fi

if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
  echo "Repository has local changes. Commit, stash, or clean before creating a worktree: $ROOT" >&2
  exit 1
fi

if git show-ref --verify --quiet "refs/heads/$BRANCH"; then
  echo "Branch already exists: $BRANCH" >&2
  exit 1
fi

if git worktree list --porcelain | grep -Fqx "branch refs/heads/$BRANCH"; then
  echo "Branch is already checked out in another worktree: $BRANCH" >&2
  exit 1
fi

git checkout main
git pull
git worktree add "$WORKTREE_DIR" -b "$BRANCH" main

ISSUE_DIR="$WORKTREE_DIR/.claude/issues/$ISSUE"
mkdir -p "$ISSUE_DIR"

{
  echo "# #$ISSUE $TITLE"
  echo
  echo "- url: $URL"
  echo "- state: $STATE"
  echo "- saved: $(date '+%Y-%m-%d %H:%M')"
  echo
  echo "## 본문"
  echo
  gh issue view "$ISSUE" --json body -q .body
} > "$ISSUE_DIR/$ISSUE.md"

if ! gh issue edit "$ISSUE" --add-assignee "@me" >/dev/null 2>&1; then
  echo "경고: 이슈 assign에 실패했습니다. gh issue edit $ISSUE --add-assignee @me 를 수동 실행하세요." >&2
fi

printf 'brainstorm' > "$ISSUE_DIR/stage"

cat <<EOF
Created issue worktree.
issue: #$ISSUE $TITLE
branch: $BRANCH
worktree: $WORKTREE_DIR
issue detail: $ISSUE_DIR/$ISSUE.md
stage: brainstorm
EOF
