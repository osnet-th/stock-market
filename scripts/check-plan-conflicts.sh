#!/bin/sh
#
# 활성 plan 간 경로 충돌 검사 (단일 모듈 버전).
# 현재 이슈의 active plan allowed_paths를 다른 모든 active plan의 allowed_paths와
# 파일/디렉토리 단위로 대조한다.

set -eu

usage() {
  echo "Usage: $0 <issue-number|TBD> [plans_dir]" >&2
  echo "Example: $0 29" >&2
}

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  usage
  exit 2
fi

ISSUE="$1"

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
PLANS_DIR="${2:-$ROOT_DIR/docs/plans}"

if [ ! -d "$PLANS_DIR" ]; then
  echo "Plan directory not found: $PLANS_DIR" >&2
  exit 2
fi

TMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/plan-conflicts.XXXXXX")
trap 'rm -rf "$TMP_DIR"' EXIT INT TERM

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

frontmatter_list() {
  file="$1"
  key="$2"

  awk -v key="$key" '
    NR == 1 && $0 == "---" { in_fm = 1; next }
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
        sub("^[[:space:]]*-[[:space:]]+", "", $0)
        print $0
      }
    }
  ' "$file" | while IFS= read -r item; do
    printf '%s\n' "$item" | trim_scalar
  done
}

path_root() {
  path="$1"
  path=${path#./}
  path=${path%/}

  case "$path" in
    */**)
      path=${path%/**}
      ;;
    */\*)
      path=${path%/*}
      ;;
  esac

  case "$path" in
    *"*"*)
      path=${path%%\**}
      path=${path%/}
      ;;
  esac

  printf '%s\n' "$path"
}

paths_overlap() {
  left=$(path_root "$1")
  right=$(path_root "$2")

  [ -n "$left" ] || return 1
  [ -n "$right" ] || return 1

  if [ "$left" = "$right" ]; then
    return 0
  fi

  case "$left" in
    "$right"/*) return 0 ;;
  esac

  case "$right" in
    "$left"/*) return 0 ;;
  esac

  return 1
}

CURRENT_PLAN=""
CURRENT_COUNT=0
CURRENT_ALLOWED="$TMP_DIR/current.allowed"
: > "$CURRENT_ALLOWED"

for plan in "$PLANS_DIR"/*.md; do
  [ -e "$plan" ] || continue

  status=$(frontmatter_scalar "$plan" status || true)
  issue=$(frontmatter_scalar "$plan" issue || true)

  [ "$status" = "active" ] || continue
  [ "$issue" = "$ISSUE" ] || continue

  CURRENT_COUNT=$((CURRENT_COUNT + 1))
  CURRENT_PLAN="$plan"
  frontmatter_list "$plan" allowed_paths >> "$CURRENT_ALLOWED"
done

if [ "$CURRENT_COUNT" -eq 0 ]; then
  echo "No active plan found for issue=$ISSUE" >&2
  exit 1
fi

if [ "$CURRENT_COUNT" -gt 1 ]; then
  echo "Multiple active plans found for issue=$ISSUE" >&2
  exit 1
fi

if [ ! -s "$CURRENT_ALLOWED" ]; then
  echo "Current plan has no allowed_paths: $CURRENT_PLAN" >&2
  exit 1
fi

CONFLICTS="$TMP_DIR/conflicts"
: > "$CONFLICTS"

for plan in "$PLANS_DIR"/*.md; do
  [ -e "$plan" ] || continue
  [ "$plan" != "$CURRENT_PLAN" ] || continue

  status=$(frontmatter_scalar "$plan" status || true)
  issue=$(frontmatter_scalar "$plan" issue || true)

  [ "$status" = "active" ] || continue
  [ "$issue" != "$ISSUE" ] || continue

  OTHER_ALLOWED="$TMP_DIR/other.allowed"
  : > "$OTHER_ALLOWED"
  frontmatter_list "$plan" allowed_paths > "$OTHER_ALLOWED"
  [ -s "$OTHER_ALLOWED" ] || continue

  while IFS= read -r current_path; do
    [ -n "$current_path" ] || continue

    while IFS= read -r other_path; do
      [ -n "$other_path" ] || continue

      if paths_overlap "$current_path" "$other_path"; then
        {
          echo "issue=$issue"
          echo "plan=$plan"
          echo "current_path=$current_path"
          echo "other_path=$other_path"
          echo
        } >> "$CONFLICTS"
      fi
    done < "$OTHER_ALLOWED"
  done < "$CURRENT_ALLOWED"
done

if [ -s "$CONFLICTS" ]; then
  echo "Active plan path conflict detected." >&2
  echo "Current issue: $ISSUE" >&2
  echo "Current plan: $CURRENT_PLAN" >&2
  echo >&2
  cat "$CONFLICTS" >&2
  echo "Resolve by sequencing the issues or narrowing allowed_paths." >&2
  exit 1
fi

echo "No active plan path conflicts found for issue=$ISSUE."
