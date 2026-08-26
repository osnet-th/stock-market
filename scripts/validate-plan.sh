#!/bin/sh

set -eu

usage() {
  echo "Usage: $0 <plan-file>" >&2
  echo "Example: $0 /Users/thlee/Documents/personal/stock-market/docs/plans/example-plan.md" >&2
}

if [ "$#" -ne 1 ]; then
  usage
  exit 2
fi

PLAN_FILE="$1"

if [ ! -f "$PLAN_FILE" ]; then
  echo "Plan file not found: $PLAN_FILE" >&2
  exit 2
fi

trim_scalar() {
  sed -e 's/^[[:space:]]*//' \
      -e 's/[[:space:]]*$//' \
      -e 's/^["'\'']//; s/["'\'']$//'
}

frontmatter_scalar() {
  file="$1"
  key="$2"

  awk -v key="$key" '
    NR == 1 && $0 == "---" { in_fm = 1; next }
    NR == 1 && $0 != "---" { exit 2 }
    in_fm && $0 == "---" { exit }
    in_fm {
      pattern = "^[[:space:]]*" key ":[[:space:]]*"
      if ($0 ~ pattern) {
        sub(pattern, "", $0)
        print $0
        exit
      }
    }
  ' "$file" | trim_scalar
}

plan_body_triggers_test_plan_status() {
  file="$1"

  awk '
    NR == 1 && $0 == "---" { in_fm = 1; next }
    NR > 1 && in_fm && $0 == "---" { in_fm = 0; next }
    in_fm { next }

    /^```java[[:space:]]*$/ { in_java = 1; next }
    /^```[[:space:]]*$/     { in_java = 0; next }
    in_java && (/@Test/ || /org\.junit/) { found = 1 }

    /\]\([^)]*examples\/[^)]*\.md\)/ { found = 1 }

    END { exit !found }
  ' "$file"
}

plan_body_triggers_schema_plan_status() {
  file="$1"

  awk '
    NR == 1 && $0 == "---" { in_fm = 1; next }
    NR > 1 && in_fm && $0 == "---" { in_fm = 0; next }
    in_fm { next }

    {
      lower = tolower($0)
    }

    /없음|없다|없는|제외|하지 않|금지|수정 금지/ { next }
    lower ~ /without|not[[:space:]]+|no[[:space:]]+/ { next }

    /@Entity/ || /@Table/ || /@Column/ || /@JoinColumn/ { found = 1 }
    /Entity 신규|Entity 추가|Entity 변경|Entity 관계/ { found = 1 }
    /엔티티 신규|엔티티 추가|엔티티 변경|엔티티 관계/ { found = 1 }
    /테이블 추가|테이블 변경|테이블 삭제/ { found = 1 }
    /컬럼 추가|컬럼 변경|컬럼 삭제/ { found = 1 }
    /인덱스 추가|인덱스 변경|인덱스 삭제/ { found = 1 }
    /제약조건|마이그레이션|백필|DB 스키마/ { found = 1 }

    lower ~ /create[[:space:]]+table/ { found = 1 }
    lower ~ /alter[[:space:]]+table/ { found = 1 }
    lower ~ /drop[[:space:]]+table/ { found = 1 }
    lower ~ /add[[:space:]]+column/ { found = 1 }
    lower ~ /db[[:space:]]+schema/ { found = 1 }
    lower ~ /schema[[:space:]]+migration/ { found = 1 }
    lower ~ /migration|backfill|ddl/ { found = 1 }
    lower ~ /primary[[:space:]]+key|foreign[[:space:]]+key|unique[[:space:]]+key/ { found = 1 }
    lower ~ /create[[:space:]]+index|drop[[:space:]]+index|db[[:space:]]+index|database[[:space:]]+index|table[[:space:]]+index/ { found = 1 }
    lower ~ /db[[:space:]]+constraint|database[[:space:]]+constraint|table[[:space:]]+constraint/ { found = 1 }

    END { exit !found }
  ' "$file"
}

frontmatter_list_count() {
  file="$1"
  key="$2"

  awk -v key="$key" '
    NR == 1 && $0 == "---" { in_fm = 1; next }
    NR == 1 && $0 != "---" { exit 2 }
    in_fm && $0 == "---" { exit }
    in_fm {
      key_pattern = "^[[:space:]]*" key ":[[:space:]]*$"
      scalar_pattern = "^[[:space:]]*[A-Za-z_][A-Za-z0-9_]*:"

      if ($0 ~ key_pattern) {
        in_list = 1
        next
      }

      if (in_list && $0 ~ scalar_pattern) {
        exit
      }

      if (in_list && $0 ~ "^[[:space:]]*-[[:space:]]+") {
        count++
      }
    }
    END { print count + 0 }
  ' "$file"
}

error_count=0

fail() {
  echo "ERROR: $1" >&2
  error_count=$((error_count + 1))
}

warn() {
  echo "WARN: $1" >&2
}

plan_body_has_requirement_ledger() {
  file="$1"

  awk '
    NR == 1 && $0 == "---" { in_fm = 1; next }
    NR > 1 && in_fm && $0 == "---" { in_fm = 0; next }
    in_fm { next }

    /^```/ { in_code = !in_code; next }
    in_code { next }

    /^##[[:space:]]+요구사항 원장[[:space:]]*$/ { found = 1 }

    END { exit !found }
  ' "$file"
}

first_line=$(sed -n '1p' "$PLAN_FILE")
if [ "$first_line" != "---" ]; then
  fail "frontmatter must start at line 1 with ---"
fi

issue=$(frontmatter_scalar "$PLAN_FILE" issue || true)
status=$(frontmatter_scalar "$PLAN_FILE" status || true)
branch=$(frontmatter_scalar "$PLAN_FILE" branch || true)
worktree=$(frontmatter_scalar "$PLAN_FILE" worktree || true)
test_plan_status=$(frontmatter_scalar "$PLAN_FILE" test_plan_status || true)
schema_plan_status=$(frontmatter_scalar "$PLAN_FILE" schema_plan_status || true)
docs_only=$(frontmatter_scalar "$PLAN_FILE" docs_only || true)
allowed_count=$(frontmatter_list_count "$PLAN_FILE" allowed_paths || echo 0)
blocked_count=$(frontmatter_list_count "$PLAN_FILE" blocked_paths || echo 0)

[ -n "$issue" ] || fail "issue is required (GitHub issue number or TBD)"
[ -n "$status" ] || fail "status is required"
[ -n "$branch" ] || fail "branch is required"
[ -n "$worktree" ] || fail "worktree is required"
[ -n "$test_plan_status" ] || fail "test_plan_status is required"
[ -n "$schema_plan_status" ] || fail "schema_plan_status is required"
[ "$allowed_count" -gt 0 ] || fail "allowed_paths must contain at least one item"
[ "$blocked_count" -gt 0 ] || fail "blocked_paths must contain at least one item"

case "$issue" in
  TBD)
    ;;
  ''|*[!0-9]*)
    fail "issue must be TBD or a GitHub issue number like 29"
    ;;
  *)
    ;;
esac

case "$status" in
  draft|active|blocked|done)
    ;;
  *)
    fail "status must be one of: draft, active, blocked, done"
    ;;
esac

if [ -n "$test_plan_status" ]; then
  case "$test_plan_status" in
    pending|approved|none)
      ;;
    *)
      fail "test_plan_status must be one of: pending, approved, none"
      ;;
  esac
fi

if [ -n "$schema_plan_status" ]; then
  case "$schema_plan_status" in
    pending|approved|none)
      ;;
    *)
      fail "schema_plan_status must be one of: pending, approved, none"
      ;;
  esac
fi

if plan_body_triggers_test_plan_status "$PLAN_FILE"; then
  if [ "$test_plan_status" != "approved" ]; then
    fail "plan references examples link or contains java test code; test_plan_status must be 'approved'"
  fi
fi

if [ "$schema_plan_status" = "pending" ]; then
  case "$status" in
    active|done)
      fail "schema_plan_status is pending; status must not be active or done until DB schema design is approved"
      ;;
  esac
fi

if [ "$test_plan_status" = "pending" ]; then
  case "$status" in
    active|done)
      fail "test_plan_status is pending; status must not be active or done until unit test decision is confirmed (write tests -> approved, or skip -> none)"
      ;;
  esac
fi

if [ "$docs_only" != "true" ] && plan_body_triggers_schema_plan_status "$PLAN_FILE"; then
  case "$schema_plan_status" in
    pending|approved)
      ;;
    *)
      fail "plan references Entity or DB schema changes; schema_plan_status must be 'pending' or 'approved' (docs/harness-only plan은 docs_only: true 로 예외 처리)"
      ;;
  esac
fi

if [ "$issue" != "TBD" ]; then
  case "$branch" in
    issue/"$issue"|issue/"$issue"-*)
      ;;
    *)
      fail "branch must be issue/$issue-{slug} when issue is $issue"
      ;;
  esac
else
  case "$branch" in
    features/*|fix/*|refactor/*|chore/*|issue/*)
      ;;
    *)
      fail "branch must start with features/, fix/, refactor/, chore/, or issue/"
      ;;
  esac
fi

case "$worktree" in
  /*)
    ;;
  *)
    fail "worktree must be an absolute path"
    ;;
esac

if [ "$issue" != "" ] && [ "$issue" != "TBD" ]; then
  if ! plan_body_has_requirement_ledger "$PLAN_FILE"; then
    warn "issue plan has no '## 요구사항 원장' section; 구현 이해 확인 Gate의 요구사항 커버리지 대조 기준이 없습니다 (docs/ai/gates/planning-gate.md)"
  fi
fi

if [ "$error_count" -gt 0 ]; then
  echo "Plan validation failed: $PLAN_FILE" >&2
  exit 1
fi

echo "Plan validation passed: $PLAN_FILE"
