# 납입 리마인더 당일 표시 + 이력 0건 판정 누락 Review 기록

**Date:** 2026-08-10
**Issue:** #111
gate: docs/gates/2026-08-10-deposit-reminder-due-today-gates.md

## Findings (심각도 순)

명시적 findings 없음.

셀프 리뷰(diff 라인 단위) 확인 사항:

- 판정 기준 불변 검증: `isDepositOverdue`의 guard 순서·말일 보정·당월 기록 검사 로직은 helper 추출 전후 동일. 기존 호출부(공개 시그니처) 변경 없음.
- `depositOverdue`/`depositDueToday` 상호 배타: 당일이면 overdue false(다음날부터), 다음날부터는 dueToday false(일자 불일치) — 배지 ternary 안전.
- `from` 4-인자 오버로드 확장: 호출부는 `getItems` 1곳뿐 — 컴파일로 확인.
- `@JsonInclude(NON_NULL)` 유지 — 신규 필드는 CASH/FUND에만 세팅, 기존 클라이언트 영향 없음(additive).
- code-convention: helper 추출로 메서드 5~10줄 유지, guard clause 우선, 중첩 1단계.

## Open Questions / Assumptions

- 이력 0건 CASH 항목의 만기 예상 금액이 함께 표시되기 시작함 — 같은 null 가드에 묶여 있던 부수 버그로 판단, 의도 수정에 포함 (plan 명시).
- `depositDay`는 entity 검증상 1 이상 가정 (기존 로직과 동일 가정).

## Change Summary

리마인더 팝업 대상을 #99 스펙("당일 + 미납")으로 확장 — 백엔드 `depositDueToday` 플래그 신설(+null 가드 버그 수정), 프런트 필터·배지 반영. 미납 배지 의미 불변. 4파일, +62/-26.
