#!/bin/sh
#
# SessionStart hook: docs/plans에 test_plan_status가 pending인 draft plan이 있으면
# "단위테스트 작성 여부 결정 게이트"를 상기시키는 additionalContext를 주입한다.
# pending draft가 없으면 아무것도 출력하지 않아 무관한 세션에서 노이즈가 없다.
#
# 관련 정책: docs/ai/test-planning-harness.md
# 기계적 차단: scripts/validate-plan.sh (test_plan_status: pending + status: active|done → fail)

PLANS_DIR="/Users/thlee/Documents/personal/stock-market/docs/plans"

[ -d "$PLANS_DIR" ] || exit 0

hits=""
for p in "$PLANS_DIR"/*-plan.md; do
  [ -f "$p" ] || continue
  status=$(sed -n 's/^status:[[:space:]]*//p' "$p" | head -1 | tr -d '[:space:]')
  tps=$(sed -n 's/^test_plan_status:[[:space:]]*//p' "$p" | head -1 | tr -d '[:space:]')
  if [ "$status" = "draft" ] && [ "$tps" = "pending" ]; then
    hits="$hits $(basename "$p")"
  fi
done

[ -n "$hits" ] || exit 0

MSG="[하네스 게이트 리마인더] test_plan_status=pending 인 draft plan 있음:$hits — root plan을 active로 전환하거나 /ce:work를 시작하기 전에 docs/ai/test-planning-harness.md에 따라 '단위테스트 작성 여부'를 태형님과 먼저 확정하라. 확정 전에는 상세 테스트 시나리오(Given/When/Then)를 plan에 선기입하지 말 것. 작성=시나리오 태형님 승인 후 test_plan_status: approved, 미작성=none. pending 상태로는 validate-plan.sh가 active/done 전환을 차단한다."

python3 - "$MSG" <<'PY'
import json, sys
msg = sys.argv[1]
print(json.dumps({
    "hookSpecificOutput": {
        "hookEventName": "SessionStart",
        "additionalContext": msg,
    }
}, ensure_ascii=False))
PY
