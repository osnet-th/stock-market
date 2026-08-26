# 납입 리마인더 당일 표시 + 이력 0건 판정 누락 Work 기록

**Date:** 2026-08-10
**Issue:** #111
gate: docs/gates/2026-08-10-deposit-reminder-due-today-gates.md
plan: docs/plans/2026-08-10-001-fix-deposit-reminder-due-today-plan.md

## 변경 내용

### 백엔드

- `PortfolioService.getItems`
  - CASH/FUND 항목은 `finalDepositMap.getOrDefault(id, List.of())`로 **이력 0건이어도 항상 판정** (기존: 이력 ≥1건일 때만 판정 → null 가드 버그)
  - `depositDueToday` 계산 추가, 만기 예상 금액도 동일 가드 해제
- `PortfolioService.isDepositDueToday` 신설 — 기준일 == 당월 유효 납입일(말일 보정 동일) && 당월 납입 기록 없음
- 공통 helper 추출: `resolveDepositDay` / `effectiveDepositDay` / `hasNoDepositThisMonth` (`isDepositOverdue`와 공유, 판정 기준 불변)
- `isDepositOverdue`의 중복 javadoc 블록 1개 제거 (기존 파일 중복)
- `PortfolioItemResponse`: `depositDueToday` 필드·생성자 파라미터 추가, 전체 `from` 오버로드 시그니처 확장(축약 오버로드는 null 위임) — additive, `@JsonInclude(NON_NULL)` 유지

### 프런트

- `portfolio.js` `checkDepositReminder`: 필터 `depositOverdue === true || depositDueToday === true` (#99 스펙 "당일 + 미납")
- `portfolio-deposit-financial.html`: 항목별 상태 배지("미납" red / "오늘 납입일" emerald), 헤더 "납입 확인 안내"·안내 문구 당일 포함으로 조정

## 변경 파일

- src/main/java/.../portfolio/application/PortfolioService.java
- src/main/java/.../portfolio/application/dto/PortfolioItemResponse.java
- src/main/resources/static/js/components/portfolio.js
- src/main/resources/static/partials/portfolio-deposit-financial.html

## 동작 변경 (의도된 것)

1. 납입일 당일 + 당월 기록 없음 → 리마인더 팝업에 "오늘 납입일"로 노출 (기존: 미노출)
2. 이력 0건 + 납입일 경과 → 미납 배지·리마인더 정상 노출 (기존: 절대 미노출)
3. 이력 0건 CASH 항목의 만기 예상 금액 표시 (기존: 미계산 — 같은 null 가드에 묶여 있던 부수 버그)
4. 포트폴리오 목록 미납 배지는 기존대로 납입일 다음날부터 (불변)
