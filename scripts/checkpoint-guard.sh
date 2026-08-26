#!/bin/bash
#
# 체크포인트 범위·방향 가드의 기계 검사 부분 (briefing-harness.md 3-1절 1~2단계).
#
# 수행 내용:
#   1. diff 스냅샷 고정: git add -N . 후 git diff HEAD를
#      {worktree}/.claude/issues/{이슈번호}/checkpoints/task-{N}.diff 로 저장
#   2. 경로 검사: 변경 파일을 root plan의 allowed_paths / blocked_paths와 대조
#
# 사용법: checkpoint-guard.sh <worktree-절대경로> [task번호] [plan파일-절대경로]
#   - task번호 생략 시 기존 최대 번호 + 1
#   - plan파일 생략 시 docs/plans에서 "worktree: <경로>" 로 탐색 (active > draft 우선)
#
# 종료 코드: 0=통과, 1=경로 위반(즉시 중단·태형님 보고 대상), 2=plan 미발견(스냅샷만 저장됨)
#
# 의미 검사(SCOPE_CREEP/DIRECTION_DRIFT 판정)는 이 스크립트가 하지 않는다.
# 스냅샷 저장 후 read-only 서브에이전트를 백그라운드로 스폰해 수행한다.

set -u

ROOT="/Users/thlee/Documents/personal/stock-market"
PLANS_DIR="$ROOT/docs/plans"

WT="${1:-}"
TASK_N="${2:-}"
PLAN_FILE="${3:-}"

if [ -z "$WT" ] || [ ! -d "$WT" ]; then
  echo "오류: worktree 절대경로가 필요합니다. 사용법: checkpoint-guard.sh <worktree> [task번호] [plan파일]" >&2
  exit 2
fi
WT="${WT%/}"

ISSUE=$(basename "$WT" | grep -oE 'issue-[0-9]+' | grep -oE '[0-9]+' | head -1)
if [ -z "$ISSUE" ]; then
  echo "오류: worktree 경로에서 이슈 번호(stock-market-issue-숫자)를 찾지 못했습니다: $WT" >&2
  exit 2
fi

if ! git -C "$WT" rev-parse --git-dir >/dev/null 2>&1; then
  echo "오류: git worktree가 아닙니다: $WT" >&2
  exit 2
fi

# plan 탐색: worktree 경로가 일치하는 plan 중 active > draft 우선
if [ -z "$PLAN_FILE" ]; then
  candidates=$(grep -l "^worktree:[[:space:]]*$WT[[:space:]]*$" "$PLANS_DIR"/*-plan.md 2>/dev/null || true)
  for want in active draft; do
    for p in $candidates; do
      s=$(sed -n 's/^status:[[:space:]]*//p' "$p" | head -1 | tr -d '[:space:]')
      if [ "$s" = "$want" ]; then PLAN_FILE="$p"; break 2; fi
    done
  done
fi

# 1. 스냅샷 저장 (신규 파일 포함 위해 add -N)
CKPT_DIR="$WT/.claude/issues/$ISSUE/checkpoints"
mkdir -p "$CKPT_DIR"

if [ -z "$TASK_N" ]; then
  last=$(ls "$CKPT_DIR" 2>/dev/null | grep -oE 'task-[0-9]+' | grep -oE '[0-9]+' | sort -n | tail -1)
  TASK_N=$(( ${last:-0} + 1 ))
fi
SNAPSHOT="$CKPT_DIR/task-$TASK_N.diff"

git -C "$WT" add -N . 2>/dev/null
git -C "$WT" diff HEAD -- . ":(exclude).claude" > "$SNAPSHOT"

CHANGED=$(git -C "$WT" diff HEAD --name-only -- . ":(exclude).claude")
COUNT=$(printf '%s' "$CHANGED" | grep -c . || true)

echo "스냅샷 저장: $SNAPSHOT (변경 파일 ${COUNT}개)"

if [ -z "$PLAN_FILE" ] || [ ! -f "$PLAN_FILE" ]; then
  echo "경고: worktree=$WT 에 해당하는 active/draft plan을 찾지 못해 경로 검사를 건너뜁니다." >&2
  exit 2
fi
echo "기준 plan: $PLAN_FILE"

# 2. 경로 검사 (allowed_paths / blocked_paths 대조)
# heredoc이 stdin을 python 프로그램으로 소비하므로 변경 목록은 임시 파일로 전달한다.
CHANGED_FILE=$(mktemp)
printf '%s\n' "$CHANGED" > "$CHANGED_FILE"
trap 'rm -f "$CHANGED_FILE"' EXIT

python3 - "$PLAN_FILE" "$CHANGED_FILE" <<'PY'
import sys, re

plan_file = sys.argv[1]
changed = [l.strip() for l in open(sys.argv[2], encoding="utf-8") if l.strip()]

def read_list(text, key):
    m = re.search(rf"^{key}:\s*$(.*?)(?=^\S|\Z)", text, re.M | re.S)
    if not m:
        return []
    return [x.strip().lstrip("- ").strip() for x in m.group(1).splitlines() if x.strip().startswith("-")]

text = open(plan_file, encoding="utf-8").read()
fm = text.split("---", 2)[1] if text.startswith("---") else text
allowed = read_list(fm, "allowed_paths")
blocked = read_list(fm, "blocked_paths")

def match(path, entry):
    if entry.endswith("/**"):
        return path.startswith(entry[:-3] + "/") or path == entry[:-3]
    return path == entry or path.startswith(entry.rstrip("/") + "/")

blocked_hits = [p for p in changed if any(match(p, e) for e in blocked)]
outside = [p for p in changed if allowed and not any(match(p, e) for e in allowed)]

if blocked_hits:
    print("경로 위반 (blocked_paths 침범):")
    for p in blocked_hits:
        print(f"  - {p}")
if outside:
    print("경로 위반 (allowed_paths 밖):")
    for p in outside:
        print(f"  - {p}")
if blocked_hits or outside:
    print("=> 즉시 중단하고 태형님에게 보고할 것 (briefing-harness.md 3-1)")
    sys.exit(1)

print(f"경로 검사 통과: 변경 {len(changed)}개 파일 모두 plan 범위 안")
PY
exit $?
