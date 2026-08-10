---
title: 납입 리마인더 당일 표시 + 이력 0건 판정 누락 수정
type: fix
status: active
date: 2026-08-10
issue: https://github.com/osnet-th/stock-market/issues/111
origin: docs/brainstorms/2026-08-10-deposit-reminder-due-today-brainstorm.md
gate: docs/gates/2026-08-10-deposit-reminder-due-today-gates.md
---

# 납입 리마인더 당일 표시 + 이력 0건 판정 누락 수정 (#111)

## Overview

리마인더 팝업 대상을 #99 스펙대로 "당일 + 미납"으로 확장하고, 납입 이력 0건 항목이 판정에서 빠지는 null 가드 버그를 수정한다. 포트폴리오 목록의 미납 배지는 기존 의미(납입일 다음날부터) 유지.

## 변경 파일

| 파일 | 변경 |
|---|---|
| `portfolio/application/PortfolioService.java` | `getItems` 판정 가드 수정(getOrDefault), `isDepositDueToday` 신설, 공통 helper(`resolveDepositDay`/`hasNoDepositThisMonth`) 추출 |
| `portfolio/application/dto/PortfolioItemResponse.java` | `depositDueToday` 필드·생성자 파라미터·`from` 확장 (기존 호출부는 null 유지) |
| `static/js/components/portfolio.js` | `checkDepositReminder` 필터를 `depositOverdue \|\| depositDueToday`로 확장 |
| `static/partials/portfolio-deposit-financial.html` | 항목별 미납/오늘 납입일 구분 배지, 헤더·안내 문구 조정 |

Entity·DB 변경 없음. 미납 배지·납입 모달·스누즈 로직 불변.

## Implementation Steps

### 1. 백엔드 — 판정 로직

- [x] `isDepositOverdue`에서 depositDay 해석을 `resolveDepositDay(item)` helper로 추출
- [x] 당월 납입 기록 부재 검사를 `hasNoDepositThisMonth(histories, referenceDate)` helper로 추출
- [x] `isDepositDueToday(item, histories, referenceDate)` 신설 — 오늘 == effectiveDay(말일 보정 동일) && 당월 기록 없음
- [x] `getItems`: CASH/FUND 항목은 `finalDepositMap.getOrDefault(id, List.of())`로 항상 판정 수행 (이력 0건 포함), `depositDueToday` 계산 추가, 만기 예상 금액도 동일 가드 해제

### 2. 백엔드 — 응답 DTO

- [x] `PortfolioItemResponse`에 `depositDueToday` 추가 (depositOverdue 옆), 전체 `from` 오버로드 시그니처 확장, 축약 오버로드는 null 위임

### 3. 프런트

- [x] `checkDepositReminder`: 필터 `i.depositOverdue === true || i.depositDueToday === true`
- [x] 팝업 항목에 상태 배지 — `depositOverdue`면 "미납", 아니면 "오늘 납입일"
- [x] 헤더 "납입 확인 안내" / 안내 문구를 당일 포함으로 조정

## Technical Considerations

- **미납 배지 불변**: 목록 배지는 `depositOverdue`만 사용하므로 당일 항목에 배지가 붙지 않음 — 확정안 (A) 유지.
- **null 가드 해제의 부수 효과**: 이력 0건 + 납입일 경과 항목이 이제 미납 배지에 정상 노출 — 버그 수정 의도 그 자체.
- **만기 예상 금액**: 기존에도 이력 0건이면 미계산이던 것을 동일 수정으로 해제 — `calculateMaturityAmount`는 필수값 없으면 null 반환하므로 안전.
- **API**: 응답 필드 additive 추가만 — 기존 클라이언트 영향 없음.

## Validation

- [x] `./gradlew compileJava` / `./gradlew test` — 117/117 PASS (로컬 PostgreSQL 가동으로 contextLoads 포함)
- [x] JS 문법 — 브라우저 하네스 전체 로드로 확인 (node 미설치로 `node --check` 대체)
- [x] 판정 로직 시나리오 검증 — 브라우저 목 하네스 (당일 배지/미납 배지/제외/납입 클릭/스누즈 재노출)
